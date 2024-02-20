/* Copyright 2024 DSTestRunner Contributors
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

enum class ActionType {
    PAUSE,
    POINTER_DOWN,
    POINTER_UP,
    POINTER_MOVE
}

class TouchAction(val actionType: ActionType, val point: Point? = null, val millis: Long? = null)

class PerformTouchAction(private val element: Element) : ActionReturn(), Action {
    private lateinit var elementLocator: Locator
    private val locatorArguments = ArrayList<String>()
    private val touchActions: MutableList<MutableList<TouchAction>> = mutableListOf()

    override fun getName(): String {
        return Localization.get("PerformTouchAction.DefaultName", element.displayName)
    }

    override fun execute(): ActionResult {
        try {
            elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            DriverSession.getSession().performTouchAction(elementLocator, touchActions)
        } catch (e: Exception) {
            return broke(Localization.get("PerformTouchAction.GeneralError", e.message), e.stackTraceToString())
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

    fun addAction(actionList: PointerActionFactory) {
        touchActions.add(actionList.actions)
    }

    fun pointer(): PointerActionFactory {
        return PointerActionFactory()
    }

    class PointerActionFactory(val actions: MutableList<TouchAction> = mutableListOf()) {
        fun pause(millis: Long = 500): PointerActionFactory {
            actions.add(TouchAction(ActionType.PAUSE, millis = millis))
            return PointerActionFactory(actions)
        }

        fun down(): PointerActionFactory {
            actions.add(TouchAction(ActionType.POINTER_DOWN))
            return PointerActionFactory(actions)
        }

        fun up(): PointerActionFactory {
            actions.add(TouchAction(ActionType.POINTER_UP))
            return PointerActionFactory(actions)
        }

        fun move(x: Int, y: Int, millis: Long? = null): PointerActionFactory {
            actions.add(TouchAction(ActionType.POINTER_MOVE, point = Point(x, y), millis = millis))
            return PointerActionFactory(actions)
        }

        fun click(x: Int, y: Int): PointerActionFactory {
            actions.add(TouchAction(ActionType.POINTER_MOVE, point = Point(x, y)))
            actions.add(TouchAction(ActionType.POINTER_DOWN))
            actions.add(TouchAction(ActionType.POINTER_UP))
            return PointerActionFactory(actions)
        }

        fun clickAndMove(startX: Int, startY: Int, endX: Int, endY: Int, millis: Long? = null): PointerActionFactory {
            actions.add(TouchAction(ActionType.POINTER_MOVE, point = Point(startX, startY)))
            actions.add(TouchAction(ActionType.POINTER_DOWN))
            actions.add(TouchAction(ActionType.POINTER_MOVE, point = Point(endX, endY), millis = millis))
            actions.add(TouchAction(ActionType.POINTER_UP))
            return PointerActionFactory(actions)
        }
    }
}

fun performTouchAction(element: Element, function: (PerformTouchAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = PerformTouchAction(element)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

fun performTouchAction(elementName: String, function: (PerformTouchAction.() -> Unit)? = null): ActionData {
    val (element, result) = ActionHelper().getElement(elementName)
    if (element != null)
        return performTouchAction(element, function)
    val name = Localization.get("PerformTouchAction.DefaultName", elementName)
    val time = System.currentTimeMillis()
    return ActionData(result!!, HashMap(), name, time, time)
}