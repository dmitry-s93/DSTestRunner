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

package driver

import logger.Logger
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import java.awt.Point

class DriverHelper {
    fun handleStaleElementReferenceException(logSource: String, numberOfAttempts: Int, function: () -> Unit) {
        try {
            function()
        } catch (e: StaleElementReferenceException) {
            if (numberOfAttempts > 0) {
                Logger.info("Stale element reference. Retrying.", logSource)
                return handleStaleElementReferenceException(logSource, numberOfAttempts - 1) {
                    function()
                }
            }
            throw e
        }
    }

    fun getElementCenter(element: WebElement): Point {
        val elementLocation = element.location
        val elementSize = element.size
        return Point(
            elementLocation.x + (elementSize.width / 2),
            elementLocation.y + (elementSize.height / 2)
        )
    }
}