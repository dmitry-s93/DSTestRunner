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
import action.Result
import logger.TestLogger
import reporter.ReporterSession
import test.error.TestFailedError

open class TestBuilder(id: String, name: String) {
    private val testId: String
    private val testName: String
    private var parentId: String
    private var required: Boolean = true
    private var result = Result.PASS
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
        function()
        before = false
    }

    fun steps(function: () -> Unit) {
        val thisRequired = required
        try {
            function()
        } catch (e: TestFailedError) {
            if (thisRequired && testId != parentId)
                throw e
        } finally {
            required = true
        }
    }

    fun after(function: () -> Unit) {
        after = true
        function()
        after = false
    }

    fun step(id: String, actionData: ActionData) {
        var stepName = name
        if (stepName.isEmpty())
            stepName = actionData.getName()
        val stepResult = actionData.getResult()
        TestLogger.log(id, parentId, stepName, stepResult)
        if (!before && !after)
            ReporterSession.getSession().addStep(id, parentId, stepName, actionData)
        name = ""
        if (stepResult.result() == Result.FAIL)
            result = Result.FAIL
        if (required && stepResult.result() == Result.FAIL)
            throw TestFailedError()
        required = true
    }

    fun step(id: String, name: String, function: () -> Unit) {
        val currentTestId = parentId
        val currentResult = result
        parentId += ".$id"
        result = Result.PASS
        try {
            function()
        } catch (e: TestFailedError) {
            throw e
        } finally {
            parentId = currentTestId
            TestLogger.log(id, parentId, name, ActionResult(result))
            ReporterSession.getSession().addMultiStep(id, parentId, name, ActionResult(result))
            if (result != Result.FAIL)
                result = currentResult
        }
    }

    fun optional() {
        required = false
    }
}


