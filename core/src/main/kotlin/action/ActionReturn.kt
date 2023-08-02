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

import config.ScreenshotConfig
import driver.DriverSession
import logger.Logger
import java.awt.image.BufferedImage

open class ActionReturn {
    open fun pass(): ActionResult {
        return ActionResult(ActionStatus.PASSED, null)
    }

    open fun fail(message: String, trace: String? = null): ActionResult {
        return ActionResult(ActionStatus.FAILED, message, trace, getScreenshot())
    }

    open fun broke(message: String, trace: String? = null): ActionResult {
        return ActionResult(ActionStatus.BROKEN, message, trace, getScreenshot())
    }

    private fun getScreenshot(): BufferedImage? {
        try {
            if (ScreenshotConfig.takeScreenshotOnError)
                return DriverSession.getSession().getScreenshot().image
            return null
        } catch (e: Exception) {
            Logger.error("Failed to take a screenshot of the error: ${e.message}")
            return null
        }
    }
}