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
import storage.ValueStorage

class SetValueToStorageAction(name: String, value: String) : ActionReturn(), Action {
    private val name: String
    private val value: String

    init {
        this.name = name
        this.value = value
    }

    override fun getName(): String {
        return Localization.get("SetValueToStorageAction.DefaultName", value, name)
    }

    override fun execute(): ActionResult {
        if (name.isEmpty())
            return fail(Localization.get("SetValueToStorageAction.NameCanNotBeEmpty"))
        ValueStorage.setValue(name, value)
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["name"] = name
        parameters["value"] = value
        return parameters
    }
}

fun setValueToStorage(name: String, value: String): ActionData {
    val action = SetValueToStorageAction(name, value)
    val result = action.execute()
    val parameters = action.getParameters()
    val actionName = action.getName()
    return ActionData(result, parameters, actionName)
}
