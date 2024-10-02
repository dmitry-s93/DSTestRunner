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
import action.helper.ActionHelper
import config.Localization
import driver.DriverSession
import storage.ValueStorage
import test.element.Locator
import test.page.Element

class SetFieldValueAction(private val element: Element, value: String) : ActionReturn(), Action {
    private val value: String = ValueStorage.replace(value)
    private var elementLocator: Locator = Locator(element.locator.value, element.locator.type)
    private var sequenceMode: Boolean = false
    private val locatorArguments = ArrayList<String>()
    @Suppress("MemberVisibilityCanBePrivate")
    var hideKeyboard: Boolean = true

    override fun getName(): String {
        return Localization.get("SetFieldValueAction.DefaultName", value, element.displayName)
    }

    override fun execute(): ActionResult {
        try {
            elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            DriverSession.getSession().setValue(elementLocator, value, sequenceMode, hideKeyboard)
        } catch (e: Exception) {
            return broke(Localization.get("SetFieldValueAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = element.displayName
        parameters["elementLocator"] = elementLocator.value
        parameters["value"] = value
        parameters["sequenceMode"] = sequenceMode.toString()
        return parameters
    }

    fun sequenceMode() {
        sequenceMode = true
    }

    /**
     * Substitutes the argument into the element locator
     */
    fun locatorArgument(value: String) {
        locatorArguments.add(ValueStorage.replace(value))
    }
}

fun setFieldValue(element: Element, value: String, function: (SetFieldValueAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = SetFieldValueAction(element, value)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

@Deprecated("Use an Element class object instead of a string")
fun setFieldValue(elementName: String, value: String, function: (SetFieldValueAction.() -> Unit)? = null): ActionData {
    val (element, result) = ActionHelper().getElement(elementName)
    if (element != null)
        return setFieldValue(element, value, function)
    val name = Localization.get("SetFieldValueAction.DefaultName", value, elementName)
    val time = System.currentTimeMillis()
    return ActionData(result!!, HashMap(), name, time, time)
}