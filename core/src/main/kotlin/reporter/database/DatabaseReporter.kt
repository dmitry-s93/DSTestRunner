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
import action.ScreenData
import config.MainConfig
import config.reporter.DatabaseReporterConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import logger.Logger
import org.apache.commons.lang3.StringUtils
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import reporter.Reporter


@Suppress("unused")
class DatabaseReporter : Reporter {
    private lateinit var testId: String
    private lateinit var testName: String
    private lateinit var testStartTime: Instant

    private var dbProjectId: Long? = DatabaseReporterConfig.projectId
    private var dbTestId: Long? = null

    private val steps: LinkedHashMap<String, LinkedHashMap<String, Step>> = LinkedHashMap()

    class Step(
        val name: String,
        val result: ActionResult,
        val parameters: HashMap<String, String>,
        val startTime: Instant,
        val stopTime: Instant
    )

    override fun setTestInfo(testId: String, testName: String) {
        this.testId = testId
        this.testName = testName
        this.testStartTime = Clock.System.now()

        transaction {
            dbTestId = TestsTable.insert { table ->
                table[projectIdRow] = dbProjectId!!
                table[sessionIdRow] = MainConfig.sessionId
                table[identifierRow] = testId
                table[nameRow] = truncateString(testName, nameRow)
                table[startTimeRow] = testStartTime
            } get TestsTable.idRow
        }
    }

    override fun addStep(
        id: String,
        parentId: String,
        name: String,
        parameters: HashMap<String, String>,
        actionResult: ActionResult,
        screenData: ScreenData?,
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

    private fun saveSteps(id: String, parentStepId: Long? = null) {
        steps[id]?.forEach { stepItem ->
            val step = stepItem.value

            val stepId = StepsTable.insert { table ->
                table[testIdRow] = dbTestId!!
                table[parentStepIdRow] = parentStepId
                table[identifierRow] = stepItem.key
                table[nameRow] = truncateString(step.name, nameRow)
                table[statusRow] = step.result.getStatus().value
                table[messageRow] =
                    step.result.getMessage()?.let { truncateString(it, columnWithNullableString = messageRow) }
                table[traceRow] = step.result.getTrace()?.let { truncateString(it, columnWithNullableString = traceRow) }
                table[startTimeRow] = step.startTime
                table[endTimeRow] = step.stopTime
            } get StepsTable.idRow

            step.parameters.forEach { parameter ->
                StepParametersTable.insert { table ->
                    table[stepIdRow] = stepId
                    table[nameRow] = parameter.key
                    table[valueRow] = truncateString(parameter.value, valueRow)
                }
            }

            saveSteps(stepItem.key, stepId)
        }
    }

    private fun truncateString(
        text: String,
        columnWithString: Column<String>? = null,
        columnWithNullableString: Column<String?>? = null
    ): String {
        val column = columnWithString ?: columnWithNullableString
        if (column != null) {
            val colLength = (column.columnType as VarCharColumnType).colLength
            val textLength = text.length
            if (textLength > colLength) {
                Logger.warning(
                    "Table: ${column.table.tableName}, column: ${column.name}. The value length ($textLength) exceeds the column size ($colLength) in the database and will be truncated.",
                    "DatabaseReporter"
                )
                return StringUtils.abbreviate(text, colLength)
            }
        }
        return text
    }
}