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

package action

class ActionData(
    actionResult: ActionResult,
    actionParameters: HashMap<String, String>,
    actionName: String
) {
    private val actionResult: ActionResult
    private val actionParameters: HashMap<String, String>
    private val actionName: String

    init {
        this.actionResult = actionResult
        this.actionParameters = actionParameters
        this.actionName = actionName
    }

    fun getResult(): ActionResult {
        return actionResult
    }

    fun getParameters(): HashMap<String, String> {
        return actionParameters
    }

    fun getName(): String {
        return actionName
    }
}