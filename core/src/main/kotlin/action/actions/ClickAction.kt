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
import java.awt.Point

class ClickAction(private val element: Element) : ActionReturn(), Action {
    private var elementLocator: Locator = Locator(element.locator.value, element.locator.type)
    private val locatorArguments = ArrayList<String>()
    private val clickPoints = ArrayList<Pair<Point, Point?>>()

    override fun getName(): String {
        return Localization.get("ClickAction.DefaultName", element.displayName)
    }

    override fun execute(): ActionResult {
        try {
            elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            DriverSession.getSession().click(elementLocator, clickPoints)
        } catch (e: Exception) {
            return broke(Localization.get("ClickAction.GeneralError", e.message), e.stackTraceToString())
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

    /**
     * Sets the point to click inside the specified element.
     * You must specify the offset of the coordinate relative to the center of the element.
     */
    @Deprecated("Use performTouchAction instead")
    fun clickPoint(xOffset: Int, yOffset: Int) {
        clickPoints.add(Pair(Point(xOffset, yOffset), null))
    }

    @Deprecated("Use performTouchAction instead")
    fun clickAndMovePoint(startX: Int, startY: Int, endX: Int, endY: Int) {
        clickPoints.add(Pair(Point(startX, startY), Point(endX, endY)))
    }
}

fun click(element: Element, function: (ClickAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = ClickAction(element)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

fun click(elementName: String, function: (ClickAction.() -> Unit)? = null): ActionData {
    val (element, result) = ActionHelper().getElement(elementName)
    if (element != null)
        return click(element, function)
    val name = Localization.get("ClickAction.DefaultName", elementName)
    val time = System.currentTimeMillis()
    return ActionData(result!!, HashMap(), name, time, time)
}