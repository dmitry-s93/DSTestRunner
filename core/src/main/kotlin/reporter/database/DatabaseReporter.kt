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
     * Writes all direct children of the step identified by [id].
     *
     * Invariants the written rows must satisfy, because the visual-diff status is derived from the
     * (status, comparison_mismatch) pair when the data is read:
     * 1. comparison_mismatch = true is possible only together with is_comparison_step = true;
     * 2. comparison_mismatch = true means the comparison was performed and found a difference;
     *    a missing baseline counts as a difference;
     * 3. a technical failure of the comparison is written as status = BROKEN with
     *    comparison_mismatch = false;
     * 4. status is NULL only for container steps, leaf steps always carry a status;
     * 5. is_comparison_step = false for container steps.
     * Invariants 1 and 3 are additionally backed by database constraints on comparison_mismatch.
     */
    private fun saveSteps(id: String, parentStepId: UUID? = null) {
        steps[id]?.forEach { stepItem ->
            val step = stepItem.value
            val stepId = UUID.randomUUID()
            val isContainer = steps.containsKey(stepItem.key)
            val comparisonMismatch = !isContainer && step.imageComparisonData?.mismatch == true
            val isComparisonStep = !isContainer && step.imageComparisonData?.isComparisonStep == true

            StepsTable.insert { table ->
                table[StepsTable.idRow] = stepId
                table[testIdRow] = dbTestId!!
                table[parentStepIdRow] = parentStepId
                table[identifierRow] = truncateString(stepItem.key, identifierRow)
                table[nameRow] = truncateString(step.name, nameRow)
                // A container has no stored status: it is computed from its leaves when read.
                table[statusRow] = if (isContainer) null else step.result.getStatus().name
                table[comparisonMismatchRow] = comparisonMismatch
                table[isComparisonStepRow] = isComparisonStep
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
                val actualImagePath = imagesData.actualImagePath
                if (!actualImagePath.isNullOrEmpty()) {
                    ImageComparisonInfoTable.insert { table ->
                        table[stepIdRow] = stepId
                        table[actualImagePathRow] = actualImagePath
                        imagesData.baselineImagePath?.let { table[baselineImagePathRow] = it }
                        imagesData.diffImagePath?.let { table[diffImagePathRow] = it }
                    }
                }
            }

            if (isContainer)
                saveSteps(stepItem.key, stepId)
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
}
