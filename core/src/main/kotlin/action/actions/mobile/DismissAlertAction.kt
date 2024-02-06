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

package action.actions.mobile

import action.Action
import action.ActionData
import action.ActionResult
import action.ActionReturn
import config.Localization
import driver.DriverSession

class DismissAlertAction(private val buttonLabel: String?) : ActionReturn(), Action {
    override fun getName(): String {
        return Localization.get("DismissAlertAction.DefaultName")
    }

    override fun execute(): ActionResult {
        try {
            DriverSession.getSession().dismissAlert(buttonLabel)
        } catch (e: Exception) {
            return broke(Localization.get("DismissAlertAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        if (buttonLabel != null)
            parameters["buttonLabel"] = buttonLabel
        return parameters
    }
}

/**
 * Dismisses an alert
 * [buttonLabel] - button to press (for iOS only)
 */
fun dismissAlert(buttonLabel: String? = null, function: (DismissAlertAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = DismissAlertAction(buttonLabel)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}