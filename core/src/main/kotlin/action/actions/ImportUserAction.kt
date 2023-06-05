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
import config.UserListConfig
import storage.ValueStorage

class ImportUserAction(name: String, prefix: String) : ActionReturn(), Action {
    private val name: String
    private val prefix: String
    private val parameters: HashMap<String, String> = HashMap()

    init {
        this.name = name
        this.prefix = prefix
    }

    override fun getName(): String {
        return Localization.get("ImportUserAction.DefaultName", name)
    }

    override fun execute(): ActionResult {
        if (name.isEmpty())
            return fail(Localization.get("ImportUserAction.NameCanNotBeEmpty"))
        val userData = UserListConfig.getUser(name)
            ?: return fail(Localization.get("ImportUserAction.UserNotFoundInConfig", name))
        userData.forEach { (fieldName, fieldValue) ->
            ValueStorage.setValue("${prefix}_${fieldName}", fieldValue)
            parameters["${prefix}_${fieldName}"] = fieldValue
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        parameters["user"] = name
        return parameters
    }
}

fun importUser(name: String, prefix: String): ActionData {
    val startTime = System.currentTimeMillis()
    val action = ImportUserAction(name, prefix)
    val result = action.execute()
    val parameters = action.getParameters()
    val actionName = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, actionName, startTime, stopTime)
}
