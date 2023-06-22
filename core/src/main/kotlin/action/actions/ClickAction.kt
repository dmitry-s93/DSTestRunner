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

class ClickAction(private val elementName: String) : ActionReturn(), Action {
    private var elementLocator: String? = PageStorage.getCurrentPage()?.getElementLocator(elementName)
    private val locatorArguments = ArrayList<String>()

    override fun getName(): String {
        return Localization.get("ClickAction.DefaultName", elementName)
    }

    override fun execute(): ActionResult {
        try {
            if (elementLocator.isNullOrEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            elementLocator = String.format(elementLocator!!, *locatorArguments.toArray())
            DriverSession.getSession().click(elementLocator!!)
        } catch (e: Exception) {
            return broke(Localization.get("ClickAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = elementName
        parameters["elementLocator"] = elementLocator.toString()
        return parameters
    }

    /**
     * Substitutes the argument into the element locator
     */
    fun locatorArgument(value: String) {
        locatorArguments.add(ValueStorage.replace(value))
    }
}

fun click(elementName: String, function: (ClickAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = ClickAction(elementName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}