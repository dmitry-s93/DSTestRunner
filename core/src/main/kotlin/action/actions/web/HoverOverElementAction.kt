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
import action.helper.ActionHelper
import config.Localization
import driver.DriverSession
import storage.ValueStorage
import test.element.Locator
import test.page.Element

class HoverOverElementAction(private val element: Element) : ActionReturn(), Action {
    private var elementLocator: Locator = Locator(element.locator.value, element.locator.type)
    private val locatorArguments = ArrayList<String>()

    override fun getName(): String {
        return Localization.get("HoverOverElementAction.DefaultName", element.displayName)
    }

    override fun execute(): ActionResult {
        try {
            elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            DriverSession.getSession().hoverOverElement(elementLocator)
        } catch (e: Exception) {
            return broke(Localization.get("HoverOverElementAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = element.displayName
        parameters["elementLocator"] = elementLocator.value
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
 * Moves the cursor over an element
 */
fun hoverOverElement(element: Element, function: (HoverOverElementAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = HoverOverElementAction(element)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

/**
 * Moves the cursor over an element
 */
@Deprecated("Use an Element class object instead of a string")
fun hoverOverElement(elementName: String, function: (HoverOverElementAction.() -> Unit)? = null): ActionData {
    val (element, result) = ActionHelper().getElement(elementName)
    if (element != null)
        return hoverOverElement(element, function)
    val name = Localization.get("HoverOverElementAction.DefaultName", elementName)
    val time = System.currentTimeMillis()
    return ActionData(result!!, HashMap(), name, time, time)
}