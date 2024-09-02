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

package reporter.csv

import action.ActionResult
import action.ImageComparisonData
import com.opencsv.CSVWriterBuilder
import config.reporter.CsvReporterConfig
import reporter.Reporter
import utils.ImageUtils
import java.io.FileWriter
import java.nio.file.Paths


@Suppress("unused")
class CsvReporter : Reporter {
    private var testId: String? = null
    private var testName: String? = null

    private val reportData: MutableList<Array<String>> = ArrayList()

    override fun setTestInfo(testId: String, testName: String) {
        this.testId = testId
        this.testName = testName
        reportData.add(arrayOf("ID", "Name", "Status", "Error"))
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
        val message = if (actionResult.getMessage() == null) "" else actionResult.getMessage().toString()
        val screenshot = actionResult.getErrorImage()
        if (screenshot != null) {
            val fileName = "$parentId.$id $name.png".replace(" ", "_")
            ImageUtils().saveImage(screenshot, Paths.get(CsvReporterConfig.reportDir, fileName).toFile())
        }
        reportData.add(arrayOf("$parentId.$id", name, actionResult.getStatus().toString(), message))
    }

    override fun quit() {
        val fileName = "$testId $testName.csv".replace(" ", "_")
        val path = Paths.get(CsvReporterConfig.reportDir, fileName)
        val writer = CSVWriterBuilder(FileWriter(path.toFile())).withSeparator('\t').build()
        writer.writeAll(reportData)
        writer.close()
    }
}