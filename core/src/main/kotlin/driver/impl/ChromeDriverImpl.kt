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

package driver.impl

import config.BrowserOptionsConfig
import config.WebDriverConfig
import driver.Driver
import logger.Logger
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionTimeoutException
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.net.URL
import java.time.Duration


@Suppress("unused")
class ChromeDriverImpl : Driver {
    private val driver: WebDriver
    private val pageLoadTimeout: Long = WebDriverConfig.getPageLoadTimeout()
    private val implicitlyWait: Long = WebDriverConfig.getImplicitlyWait()

    init {
        val chromeOptions = ChromeOptions()
        chromeOptions.addArguments(BrowserOptionsConfig.getArguments())
        driver = RemoteWebDriver(URL(WebDriverConfig.getRemoteAddress()), chromeOptions)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(pageLoadTimeout))
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(implicitlyWait))
    }

    override fun click(locator: String) {
        getWebElement(locator).click()
    }

    override fun checkLoadPage(url: String, identifier: String?): Boolean {
        setImplicitlyWait(1)
        return try {
            await()
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(200))
                .atMost(Duration.ofMillis(pageLoadTimeout))
                .until {
                    getCurrentUrl().startsWith(url) && (identifier.isNullOrEmpty() || getWebElements(identifier).isNotEmpty())
                }
            true
        } catch (e: ConditionTimeoutException) {
            false
        } finally {
            setImplicitlyWait(implicitlyWait)
        }
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
        val webElement = driver.findElement(By.xpath(locator))
        await()
            .atLeast(Duration.ofMillis(0))
            .pollDelay(Duration.ofMillis(200))
            .atMost(Duration.ofMillis(implicitlyWait))
            .until { webElement.isDisplayed }
        return webElement
    }

    private fun getWebElements(locator: String): MutableList<WebElement> {
        return driver.findElements(By.xpath(locator))
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

    override fun isExist(locator: String): Boolean {
        setImplicitlyWait(1)
        return try {
            await()
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(200))
                .atMost(Duration.ofMillis(implicitlyWait))
                .until { getWebElements(locator).isNotEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        } finally {
            setImplicitlyWait(implicitlyWait)
        }
    }

    override fun isNotExist(locator: String): Boolean {
        setImplicitlyWait(1)
        return try {
            await()
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(200))
                .atMost(Duration.ofMillis(implicitlyWait))
                .until { getWebElements(locator).isEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        } finally {
            setImplicitlyWait(implicitlyWait)
        }
    }

    override fun quit() {
        driver.quit()
    }

    private fun setImplicitlyWait(implicitlyWait: Long) {
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(implicitlyWait))
    }
}