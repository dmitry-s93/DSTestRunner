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

package action.actions.web

import action.Action
import action.ActionData
import action.ActionResult
import action.ActionReturn
import config.Localization
import driver.DriverSession
import storage.ValueStorage
import utils.ResourceUtils


class ExecuteJavaScriptAction() : ActionReturn(), Action {
    var script: String? = null
    var jsFile: String? = null
    private var jsFileParameters: HashMap<String, String> = HashMap()

    override fun getName(): String {
        return Localization.get("ExecuteJavaScriptAction.DefaultName")
    }

    override fun execute(): ActionResult {
        try {
            if (script == null && jsFile != null) script = ResourceUtils().getResourceByName("js/$jsFile")
            if (script.isNullOrEmpty()) return broke(Localization.get("ExecuteJavaScriptAction.ScriptIsEmpty"))

            if (!jsFile.isNullOrEmpty()) {
                jsFileParameters.forEach {
                    script = script!!.replace("{${it.key}}", it.value)
                }
            } else {
                script = ValueStorage.replace(script!!)
            }

            DriverSession.getSession().executeJavaScript(script!!)
        } catch (e: Exception) {
            return broke(Localization.get("ExecuteJavaScriptAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["script"] = script.toString()
        jsFileParameters.forEach {
            parameters[it.key] = it.value
        }
        return parameters
    }

    /**
     * Specifies the parameter to replace in the js file
     */
    fun jsFileParameter(param: String, value: String) {
        jsFileParameters[param] = ValueStorage.replace(value)
    }
}

/**
 * Executes JavaScript
 */
fun executeJavaScript(function: (ExecuteJavaScriptAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = ExecuteJavaScriptAction()
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}