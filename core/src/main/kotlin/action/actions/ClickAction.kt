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
import java.awt.Point
import java.util.*

class ClickAction(private val elementName: String) : ActionReturn(), Action {
    private var elementLocator: String? = null
    private val locatorArguments = ArrayList<String>()
    private val clickPoints = ArrayList<Point>()

    override fun getName(): String {
        return Localization.get("ClickAction.DefaultName", elementName)
    }

    override fun execute(): ActionResult {
        try {
            val pageData = PageStorage.getCurrentPage() ?: return broke(Localization.get("General.CurrentPageIsNotSet"))
            if (!pageData.isElementExist(elementName))
                return broke(Localization.get("General.ElementIsNotSetOnPage", elementName, pageData.getPageName()))
            elementLocator = pageData.getElementLocator(elementName)
            if (elementLocator.isNullOrEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            elementLocator = String.format(elementLocator!!, *locatorArguments.toArray())
            DriverSession.getSession().click(elementLocator!!, clickPoints)
        } catch (e: Exception) {
            return broke(Localization.get("ClickAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = elementName
        parameters["elementLocator"] = elementLocator.toString()
        if (clickPoints.isNotEmpty()) {
            val points = StringBuilder()
            clickPoints.forEach {
                points.append("[${it.x},${it.y}]")
            }
            parameters["clickPoints"] = points.toString()
        }
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
    fun clickPoint(xOffset: Int, yOffset: Int) {
        clickPoints.add(Point(xOffset, yOffset))
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