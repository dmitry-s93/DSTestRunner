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

import action.helper.Direction
import pazone.ashot.Screenshot
import test.element.Locator
import java.awt.Point

private const val notImplementedMessage = "Method not implemented for this driver instance"

interface Driver {
    fun getElementTimeout(): Long
    fun click(locator: Locator, points: ArrayList<Pair<Point, Point?>>? = null)
    fun checkLoadPage(url: String?, identifier: Locator?): Boolean
    fun getElementValue(locator: Locator): String
    fun getScreenshot(
        longScreenshot: Boolean = false,
        ignoredElements: Set<Locator> = HashSet(),
        screenshotAreas: Set<Locator> = HashSet()
    ): Screenshot

    fun setValue(locator: Locator, value: String, sequenceMode: Boolean = false, hideKeyboard: Boolean = true)
    fun setSelectValue(locator: Locator, value: String)
    fun isExist(locator: Locator): Boolean
    fun isNotExist(locator: Locator): Boolean
    fun isEnabled(locator: Locator): Boolean
    fun navigateBack()
    fun quit()

    // Web only
    fun setPage(url: String) {
        throw NotImplementedError(notImplementedMessage)
    }

    fun switchToWindow(url: String?): Boolean {
        throw NotImplementedError(notImplementedMessage)
    }

    fun closeWindow(url: String?): Boolean {
        throw NotImplementedError(notImplementedMessage)
    }

    fun getCurrentUrl(): String {
        throw NotImplementedError(notImplementedMessage)
    }

    fun uploadFile(locator: Locator, file: String) {
        throw NotImplementedError(notImplementedMessage)
    }

    fun executeJavaScript(script: String, vararg args: Any?): Any? {
        throw NotImplementedError(notImplementedMessage)
    }

    fun hoverOverElement(locator: Locator) {
        throw NotImplementedError(notImplementedMessage)
    }

    // Mobile only
    fun swipeElement(locator: Locator, direction: Direction) {
        throw NotImplementedError(notImplementedMessage)
    }

    fun hideKeyboard() {
        throw NotImplementedError(notImplementedMessage)
    }

    fun setLocation(latitude: Double, longitude: Double) {
        throw NotImplementedError(notImplementedMessage)
    }

    fun installApp(appPath: String?) {
        throw NotImplementedError(notImplementedMessage)
    }

    fun activateApp(bundleId: String?) {
        throw NotImplementedError(notImplementedMessage)
    }

    fun terminateApp(bundleId: String?) {
        throw NotImplementedError(notImplementedMessage)
    }

    fun removeApp(bundleId: String?) {
        throw NotImplementedError(notImplementedMessage)
    }

    fun isAppInstalled(bundleId: String?): Boolean {
        throw NotImplementedError(notImplementedMessage)
    }
}