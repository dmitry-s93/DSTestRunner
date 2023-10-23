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
import test.element.Locator

class SetValueToStorageAction(private val name: String, private var value: String?, private val elementName: String?) : ActionReturn(), Action {
    private var elementLocator: Locator? = null
    private val locatorArguments = ArrayList<String>()

    override fun getName(): String {
        return Localization.get("SetValueToStorageAction.DefaultName", value, name)
    }

    override fun execute(): ActionResult {
        try {
            if (name.isEmpty())
                return broke(Localization.get("SetValueToStorageAction.NameCanNotBeEmpty"))
            if (value == null && elementName == null)
                return broke(Localization.get("SetValueToStorageAction.RequiredParameterNotSpecified"))
            if (value != null)
                value = ValueStorage.replace(value!!)
            if (elementName != null) {
                val pageData = PageStorage.getCurrentPage() ?: return broke(Localization.get("General.CurrentPageIsNotSet"))
                val element = pageData.getElement(elementName)
                    ?: return broke(Localization.get("General.ElementIsNotSetOnPage", elementName, pageData.getPageName()))
                elementLocator = element.getLocator().withReplaceArgs(*locatorArguments.toArray())
                if (elementLocator!!.value.isEmpty())
                    return broke(Localization.get("General.ElementLocatorNotSpecified"))
                value = DriverSession.getSession().getElementValue(elementLocator!!)
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
        if (elementName != null)
            parameters["elementName"] = elementName.toString()
        if (elementLocator != null)
            parameters["elementLocator"] = elementLocator!!.value
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
fun setValueToStorage(name: String, value: String? = null, elementName: String? = null, function: (SetValueToStorageAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = SetValueToStorageAction(name, value, elementName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val actionName = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, actionName, startTime, stopTime)
}
