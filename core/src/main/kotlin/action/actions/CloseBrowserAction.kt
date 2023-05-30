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

class CloseBrowserAction : ActionReturn(), Action {

    override fun getName(): String {
        return Localization.get("CloseBrowserAction.DefaultName")
    }

    override fun execute(): ActionResult {
        try {
            DriverSession.closeSession()
        } catch (e: Exception) {
            return fail(Localization.get("CloseBrowserAction.GeneralError", e.message))
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        return HashMap()
    }
}

fun closeBrowser(): ActionData {
    val action = CloseBrowserAction()
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    return ActionData(result, parameters, name)
}
