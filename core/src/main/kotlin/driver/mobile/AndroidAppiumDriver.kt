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


package driver.mobile

import config.AppiumDriverConfig
import driver.Driver
import io.appium.java_client.android.AndroidDriver
import pazone.ashot.Screenshot
import test.element.Locator
import java.awt.Point
import java.net.URL
import java.time.Duration


@Suppress("unused")
class AndroidAppiumDriver : Driver {
    private val driver: AndroidDriver

    init {
        driver = AndroidDriver(URL(AppiumDriverConfig.remoteAddress), AppiumDriverConfig.desiredCapabilities)
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
    }

    override fun click(locator: Locator, points: ArrayList<Point>?) {
        TODO("Not yet implemented")
    }

    override fun checkLoadPage(url: String, identifier: Locator?): Boolean {
        TODO("Not yet implemented")
    }

    override fun switchToWindow(url: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun closeWindow(url: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCurrentUrl(): String {
        TODO("Not yet implemented")
    }

    override fun getElementValue(locator: Locator): String {
        TODO("Not yet implemented")
    }

    override fun getScreenshot(longScreenshot: Boolean, ignoredElements: Set<Locator>, screenshotAreas: List<Locator>): Screenshot {
        TODO("Not yet implemented")
    }

    override fun setPage(url: String) {
        TODO("Not yet implemented")
    }

    override fun setValue(locator: Locator, value: String, sequenceMode: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setSelectValue(locator: Locator, value: String) {
        TODO("Not yet implemented")
    }

    override fun uploadFile(locator: Locator, file: String) {
        TODO("Not yet implemented")
    }

    override fun isExist(locator: Locator): Boolean {
        TODO("Not yet implemented")
    }

    override fun isNotExist(locator: Locator): Boolean {
        TODO("Not yet implemented")
    }

    override fun executeJavaScript(script: String, vararg args: Any?): Any? {
        TODO("Not yet implemented")
    }


    override fun quit() {
        driver.quit()
    }
}