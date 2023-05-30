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

class CheckElementValueAction(elementName: String, expectedValue: String) : ActionReturn(), Action {
    private val elementName: String
    private val expectedValue: String
    private var elementValue: String? = null

    init {
        this.elementName = elementName
        this.expectedValue = ValueStorage.replace(expectedValue)
    }

    override fun getName(): String {
        return Localization.get("CheckElementValueAction.DefaultName", elementName)
    }

    override fun execute(): ActionResult {
        try {
            val locator = PageStorage.getCurrentPage()?.getElementLocator(elementName)
            if (locator.isNullOrEmpty())
                return fail(Localization.get("General.ElementLocatorNotSpecified"))
            elementValue = DriverSession.getSession().getElementValue(locator)
            if (elementValue != expectedValue)
                return fail(Localization.get("CheckElementValueAction.ElementValueNotMatch", elementValue, expectedValue))
        } catch (e: Exception) {
            return fail(Localization.get("CheckElementValueAction.GeneralError", e.message))
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = elementName
        parameters["elementValue"] = elementValue.toString()
        parameters["expectedValue"] = expectedValue
        return parameters
    }
}

fun checkElementValue(elementName: String, expectedValue: String, function: (CheckElementValueAction.() -> Unit)? = null): ActionData {
    val action = CheckElementValueAction(elementName, expectedValue)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    return ActionData(result, parameters, name)
}
