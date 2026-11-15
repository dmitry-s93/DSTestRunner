/* Copyright 2024 DSTestRunner Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reporter.database

import action.ActionResult
import action.ImageComparisonData
import config.reporter.DatabaseReporterConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import logger.Logger
import org.apache.commons.lang3.StringUtils
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import reporter.Reporter
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO


@Suppress("unused")
class DatabaseReporter : Reporter {
    private lateinit var testId: String
    private lateinit var testName: String
    private lateinit var testStartTime: Instant

    private var dbProjectId: UUID? = DatabaseReporterConfig.projectId
    private var dbTestId: UUID? = null

    private val steps: LinkedHashMap<String, LinkedHashMap<String, Step>> = LinkedHashMap()

    class Step(
        val name: String,
        val result: ActionResult,
        val parameters: HashMap<String, String>,
        val startTime: Instant,
        val stopTime: Instant,
        val imageComparisonData: ImageComparisonData?
    )

    override fun setTestInfo(testId: String, testName: String) {
        this.testId = testId
        this.testName = testName
        this.testStartTime = Clock.System.now()

        transaction {
            val generatedTestId = UUID.randomUUID()
            // The test row is created before the steps are known, so its end time is a placeholder
            // until the session is closed.
            TestsTable.insert { table ->
                table[idRow] = generatedTestId
                table[projectIdRow] = dbProjectId!!
                table[runIdRow] = DatabaseReporterConfig.runId!!
                table[identifierRow] = truncateString(testId, identifierRow)
                table[nameRow] = truncateString(testName, nameRow)
                table[startTimeRow] = testStartTime
                table[endTimeRow] = testStartTime
            }
            dbTestId = generatedTestId
        }
    }

    override fun addStep(
        id: String,
        parentId: String,
        name: String,
        parameters: HashMap<String, String>,
        actionResult: ActionResult,
        imageComparisonData: ImageComparisonData?,
        startTime: Long,
        stopTime: Long
    ) {
        if (!steps.containsKey(parentId))
            steps[parentId] = LinkedHashMap()
        if (steps[parentId]?.containsKey("$parentId.$id") == true)
            Logger.warning("Duplicate step ID: $parentId.$id", "DatabaseReporter")

        val step = Step(
            name = name,
            result = actionResult,
            parameters = parameters,
            imageComparisonData = imageComparisonData,
            startTime = fromEpochMilliseconds(startTime),
            stopTime = fromEpochMilliseconds(stopTime)
        )

        steps[parentId]?.put("$parentId.$id", step)

        if (parentId == testId) {
            transaction {
                saveSteps(testId)
            }
            steps.clear()
        }
    }

    override fun quit() {
        transaction {
            TestsTable.update({ TestsTable.idRow eq dbTestId!! }) {
                it[endTimeRow] = Clock.System.now()
            }
        }
    }

    /**
     * Writes all direct children of the step identified by [id] and returns the worst status among
     * the written steps, or null when the step has no children.
     */
    private fun saveSteps(id: String, parentStepId: UUID? = null): String? {
        var worstStatus: String? = null

        steps[id]?.forEach { stepItem ->
            val step = stepItem.value
            val stepId = UUID.randomUUID()
            val isContainer = steps.containsKey(stepItem.key)
            val screenshotMismatch = step.imageComparisonData?.markedImagePath != null

            // TODO(temporary): screenshot mismatch is reported as BROKEN by ImageComparison;
            // remap to FAILED until the runner reports FAILED directly.
            val status = if (screenshotMismatch) DB_STATUS_FAILED else step.result.getStatus().name

            StepsTable.insert { table ->
                table[StepsTable.idRow] = stepId
                table[testIdRow] = dbTestId!!
                table[parentStepIdRow] = parentStepId
                table[identifierRow] = truncateString(stepItem.key, identifierRow)
                table[nameRow] = truncateString(step.name, nameRow)
                table[statusRow] = status
                table[screenshotMismatchRow] = screenshotMismatch
                table[messageRow] = step.result.getMessage()
                table[traceRow] = step.result.getTrace()
                table[startTimeRow] = step.startTime
                table[endTimeRow] = step.stopTime
            }

            step.parameters.forEach { parameter ->
                StepParametersTable.insert { table ->
                    table[StepParametersTable.stepIdRow] = stepId
                    table[nameRow] = truncateString(parameter.key, nameRow)
                    table[valueRow] = parameter.value
                }
            }

            step.result.getErrorImage()?.let { image ->
                val byteArrayOutputStream = ByteArrayOutputStream()
                ImageIO.write(image, "png", byteArrayOutputStream)
                ErrorImagesTable.insert { table ->
                    table[stepIdRow] = stepId
                    table[imageRow] = byteArrayOutputStream.toByteArray()
                }
            }

            step.imageComparisonData?.let { imagesData ->
                val currentImagePath = imagesData.currentImagePath
                if (!currentImagePath.isNullOrEmpty()) {
                    ImageComparisonInfoTable.insert { table ->
                        table[stepIdRow] = stepId
                        table[currentImagePathRow] = currentImagePath
                        imagesData.templateImagePath?.let { table[templateImagePathRow] = it }
                        imagesData.markedImagePath?.let { table[markedImagePathRow] = it }
                    }
                }
            }

            // A container step takes the worst status of its children; a leaf keeps its own status.
            val finalStatus = if (isContainer) {
                val childrenStatus = saveSteps(stepItem.key, stepId)
                if (childrenStatus != null) {
                    StepsTable.update({ StepsTable.idRow eq stepId }) {
                        it[statusRow] = childrenStatus
                    }
                    childrenStatus
                } else {
                    status
                }
            } else {
                status
            }

            worstStatus = worseStatus(worstStatus, finalStatus)
        }

        return worstStatus
    }

    private fun worseStatus(current: String?, candidate: String): String {
        if (current == null)
            return candidate
        return if (statusSeverity(candidate) > statusSeverity(current)) candidate else current
    }

    private fun statusSeverity(status: String): Int {
        return when (status) {
            DB_STATUS_BROKEN -> 3
            DB_STATUS_FAILED -> 2
            DB_STATUS_PASSED -> 1
            else -> 0
        }
    }

    private fun truncateString(text: String, column: Column<String>): String {
        val colLength = (column.columnType as VarCharColumnType).colLength
        val textLength = text.length
        if (textLength > colLength) {
            Logger.warning(
                "Table: ${column.table.tableName}, column: ${column.name}. The value length ($textLength) exceeds the column size ($colLength) in the database and will be truncated.",
                "DatabaseReporter"
            )
            return StringUtils.abbreviate(text, colLength)
        }
        return text
    }

    companion object {
        private const val DB_STATUS_PASSED = "PASSED"
        private const val DB_STATUS_FAILED = "FAILED"
        private const val DB_STATUS_BROKEN = "BROKEN"
    }
}
