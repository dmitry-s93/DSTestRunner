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

package driver.web

import config.BrowserOptionsConfig
import config.PreloaderConfig
import config.ScreenshotConfig
import config.WebDriverConfig
import driver.Driver
import driver.DriverHelper
import logger.Logger
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionTimeoutException
import org.openqa.selenium.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.safari.SafariOptions
import org.openqa.selenium.support.ui.Select
import pazone.ashot.AShot
import pazone.ashot.Screenshot
import pazone.ashot.ShootingStrategies
import pazone.ashot.coordinates.Coords
import pazone.ashot.cropper.indent.IndentCropper
import pazone.ashot.cropper.indent.IndentFilerFactory.blur
import test.element.Locator
import test.element.LocatorType
import java.awt.Point
import java.net.URL
import java.time.Duration


@Suppress("unused")
class WebDriver : Driver {
    private val driver: WebDriver
    private val pageLoadTimeout: Long = WebDriverConfig.pageLoadTimeout
    private val elementTimeout: Long = WebDriverConfig.elementTimeout
    private val poolDelay: Long = 50
    private val preloaderElements: List<Locator> = PreloaderConfig.elements
    private val numberOfAttempts = 3
    private val browserType = WebDriverConfig.browserType

    init {
        when(browserType) {
            BrowserType.CHROME -> {
                val chromeOptions = ChromeOptions()
                chromeOptions.addArguments(BrowserOptionsConfig.arguments)
                driver = RemoteWebDriver(URL(WebDriverConfig.remoteAddress), chromeOptions)
            }
            BrowserType.FIREFOX -> {
                val firefoxOptions = FirefoxOptions()
                firefoxOptions.addArguments(BrowserOptionsConfig.arguments)
                driver = RemoteWebDriver(URL(WebDriverConfig.remoteAddress), firefoxOptions)
            }
            BrowserType.EDGE -> {
                val edgeOptions = EdgeOptions()
                edgeOptions.addArguments(BrowserOptionsConfig.arguments)
                driver = RemoteWebDriver(URL(WebDriverConfig.remoteAddress), edgeOptions)
            }
            BrowserType.SAFARI -> {
                val safariOptions = SafariOptions()
                driver = RemoteWebDriver(URL(WebDriverConfig.remoteAddress), safariOptions)
            }
        }
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(pageLoadTimeout))
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
    }

    override fun getElementTimeout(): Long {
        return elementTimeout
    }

    override fun click(locator: Locator, points: ArrayList<Pair<Point, Point?>>?) {
        DriverHelper().handleStaleElementReferenceException("click", numberOfAttempts) {
            val element = getWebElement(locator)
            if (points.isNullOrEmpty()) {
                element.click()
            } else {
                val center = DriverHelper().getElementCenter(element)
                val action = Actions(driver)
                points.forEach {
                    val point1 = it.first
                    val point2 = it.second
                    if (point2 != null) {
                        action.moveToLocation(center.x + point1.x, center.y + point1.y)
                        action.clickAndHold()
                        action.moveToLocation(center.x + point2.x, center.y + point2.y)
                        action.release()
                    } else {
                        action.moveToLocation(center.x + point1.x, center.y + point1.y)
                        action.click()
                    }
                }
                action.perform()
            }
            hideCursor()
        }
    }

    override fun checkLoadPage(url: String?, identifier: Locator?): Boolean {
        return try {
            await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(pageLoadTimeout))
                .until {
                    (url == null || getCurrentUrl().startsWith(url)) && (identifier == null || getWebElements(identifier, false).isNotEmpty()) && !isPreloaderDisplayed()
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
        val initialWindowHandle = driver.windowHandle
        for (windowHandle in driver.windowHandles) {
            driver.switchTo().window(windowHandle)
            if (url.isNullOrEmpty() && windowHandle != initialWindowHandle)
                return true
            if (url != null && getCurrentUrl().startsWith(url))
                return true
        }
        setWindowHandleOrFirst(initialWindowHandle)
        return false
    }

    override fun closeWindow(url: String?): Boolean {
        if (url.isNullOrEmpty()) {
            driver.close()
            setWindowHandleOrFirst()
            return true
        } else {
            val initialWindowHandle = driver.windowHandle
            for (windowHandle in driver.windowHandles) {
                driver.switchTo().window(windowHandle)
                if (getCurrentUrl().startsWith(url)) {
                    driver.close()
                    setWindowHandleOrFirst(initialWindowHandle)
                    return true
                }
            }
        }
        return false
    }

    private fun setWindowHandleOrFirst(windowHandle: String? = null) {
        val windowHandles = driver.windowHandles
        if (windowHandles.isEmpty())
            return
        if (windowHandle != null && windowHandles.contains(windowHandle))
            driver.switchTo().window(windowHandle)
        else
            driver.switchTo().window(windowHandles.first())
    }

    override fun getCurrentUrl(): String {
        return driver.currentUrl
    }

    override fun getElementValue(locator: Locator): String {
        var value: String? = ""
        DriverHelper().handleStaleElementReferenceException("getElementValue", numberOfAttempts) {
            val element = getWebElement(locator)
            value = element.text
            if (value.isNullOrEmpty())
                value = element.getAttribute("value")
        }
        return value ?: ""
    }

    override fun getScreenshot(longScreenshot: Boolean, ignoredElements: Set<Locator>, screenshotAreas: Set<Locator>): Screenshot {
        ScreenshotConfig.executeJavaScriptBeforeScreenshot?.let { executeJavaScript(it) }
        scrollToTop()
        removeFocus()
        val waitTime = ScreenshotConfig.waitTimeBeforeScreenshot
        if (waitTime > 0)
            Thread.sleep(waitTime)
        val strategy =
            if (longScreenshot)
                ShootingStrategies.viewportPasting(100)
            else
                ShootingStrategies.simple()
        val screenshot = with(AShot()) {
            shootingStrategy(strategy)
            ignoredAreas(getIgnoredAreas(ignoredElements))
            if (screenshotAreas.isNotEmpty()) {
                val webElements: MutableList<WebElement> = mutableListOf()
                screenshotAreas.forEach { locator ->
                    webElements.addAll(getWebElements(locator, onlyDisplayed = true))
                }
                imageCropper(IndentCropper().addIndentFilter(blur()))
                takeScreenshot(driver, webElements)
            } else {
                takeScreenshot(driver)
            }
        }
        ScreenshotConfig.executeJavaScriptAfterScreenshot?.let { executeJavaScript(it) }
        return screenshot
    }

    private fun getIgnoredAreas(locators: Set<Locator>): Set<Coords> {
        val ignoredAreas: HashSet<Coords> = HashSet()
        locators.forEach { locator ->
            DriverHelper().handleStaleElementReferenceException("getIgnoredAreas", numberOfAttempts) {
                val webElements = getWebElements(locator, onlyDisplayed = true)
                webElements.forEach { webElement ->
                    val x = webElement.location.x
                    val y = webElement.location.y
                    val width = webElement.size.width
                    val height = webElement.size.height
                    ignoredAreas.add(Coords(x, y, width, height))
                }
            }
        }
        return ignoredAreas
    }

    private fun getWebElement(locator: Locator): WebElement {
        var element: WebElement? = null
        try {
            await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until {
                    val elements = getWebElements(locator, onlyDisplayed = true, scrollToCheckVisibility = true)
                    if (elements.isNotEmpty()) {
                        element = elements[0]
                        return@until true
                    }
                    return@until false
                }
        } catch (_: ConditionTimeoutException) {}
        if (element != null) {
            scrollToElement(element!!)
            return element as WebElement
        }
        return driver.findElement(byDetect(locator))
    }

    private fun getWebElements(locator: Locator, onlyDisplayed: Boolean, scrollToCheckVisibility: Boolean = false): List<WebElement> {
        val elements = driver.findElements(byDetect(locator))
        if (onlyDisplayed) {
            val displayedElements: MutableList<WebElement> = mutableListOf()
            elements.forEach { element ->
                if (scrollToCheckVisibility)
                    scrollToElement(element)
                if (element.isDisplayed)
                    displayedElements.add(element)
            }
            return displayedElements
        }
        return elements
    }

    private fun scrollToElement(element: WebElement) {
        executeJavaScript("arguments[0].scrollIntoView({block: 'center'});", element)
    }

    private fun scrollToTop() {
        executeJavaScript("window.scrollTo(0, 0);")
    }

    private fun removeFocus() {
        executeJavaScript("document.activeElement.blur();")
    }

    private fun hideCursor() {
        with (Actions(driver)) {
            moveToLocation(0, 0)
            perform()
        }
    }

    private fun byDetect(locator: Locator): By {
        return when(locator.type) {
            LocatorType.XPATH -> By.xpath(locator.value)
            LocatorType.CSS_SELECTOR -> By.cssSelector(locator.value)
            LocatorType.CLASS_NAME -> By.ByClassName(locator.value)
            LocatorType.ID -> By.id(locator.value)
            null -> By.xpath(locator.value)
            else -> throw UnsupportedOperationException("Locator type not supported: ${locator.type.value}")
        }
    }

    override fun setPage(url: String) {
        try {
            driver.get(url)
        } catch (e: InvalidArgumentException) {
            Logger.error("Invalid argument specified: $url", "setPage")
            throw e
        }
    }

    override fun setValue(locator: Locator, value: String, sequenceMode: Boolean, hideKeyboard: Boolean) {
        DriverHelper().handleStaleElementReferenceException("setValue", numberOfAttempts) {
            val webElement = getWebElement(locator)
            if (browserType == BrowserType.SAFARI) {
                webElement.clear()
            } else {
                webElement.sendKeys(Keys.chord(Keys.CONTROL, "a") + Keys.DELETE)
            }
            if (sequenceMode) {
                value.forEach {
                    webElement.sendKeys(it.toString())
                    Thread.sleep(50)
                }
            } else {
                webElement.sendKeys(value)
            }
        }
    }

    override fun setSelectValue(locator: Locator, value: String) {
        val select = Select(getWebElement(locator))
        select.selectByVisibleText(value)
    }

    override fun uploadFile(locator: Locator, file: String) {
        val webElement = getWebElements(locator, onlyDisplayed = false)[0]
        webElement.sendKeys(file)
    }

    override fun isExist(locator: Locator): Boolean {
        return try {
            await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, onlyDisplayed = true, scrollToCheckVisibility = true).isNotEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    override fun isNotExist(locator: Locator): Boolean {
        return try {
            await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, onlyDisplayed = true, scrollToCheckVisibility = true).isEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    override fun isEnabled(locator: Locator): Boolean {
        return getWebElement(locator).isEnabled
    }

    override fun executeJavaScript(script: String, vararg args: Any?): Any? {
        val js = driver as JavascriptExecutor
        return js.executeScript(script, *args)
    }

    override fun hoverOverElement(locator: Locator) {
        val action = Actions(driver)
        val element = getWebElement(locator)
        action.moveToElement(element).perform()
    }

    override fun navigateBack() {
        driver.navigate().back()
    }

    override fun quit() {
        driver.quit()
    }
}