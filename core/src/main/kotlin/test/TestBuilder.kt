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
import logger.TestLogger
import reporter.ReporterSession
import test.error.TestFailedError

open class TestBuilder(id: String, name: String) {
    private val testId: String
    private val testName: String
    private var parentId: String
    private var required: Boolean = true
    private var status = ActionStatus.PASSED
    private var before: Boolean = false
    private var after: Boolean = false

    var name: String = ""

    init {
        testId = id
        parentId = id
        testName = name
        ReporterSession.getSession().setTestInfo(testId, testName)
    }

    fun before(function: () -> Unit) {
        before = true
        executeSteps(function)
        before = false
    }

    fun steps(function: () -> Unit) {
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
            if (thisRequired && testId != parentId)
                throw e
        } finally {
            required = thisRequired
        }
    }

    fun step(id: String, actionData: ActionData) {
        var stepName = name
        if (stepName.isEmpty())
            stepName = actionData.getName()
        val stepResult = actionData.getResult()
        val stepParams = actionData.getParameters()
        val startTime = actionData.getStartTime()
        val stopTime = actionData.getStopTime()
        TestLogger.log(id, parentId, stepName, stepResult)
        if (!before && !after)
            ReporterSession.getSession().addStep(id, parentId, stepName, stepParams, stepResult, startTime, stopTime)
        name = ""
        if (stepResult.status() > status)
            status = stepResult.status()
        if (required && (stepResult.status() == ActionStatus.FAILED || stepResult.status() == ActionStatus.BROKEN))
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
        } catch (e: TestFailedError) {
            throw e
        } finally {
            val stopTime = System.currentTimeMillis()
            parentId = currentTestId
            TestLogger.log(id, parentId, name, ActionResult(status))
            ReporterSession.getSession().addStep(id, parentId, name, HashMap(), ActionResult(status), startTime, stopTime)
            if (currentStatus > status)
                status = currentStatus
            required = true
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