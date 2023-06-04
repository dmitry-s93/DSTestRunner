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

package logger

import action.ActionResult
import action.Result

class TestLogger {
    companion object {
        fun log(id: String, parentId: String, testName: String, actionResult: ActionResult) {
            val result = actionResult.result()
            val message = actionResult.message()

            val ansiColor = when (result) {
                Result.PASS -> Color.ANSI_GREEN
                Result.FAIL -> Color.ANSI_RED
            }

            var errorPart = ""
            if (message != null)
                errorPart = "\n${Color.ANSI_RED}$message${Color.ANSI_RESET}"

            println("${Logger.getTime()} ${Logger.getThreadId()} $ansiColor$result${Color.ANSI_RESET} --> ${Color.ANSI_CYAN}$parentId.$id $testName${Color.ANSI_RESET}$errorPart")
        }
    }
}