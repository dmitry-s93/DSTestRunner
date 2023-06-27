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
import config.WebDriverConfig
import driver.Driver
import logger.Logger
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionTimeoutException
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.Select
import java.awt.Point
import java.net.URL
import java.time.Duration


@Suppress("unused")
class ChromeDriver : Driver {
    private val driver: WebDriver
    private val pageLoadTimeout: Long = WebDriverConfig.getPageLoadTimeout()
    private val elementTimeout: Long = WebDriverConfig.getElementTimeout()
    private val poolDelay: Long = 50
    private val preloaderElements: List<String> = PreloaderConfig.getElements()

    init {
        val chromeOptions = ChromeOptions()
        chromeOptions.addArguments(BrowserOptionsConfig.getArguments())
        driver = RemoteWebDriver(URL(WebDriverConfig.getRemoteAddress()), chromeOptions)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(pageLoadTimeout))
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
    }

    override fun click(locator: String, points: ArrayList<Point>?) {
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

    override fun checkLoadPage(url: String, identifier: String?): Boolean {
        return try {
            await()
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(pageLoadTimeout))
                .until {
                    getCurrentUrl().startsWith(url) && (identifier.isNullOrEmpty() || getWebElements(identifier, false).isNotEmpty()) && !isPreloaderDisplayed()
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

    override fun getElementValue(locator: String): String {
        return getWebElement(locator).text
    }

    override fun getScreenshot(): ByteArray {
        return (driver as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
    }

    private fun getWebElement(locator: String): WebElement {
        var element: WebElement? = null
        await()
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
        if (element != null)
            return element as WebElement
        return driver.findElement(By.xpath(locator))
    }

    private fun getWebElements(locator: String, onlyDisplayed: Boolean): List<WebElement> {
        val elements = driver.findElements(By.xpath(locator))
        if (onlyDisplayed)
            return elements.filter { it.isDisplayed }
        return elements
    }

    override fun setPage(url: String) {
        try {
            driver.get(url)
        } catch (e: InvalidArgumentException) {
            Logger.error("Invalid argument specified: $url", "setPage")
            throw e
        }
    }

    override fun setValue(locator: String, value: String, sequenceMode: Boolean) {
        val webElement = getWebElement(locator)
        webElement.clear()
        if (sequenceMode) {
            value.forEach {
                webElement.sendKeys(it.toString())
                Thread.sleep(50)
            }
            return
        }
        webElement.sendKeys(value)
    }

    override fun setSelectValue(locator: String, value: String) {
        val select = Select(getWebElement(locator))
        select.selectByVisibleText(value)
    }

    override fun isExist(locator: String): Boolean {
        return try {
            await()
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, true).isNotEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    override fun isNotExist(locator: String): Boolean {
        return try {
            await()
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, true).isEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    override fun quit() {
        driver.quit()
    }
}