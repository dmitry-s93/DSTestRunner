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
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import storage.ValueStorage
import test.element.Locator
import test.page.Element
import java.time.Duration

class CheckElementValueAction(private val element: Element, expectedValue: String) : ActionReturn(), Action {
    private var elementLocator: Locator = Locator(element.locator.value, element.locator.type)
    private val expectedValue: String = ValueStorage.replace(expectedValue)
    private var elementValue: String? = null
    private val locatorArguments = ArrayList<String>()
    private var waitForExpectedValue: Boolean = false
    private var regexMode: Boolean = false
    var scrollToFindElement: Boolean? = null

    override fun getName(): String {
        return Localization.get("CheckElementValueAction.DefaultName", element.displayName)
    }

    override fun execute(): ActionResult {
        try {
            elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            val driverSession = DriverSession.getSession()
            if (waitForExpectedValue) {
                try {
                    Awaitility.await()
                        .atLeast(Duration.ofMillis(0))
                        .pollDelay(Duration.ofMillis(100))
                        .atMost(Duration.ofMillis(driverSession.getElementTimeout()))
                        .until {
                            elementValue = driverSession.getElementValue(elementLocator, scrollToFindElement)
                            isValueMatch(elementValue!!, expectedValue)
                        }
                } catch (_: ConditionTimeoutException) {
                    return fail(Localization.get("CheckElementValueAction.ElementValueNotMatch", elementValue, expectedValue))
                }
            } else {
                elementValue = driverSession.getElementValue(elementLocator, scrollToFindElement)
                if (!isValueMatch(elementValue!!, expectedValue))
                    return fail(Localization.get("CheckElementValueAction.ElementValueNotMatch", elementValue, expectedValue))
            }
        } catch (e: Exception) {
            return broke(Localization.get("CheckElementValueAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    private fun isValueMatch(currentValue: String, expectedValue: String): Boolean {
        if (regexMode)
            return currentValue.matches(Regex(expectedValue))
        return currentValue == expectedValue
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = element.displayName
        parameters["elementLocator"] = elementLocator.value
        parameters["elementValue"] = elementValue.toString()
        if (expectedValue != elementValue)
            parameters["expectedValue"] = expectedValue
        parameters["waitForExpectedValue"] = waitForExpectedValue.toString()
        if (regexMode)
            parameters["regexMode"] = "true"
        return parameters
    }

    /**
     * Substitutes the argument into the element locator
     */
    fun locatorArgument(value: String) {
        locatorArguments.add(ValueStorage.replace(value))
    }

    /**
     * Wait for expected value
     */
    fun waitForExpectedValue() {
        waitForExpectedValue = true
    }

    /**
     * Check expected value as regular expression
     */
    fun regexMode() {
        regexMode = true
    }
}

fun checkElementValue(
    element: Element,
    expectedValue: String,
    function: (CheckElementValueAction.() -> Unit)? = null
): ActionData {
    val startTime = System.currentTimeMillis()
    val action = CheckElementValueAction(element, expectedValue)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

@Deprecated("Use an Element class object instead of a string")
fun checkElementValue(
    elementName: String,
    expectedValue: String,
    function: (CheckElementValueAction.() -> Unit)? = null
): ActionData {
    val (element, result) = ActionHelper().getElement(elementName)
    if (element != null)
        return checkElementValue(element, expectedValue, function)
    val name = Localization.get("CheckElementValueAction.DefaultName", elementName)
    val time = System.currentTimeMillis()
    return ActionData(result!!, HashMap(), name, time, time)
}