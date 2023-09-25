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

class IsDisabledAction(private val elementName: String) : ActionReturn(), Action {
    private var elementLocator: Locator? = null
    private var elementDisplayName: String = elementName
    private val locatorArguments = ArrayList<String>()

    override fun getName(): String {
        return Localization.get("IsDisabledAction.DefaultName", elementDisplayName)
    }

    override fun execute(): ActionResult {
        try {
            val pageData = PageStorage.getCurrentPage() ?: return broke(Localization.get("General.CurrentPageIsNotSet"))
            val element = pageData.getElement(elementName)
                ?: return broke(Localization.get("General.ElementIsNotSetOnPage", elementName, pageData.getPageName()))
            element.getDisplayName()?.let { elementDisplayName = it }
            elementLocator = element.getLocator().withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator!!.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            if (DriverSession.getSession().isEnabled(elementLocator!!))
                return fail(Localization.get("IsDisabledAction.ElementIsNotDisabled"))
        } catch (e: Exception) {
            return broke(Localization.get("IsDisabledAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = elementName
        parameters["elementLocator"] = elementLocator?.value.toString()
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
 * Checks if element [elementName] is disabled
 */
fun isDisabled(elementName: String, function: (IsDisabledAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = IsDisabledAction(elementName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}