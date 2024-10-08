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

package test

import action.ActionData
import action.ActionResult
import action.ActionStatus
import action.ImageComparisonData
import logger.TestLogger
import reporter.ReporterHelper
import test.error.TestFailedError
import utils.ImageComparison

open class TestBuilder(id: String, name: String) {
    private val testId: String
    private val testName: String
    private var parentId: String
    private var required: Boolean = true
    private var status = ActionStatus.PASSED
    private var before: Boolean = false
    private var after: Boolean = false

    private var complexStepError = false

    var name: String = ""

    init {
        testId = id
        parentId = id
        testName = name
        ReporterHelper.setTestInfo(testId, testName)
    }

    fun before(function: () -> Unit) {
        before = true
        executeSteps(function)
        before = false
    }

    fun steps(function: () -> Unit) {
        if (complexStepError)
            return
        executeSteps(function)
    }

    fun after(function: () -> Unit) {
        after = true
        executeSteps(function)
        after = false
    }

    private fun executeSteps(function: () -> Unit) {
        val thisRequired = required
        required = true
        try {
            function()
        } catch (e: TestFailedError) {
            complexStepError = true
        } finally {
            required = thisRequired
        }
    }

    fun step(id: String, actionData: ActionData) {
        var stepName = name
        if (stepName.isEmpty())
            stepName = actionData.actionName
        var stepResult = actionData.actionResult
        val stepParams = actionData.actionParameters
        val screenshot = actionData.screenshot
        val startTime = actionData.startTime
        var stopTime = actionData.stopTime
        if (!(before || after) || stepResult.getStatus() != ActionStatus.PASSED) {
            var imageComparisonData: ImageComparisonData? = null
            if (screenshot != null && stepResult.getStatus() == ActionStatus.PASSED) {
                required = false
                val (compareStepResult, compareScreenData) = ImageComparison(id, testId, parentId, screenshot).compare()
                stepName += " \uD83D\uDDBC"
                stepResult = compareStepResult
                imageComparisonData = compareScreenData
                stopTime = System.currentTimeMillis()
            }
            ReporterHelper.addStep(id, parentId, stepName, stepParams, stepResult, imageComparisonData, startTime, stopTime)
        }
        TestLogger.log(id, parentId, stepName, stepResult)
        name = ""
        if (stepResult.getStatus() > status)
            status = stepResult.getStatus()
        if (required && (stepResult.getStatus() == ActionStatus.FAILED || stepResult.getStatus() == ActionStatus.BROKEN))
            throw TestFailedError()
        required = true
    }

    fun step(id: String, name: String, function: () -> Unit) {
        val currentTestId = parentId
        val currentStatus = status
        parentId += ".$id"
        status = ActionStatus.PASSED
        val startTime = System.currentTimeMillis()
        try {
            function()
            if (complexStepError && required)
                throw TestFailedError()
        } catch (e: TestFailedError) {
            throw e
        } finally {
            val stopTime = System.currentTimeMillis()
            parentId = currentTestId
            TestLogger.log(id, parentId, name, ActionResult(status))
            if (!(before || after) || status != ActionStatus.PASSED)
                ReporterHelper.addStep(id, parentId, name, HashMap(), ActionResult(status), null, startTime, stopTime)
            if (currentStatus > status)
                status = currentStatus
            required = true
            complexStepError = false
        }
    }

    /**
     * Makes the step optional.
     * The failure of an optional step does not prevent the test from continuing.
     */
    fun optional() {
        required = false
    }
}