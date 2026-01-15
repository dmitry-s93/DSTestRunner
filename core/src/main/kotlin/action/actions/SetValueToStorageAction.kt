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

class SetValueToStorageAction(private val name: String, private var value: String?, private val element: Element?, private val attributeName: String?) : ActionReturn(), Action {
    private var elementLocator: Locator? = null
    private val locatorArguments = ArrayList<String>()

    override fun getName(): String {
        return Localization.get("SetValueToStorageAction.DefaultName", value, name)
    }

    override fun execute(): ActionResult {
        try {
            if (name.isEmpty())
                return broke(Localization.get("SetValueToStorageAction.NameCanNotBeEmpty"))
            if (value == null && element == null)
                return broke(Localization.get("SetValueToStorageAction.RequiredParameterNotSpecified"))
            if (value != null)
                value = ValueStorage.replace(value!!)
            if (element != null) {
                elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
                if (elementLocator!!.value.isEmpty())
                    return broke(Localization.get("General.ElementLocatorNotSpecified"))
                value = DriverSession.getSession().getElementValue(elementLocator!!, attributeName = attributeName)
            }
            ValueStorage.setValue(name, value!!)
            return pass()
        } catch (e: Exception) {
            return broke(Localization.get("SetValueToStorageAction.GeneralError", e.message), e.stackTraceToString())
        }
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["name"] = name
        parameters["value"] = value.toString()
        if (element != null)
            parameters["elementName"] = element.toString()
        if (elementLocator != null)
            parameters["elementLocator"] = elementLocator!!.value
        if (attributeName != null)
            parameters["attributeName"] = attributeName
        return parameters
    }

    /**
     * Substitutes the argument into the element locator
     */
    fun locatorArgument(value: String) {
        locatorArguments.add(ValueStorage.replace(value))
    }
}

/**
 * Adds the value in the test storage
 *
 * Requires [value] or [elementName]
 */
fun setValueToStorage(name: String, value: String? = null, elementName: String? = null, attributeName: String? = null, function: (SetValueToStorageAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    if (elementName != null) {
        val (element, result) = ActionHelper().getElement(elementName)
        if (element != null)
            return setValueToStorage(name, element, attributeName, function)
        val actionName = Localization.get("SetValueToStorageAction.DefaultName", value, name)
        val stopTime = System.currentTimeMillis()
        return ActionData(result!!, HashMap(), actionName, startTime, stopTime)
    }
    val action = SetValueToStorageAction(name, value, null,  attributeName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val actionName = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, actionName, startTime, stopTime)
}

/**
 * Adds the value in the test storage
 *
 * Requires [element]
 */
fun setValueToStorage(name: String, element: Element, attributeName: String? = null, function: (SetValueToStorageAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = SetValueToStorageAction(name, null, element, attributeName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val actionName = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, actionName, startTime, stopTime)
}
