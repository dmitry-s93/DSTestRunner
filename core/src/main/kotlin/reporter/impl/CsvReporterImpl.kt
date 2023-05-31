/* Copyright 2023 DSTestRunner Contributors
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

package reporter.impl

import action.ActionData
import action.ActionResult
import com.opencsv.CSVWriterBuilder
import config.ReporterConfig
import reporter.Reporter
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Paths


class CsvReporterImpl : Reporter {
    private var testId: String? = null
    private var testName: String? = null

    private val reportData: MutableList<Array<String>> = ArrayList()

    override fun setTestInfo(testId: String, testName: String) {
        this.testId = testId
        this.testName = testName
        reportData.add(arrayOf("ID", "Name", "Result", "Error"))
    }

    override fun addStep(id: String, parentId: String, name: String, actionData: ActionData) {
        addLine(id, parentId, name, actionData.getResult())
    }

    override fun addMultiStep(id: String, parentId: String, name: String, actionResult: ActionResult) {
        addLine(id, parentId, name, actionResult)
    }

    private fun addLine(id: String, parentId: String, name: String, actionResult: ActionResult) {
        val actionError = if (actionResult.error() == null) "" else actionResult.error()
        if (actionResult.screenshot() != null)
            saveScreenshot(actionResult.screenshot()!!, "$parentId.$id $name.png".replace(" ", "_"))
        reportData.add(arrayOf("$parentId.$id", name, actionResult.result().toString(), actionError.toString()))
    }

    private fun saveScreenshot(screenshot: ByteArray, fileName: String) {
        val path = Paths.get(ReporterConfig.getReportDir(), fileName)
        FileOutputStream(path.toFile()).use { stream -> stream.write(screenshot) }
    }

    override fun quit() {
        val fileName = "$testId $testName.csv".replace(" ", "_")
        val path = Paths.get(ReporterConfig.getReportDir(), fileName)
        val writer = CSVWriterBuilder(FileWriter(path.toFile())).withSeparator('\t').build()
        writer.writeAll(reportData)
        writer.close()
    }
}