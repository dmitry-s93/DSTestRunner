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

class ActionResult(
    result: Result = Result.FAILED,
    message: String? = null,
    trace: String? = null,
    screenshot: ByteArray? = null
) {
    private val result: Result
    private val message: String?
    private val trace: String?
    private val screenshot: ByteArray?

    init {
        this.result = result
        this.message = message
        this.trace = trace
        this.screenshot = screenshot
    }

    fun result(): Result {
        return result
    }

    fun message(): String? {
        return message
    }

    fun trace(): String? {
        return trace
    }

    /**
     * Returns a screenshot of the error
     */
    fun screenshot(): ByteArray? {
        return screenshot
    }
}