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

class IsExistAction(private val element: Element) : ActionReturn(), Action {
    private var elementLocator: Locator = Locator(element.locator.value, element.locator.type)
    private val locatorArguments = ArrayList<String>()
    var scrollToFindElement: Boolean? = null
    var waitAtMostMillis: Long? = null

    override fun getName(): String {
        return Localization.get("IsExistAction.DefaultName", element.displayName)
    }

    override fun execute(): ActionResult {
        try {
            elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            if (!DriverSession.getSession().isExist(elementLocator, scrollToFindElement, waitAtMostMillis))
                return fail(Localization.get("IsExistAction.ElementIsMissing"))
        } catch (e: Exception) {
            return broke(Localization.get("IsExistAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = element.displayName
        parameters["elementLocator"] = elementLocator.value
        if (waitAtMostMillis != null)
            parameters["waitAtMostMillis"] = waitAtMostMillis.toString()
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
 * Checks for the presence of the [element] on the page
 */
fun isExist(element: Element, function: (IsExistAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = IsExistAction(element)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

/**
 * Checks for the presence of the element [elementName] on the page
 */
fun isExist(elementName: String, function: (IsExistAction.() -> Unit)? = null): ActionData {
    val (element, result) = ActionHelper().getElement(elementName)
    if (element != null)
        return isExist(element, function)
    val name = Localization.get("IsExistAction.DefaultName", elementName)
    val time = System.currentTimeMillis()
    return ActionData(result!!, HashMap(), name, time, time)
}