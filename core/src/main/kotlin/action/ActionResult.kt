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

import java.awt.image.BufferedImage

class ActionResult(
    private val status: ActionStatus = ActionStatus.FAILED,
    private val message: String? = null,
    private val trace: String? = null,
    private val errorImage: BufferedImage? = null
) {
    fun getStatus(): ActionStatus {
        return status
    }

    fun getMessage(): String? {
        return message
    }

    fun getTrace(): String? {
        return trace
    }

    /**
     * Returns a screenshot of the error
     */
    fun getErrorImage(): BufferedImage? {
        return errorImage
    }
}