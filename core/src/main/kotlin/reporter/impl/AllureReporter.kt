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

import action.ActionResult
import action.Result
import config.ReporterConfig
import org.json.JSONArray
import org.json.JSONObject
import reporter.Reporter
import reporter.ReporterSession
import utils.StringUtils
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Paths
import java.util.*


@Suppress("unused")
class AllureReporter : Reporter {
    private lateinit var testId: String
    private lateinit var testName: String
    private var startTime: Long = 0

    private var description: String? = null
    private val labels = JSONArray()
    private val steps: LinkedHashMap<String, LinkedHashMap<String, JSONObject>> = LinkedHashMap()

    private var testResult: Result = Result.PASS

    override fun setTestInfo(testId: String, testName: String) {
        this.testId = testId
        this.testName = testName
        this.startTime = System.currentTimeMillis()
    }

    override fun addStep(
        id: String,
        parentId: String,
        name: String,
        parameters: HashMap<String, String>,
        actionResult: ActionResult,
        startTime: Long,
        stopTime: Long
    ) {
        val step = JSONObject()
        val result = actionResult.result()
        with(step) {
            put("name", name)
            put("status", getStatus(result))
            if (result == Result.FAIL) {
                put("statusDetails", getStatusDetails(actionResult))
                testResult = result
            }
            put("stage", "finished")
            put("attachments", getAttachments(actionResult))
            put("parameters", getParameters(parameters))
            put("start", startTime)
            put("stop", stopTime)
        }
        if (!steps.containsKey(parentId))
            steps[parentId] = LinkedHashMap()
        steps[parentId]?.put("$parentId.$id", step)
    }

    override fun quit() {
        val stopTime = System.currentTimeMillis()
        val uuid = UUID.randomUUID()

        addLabel("thread", Thread.currentThread().name)
        addLabel("framework", "DSTestRunner")
        addLabel("language", "kotlin")

        val result = JSONObject()
        with(result) {
            put("uuid", uuid)
            put("historyId", StringUtils().md5sum(testId))
            put("fullName", "$testId $testName".replace(" ", "_"))
            put("labels", labels)
            put("name", testName)
            put("status", getStatus(testResult))
            put("stage", "finished")
            put("description", description)
            put("steps", getSteps(testId))
            put("start", startTime)
            put("stop", stopTime)
        }
        val fileName = "$uuid-result.json"
        val path = Paths.get(ReporterConfig.getReportDir(), fileName)
        val file = FileWriter(path.toFile())
        file.write(result.toString())
        file.flush()
        file.close()
    }

    private fun getSteps(id: String): JSONArray {
        val jsonArray = JSONArray()
        steps[id]?.forEach {
            val step = it.value
            step.put("steps", getSteps(it.key))
            jsonArray.put(step)
        }
        return jsonArray
    }

    private fun addLabel(name: String, value: String) {
        labels.put(
            with(JSONObject()) {
                put("name", name)
                put("value", value)
            }
        )
    }

    fun epic(name: String) {
        addLabel("epic", name)
    }

    fun feature(name: String) {
        addLabel("feature", name)
    }

    fun story(name: String) {
        addLabel("story", name)
    }

    fun description(description: String) {
        this.description = description
    }

    private fun getStatus(result: Result): String {
        return when (result) {
            Result.PASS -> "passed"
            Result.FAIL -> "failed"
        }
    }

    private fun getStatusDetails(result: ActionResult): JSONObject {
        val statusDetails = JSONObject()
        with(statusDetails) {
            put("known", false)
            put("muted", false)
            put("flaky", false)
            put("message", result.message())
            put("trace", result.trace())
        }
        return statusDetails
    }

    private fun getParameters(parameters: HashMap<String, String>): JSONArray {
        val jsonArray = JSONArray()
        parameters.forEach {
            jsonArray.put(
                with(JSONObject()) {
                    put("name", it.key)
                    put("value", it.value)
                }
            )
        }
        return jsonArray
    }

    private fun getAttachments(actionResult: ActionResult): JSONArray {
        val attachments = JSONArray()
        val screenshot = actionResult.screenshot()
        if (screenshot != null) {
            val uuid = UUID.randomUUID()
            val fileName = "$uuid-attachment.png"
            attachments.put(
                with(JSONObject()) {
                    put("name", "Screenshot")
                    put("source", fileName)
                    put("type", "image/png")
                }
            )
            saveScreenshot(screenshot, fileName)
        }
        return attachments
    }

    private fun saveScreenshot(screenshot: ByteArray, fileName: String) {
        val path = Paths.get(ReporterConfig.getReportDir(), fileName)
        FileOutputStream(path.toFile()).use { stream -> stream.write(screenshot) }
    }
}

fun allure(function: AllureReporter.() -> Unit) {
    function.invoke(ReporterSession.getSession() as AllureReporter)
}