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

package action.actions

import action.Action
import action.ActionData
import action.ActionResult
import action.ActionReturn
import config.Localization
import driver.DriverSession
import storage.PageStorage
import storage.ValueStorage

class SetFieldValueAction(private val fieldName: String, value: String) : ActionReturn(), Action {
    private val value: String
    private val elementLocator: String? = PageStorage.getCurrentPage()?.getElementLocator(fieldName)
    private var sequenceMode: Boolean = false

    init {
        this.value = ValueStorage.replace(value)
    }

    override fun getName(): String {
        return Localization.get("SetFieldValueAction.DefaultName", value, fieldName)
    }

    override fun execute(): ActionResult {
        try {
            if (elementLocator.isNullOrEmpty())
                return fail(Localization.get("General.ElementLocatorNotSpecified"))
            DriverSession.getSession().setValue(elementLocator, value, sequenceMode)
        } catch (e: Exception) {
            return fail(Localization.get("SetFieldValueAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = fieldName
        parameters["elementLocator"] = elementLocator.toString()
        parameters["value"] = value
        parameters["sequenceMode"] = sequenceMode.toString()
        return parameters
    }

    fun sequenceMode() {
        sequenceMode = true
    }
}

fun setFieldValue(fieldName: String, value: String, function: (SetFieldValueAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = SetFieldValueAction(fieldName, value)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}
