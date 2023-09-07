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
import config.PreloaderConfig
import config.ScreenshotConfig
import driver.Driver
import io.appium.java_client.AppiumBy.ByAccessibilityId
import io.appium.java_client.AppiumBy.ByAndroidUIAutomator
import io.appium.java_client.android.AndroidDriver
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import pazone.ashot.AShot
import pazone.ashot.Screenshot
import pazone.ashot.ShootingStrategies
import pazone.ashot.coordinates.Coords
import pazone.ashot.cropper.indent.IndentCropper
import pazone.ashot.cropper.indent.IndentFilerFactory
import test.element.Locator
import test.element.LocatorType
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URL
import java.time.Duration
import javax.imageio.ImageIO


@Suppress("unused")
class AndroidAppiumDriver : Driver {
    private val driver: AndroidDriver
    private val pageLoadTimeout: Long = AppiumDriverConfig.pageLoadTimeout
    private val elementTimeout: Long = AppiumDriverConfig.elementTimeout
    private val poolDelay: Long = 50
    private val preloaderElements: List<Locator> = PreloaderConfig.elements

    init {
        driver = AndroidDriver(URL(AppiumDriverConfig.remoteAddress), AppiumDriverConfig.desiredCapabilities)
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
    }

    override fun click(locator: Locator, points: ArrayList<Point>?) {
        val element = getWebElement(locator)
        if (points.isNullOrEmpty()) {
            element.click()
        } else {
            with (Actions(driver)) {
                points.forEach {
                    moveToElement(element)
                    moveByOffset(it.x, it.y)
                    click()
                }
                perform()
            }
        }
    }

    override fun checkLoadPage(url: String?, identifier: Locator?): Boolean {
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(pageLoadTimeout))
                .until {
                    (identifier == null || getWebElements(identifier, false).isNotEmpty()) && !isPreloaderDisplayed()
                }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    private fun isPreloaderDisplayed(): Boolean {
        preloaderElements.forEach { locator ->
            if (getWebElements(locator, true).isNotEmpty())
                return true
        }
        return false
    }

    override fun switchToWindow(url: String?): Boolean {
        throw UnsupportedOperationException("Operation not supported")
    }

    override fun closeWindow(url: String?): Boolean {
        throw UnsupportedOperationException("Operation not supported")
    }

    override fun getCurrentUrl(): String {
        throw UnsupportedOperationException("Operation not supported")
    }

    override fun getElementValue(locator: Locator): String {
        return getWebElement(locator).text
    }

    override fun getScreenshot(longScreenshot: Boolean, ignoredElements: Set<Locator>, screenshotAreas: Set<Locator>): Screenshot {
        val waitTime = ScreenshotConfig.waitTimeBeforeScreenshot
        if (waitTime > 0)
            Thread.sleep(waitTime)
        val screenshot: Screenshot
        if (screenshotAreas.isNotEmpty()) {
            val webElements: MutableList<WebElement> = mutableListOf()
            screenshotAreas.forEach { locator ->
                webElements.add(getWebElement(locator))
            }
            screenshot = with(AShot()) {
                shootingStrategy(ShootingStrategies.simple())
                imageCropper(IndentCropper().addIndentFilter(IndentFilerFactory.blur()))
                takeScreenshot(driver, webElements)
            }
        } else {
            val appArea = getAppArea()
            screenshot = Screenshot(cropImage(takeScreenshot(), appArea))
            screenshot.originShift = appArea
        }
        screenshot.ignoredAreas = getIgnoredAreas(ignoredElements, screenshot.originShift)
        return screenshot
    }

    private fun takeScreenshot(): BufferedImage {
        val inputStream = ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES))
        return ImageIO.read(inputStream)
    }

    private fun cropImage(image: BufferedImage, coords: Coords): BufferedImage {
        return image.getSubimage(coords.x, coords.y, coords.width, coords.height)
    }

    private fun getAppArea(): Coords {
        val statusBar = driver.systemBars["statusBar"]
        val statusBarVisible = statusBar?.get("visible") as Boolean
        val windowSize = driver.manage().window().size
        val windowPosition = driver.manage().window().position

        val x = windowPosition.x
        var y = windowPosition.y
        val width = windowSize.width
        var height = windowSize.height

        if (statusBarVisible) {
            val statusBarHeight = (statusBar["height"] as Long).toInt()
            if (y < statusBarHeight) {
                y += statusBarHeight
                height -= y
            }
        }

        return Coords(x, y, width, height)
    }

    private fun getIgnoredAreas(locators: Set<Locator>, originShift: Coords): Set<Coords> {
        val ignoredAreas: HashSet<Coords> = HashSet()
        locators.forEach { locator ->
            val webElements = getWebElements(locator, onlyDisplayed = true)
            webElements.forEach { webElement ->
                val x = webElement.location.x - originShift.x
                val y = webElement.location.y - originShift.y
                val width = webElement.size.width
                val height = webElement.size.height
                ignoredAreas.add(Coords(x, y, width, height))
            }
        }
        return ignoredAreas
    }

    private fun getWebElement(locator: Locator): WebElement {
        var element: WebElement? = null
        try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until {
                    val elements = getWebElements(locator, true)
                    if (elements.isNotEmpty()) {
                        element = elements[0]
                        return@until true
                    }
                    return@until false
                }
        } catch (_: ConditionTimeoutException) {}
        if (element != null)
            return element as WebElement
        return driver.findElement(byDetect(locator))
    }

    private fun getWebElements(locator: Locator, onlyDisplayed: Boolean): List<WebElement> {
        val elements = driver.findElements(byDetect(locator))
        if (onlyDisplayed)
            return elements.filter { it.isDisplayed }
        return elements
    }

    private fun byDetect(locator: Locator): By {
        return when(locator.type) {
            LocatorType.XPATH -> By.xpath(locator.value)
            LocatorType.CSS_SELECTOR -> By.cssSelector(locator.value)
            LocatorType.CLASS_NAME -> By.ByClassName(locator.value)
            LocatorType.ID -> By.id(locator.value)
            LocatorType.ACCESSIBILITY_ID -> ByAccessibilityId(locator.value)
            LocatorType.ANDROID_UI_AUTOMATOR -> ByAndroidUIAutomator(locator.value)
            null -> By.xpath(locator.value)
        }
    }

    override fun setPage(url: String) {
        throw UnsupportedOperationException("Operation not supported")
    }

    override fun setValue(locator: Locator, value: String, sequenceMode: Boolean) {
        val webElement = getWebElement(locator)
        webElement.clear()
        if (sequenceMode) {
            webElement.click()
            value.forEach {
                Actions(driver).sendKeys(it.toString()).perform()
                Thread.sleep(50)
            }
            driver.hideKeyboard()
            return
        }
        webElement.sendKeys(value)
        driver.hideKeyboard()
    }

    override fun setSelectValue(locator: Locator, value: String) {
        TODO("Not yet implemented")
    }

    override fun uploadFile(locator: Locator, file: String) {
        throw UnsupportedOperationException("Operation not supported")
    }

    override fun isExist(locator: Locator): Boolean {
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, true).isNotEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    override fun isNotExist(locator: Locator): Boolean {
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, true).isEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    override fun executeJavaScript(script: String, vararg args: Any?): Any? {
        throw UnsupportedOperationException("Operation not supported")
    }

    override fun quit() {
        driver.quit()
    }
}