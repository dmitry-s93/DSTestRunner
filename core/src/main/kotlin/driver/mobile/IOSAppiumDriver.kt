package driver.mobile

import config.AppiumDriverConfig
import config.PreloaderConfig
import config.ScreenshotConfig
import driver.Driver
import driver.DriverHelper
import driver.mobile.device.Device
import driver.mobile.device.DeviceFactory
import io.appium.java_client.AppiumBy
import io.appium.java_client.ios.IOSDriver
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
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
class IOSAppiumDriver : Driver {
    private lateinit var driver: IOSDriver
    private var device: Device? = null
    private val viewportRect: Coords
    private val screenScale: Int
    private val pageLoadTimeout: Long = AppiumDriverConfig.pageLoadTimeout
    private val elementTimeout: Long = AppiumDriverConfig.elementTimeout
    private val poolDelay: Long = 50
    private val preloaderElements: List<Locator> = PreloaderConfig.elements
    private val numberOfAttempts = 3


    init {
        device = DeviceFactory.importDevice()
        startSession(retry = true)
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
        viewportRect = getViewportRect()
        screenScale = getScreenScale()
    }

    private fun startSession(retry: Boolean) {
        try {
            val remoteAddress = URL(device!!.remoteAddress)
            val capabilities = device!!.capabilities
            driver = IOSDriver(remoteAddress, capabilities)
        } catch (e: Exception) {
            if (retry)
                return startSession(retry = false)
            DeviceFactory.addDeviceToBlocklist(device!!)
            throw e
        }
    }

    private fun getViewportRect(): Coords {
        val viewportRect = driver.executeScript("mobile: viewportRect") as Map<*, *>

        val left = viewportRect["left"] as Long
        val top = viewportRect["top"] as Long
        val width = viewportRect["width"] as Long
        val height = viewportRect["height"] as Long

        return Coords(left.toInt(), top.toInt(), width.toInt(), height.toInt())
    }

    private fun getScreenScale(): Int {
        val deviceScreenInfo = driver.executeScript("mobile: deviceScreenInfo") as Map<*, *>
        val scale = deviceScreenInfo["scale"] as Long
        return scale.toInt()
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
                throw NotImplementedError("Not yet implemented")
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
                    !isPreloaderDisplayed() && (identifier == null || getWebElements(identifier, onlyDisplayed = false).isNotEmpty())
                }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    private fun isPreloaderDisplayed(): Boolean {
        preloaderElements.forEach { locator ->
            if (getWebElements(locator, onlyDisplayed = true).isNotEmpty())
                return true
        }
        return false
    }

    override fun getElementValue(locator: Locator): String {
        var value = ""
        DriverHelper().handleStaleElementReferenceException("getElementValue", numberOfAttempts) {
            value = getWebElement(locator).text
        }
        return value
    }

    override fun getScreenshot(longScreenshot: Boolean, ignoredElements: Set<Locator>, screenshotAreas: Set<Locator>): Screenshot {
        hideKeyboard()
        val waitTime = ScreenshotConfig.waitTimeBeforeScreenshot
        if (waitTime > 0)
            Thread.sleep(waitTime)
        // TODO: Add support for long screenshots
        return getSingleScreenshot(ignoredElements, screenshotAreas)
    }

    private fun getSingleScreenshot(ignoredElements: Set<Locator>, screenshotAreas: Set<Locator>): Screenshot {
        val screenshot: Screenshot
        if (screenshotAreas.isNotEmpty()) {
            val webElements: MutableList<WebElement> = mutableListOf()
            screenshotAreas.forEach { locator ->
                webElements.addAll(getWebElements(locator, onlyDisplayed = true))
            }
            screenshot = with(AShot()) {
                shootingStrategy(ShootingStrategies.simple())
                imageCropper(IndentCropper().addIndentFilter(IndentFilerFactory.blur()))
                takeScreenshot(driver, webElements)
            }
        } else {
            screenshot = Screenshot(takeScreenshot(viewportRect))
            screenshot.originShift = viewportRect
        }
        val ignoredAreas = getIgnoredAreas(ignoredElements, screenshot.originShift)
        screenshot.ignoredAreas = Coords.intersection(screenshot.coordsToCompare, ignoredAreas)
        return screenshot
    }

    private fun takeScreenshot(area: Coords): BufferedImage {
        val inputStream = ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES))
        return cropImage(ImageIO.read(inputStream), area)
    }

    private fun cropImage(image: BufferedImage, coords: Coords): BufferedImage {
        val x = coords.x
        val y = coords.y
        var width = coords.width
        var height = coords.height

        if (x + width > image.width)
            width = image.width
        if (y + height > image.height)
            height = image.height

        return image.getSubimage(x, y, width, height)
    }

    private fun getIgnoredAreas(locators: Set<Locator>, originShift: Coords, yOffset: Int = 0): Set<Coords> {
        val ignoredAreas: HashSet<Coords> = HashSet()
        locators.forEach { locator ->
            val webElements = getWebElements(locator, onlyDisplayed = true)
            webElements.forEach { webElement ->
                val elementLocation = webElement.location
                val elementSize = webElement.size

                if ((elementLocation.y * screenScale) + (elementSize.height * screenScale) >= originShift.y) {
                    val x = elementLocation.x * screenScale - originShift.x
                    val y = elementLocation.y * screenScale - originShift.y + yOffset
                    val width = elementSize.width * screenScale
                    val height = elementSize.height * screenScale
                    ignoredAreas.add(Coords(x, y, width, height))
                }
            }
        }
        return ignoredAreas
    }

    override fun setValue(locator: Locator, value: String, sequenceMode: Boolean) {
        DriverHelper().handleStaleElementReferenceException("setValue", numberOfAttempts) {
            val webElement = getWebElement(locator)
            webElement.clear()
            webElement.sendKeys(value)
            hideKeyboard()
        }
    }

    override fun hideKeyboard() {
        if (driver.isKeyboardShown) {
            getWebElement(
                Locator("type='XCUIElementTypeButton' AND name='keyboard hide'", LocatorType.IOS_PREDICATE_STRING)
            ).click()
        }
    }

    override fun setSelectValue(locator: Locator, value: String) {
        TODO("Not yet implemented")
    }

    override fun isExist(locator: Locator): Boolean {
        TODO("Not yet implemented")
    }

    override fun isNotExist(locator: Locator): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEnabled(locator: Locator): Boolean {
        TODO("Not yet implemented")
    }

    override fun navigateBack() {
        TODO("Not yet implemented")
    }

    override fun quit() {
        try {
            driver.quit()
        } catch (e: Exception) {
            throw e
        } finally {
            if (device != null)
                DeviceFactory.returnDevice(device!!)
        }
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
                    val elements = getWebElements(locator, onlyDisplayed = false)
                    if (elements.isNotEmpty()) {
                        element = elements[0]
                        return@until true
                    }
                    return@until false
                }
        } catch (_: ConditionTimeoutException) {
            return driver.findElement(byDetect(locator))
        }
        return element as WebElement
    }

    private fun getWebElements(locator: Locator, onlyDisplayed: Boolean): List<WebElement> {
        val elements = driver.findElements(byDetect(locator))
        if (onlyDisplayed) {
            val displayedElements: MutableList<WebElement> = mutableListOf()
            elements.forEach { element ->
                if (element.isDisplayed)
                    displayedElements.add(element)
            }
            return displayedElements
        }
        return elements
    }

    private fun byDetect(locator: Locator): By {
        return when(locator.type) {
            LocatorType.XPATH -> By.xpath(locator.value)
            LocatorType.CLASS_NAME -> By.ByClassName(locator.value)
            LocatorType.ID -> By.id(locator.value)
            LocatorType.ACCESSIBILITY_ID -> AppiumBy.ByAccessibilityId(locator.value)
            LocatorType.IOS_CLASS_CHAIN -> AppiumBy.iOSClassChain(locator.value)
            LocatorType.IOS_PREDICATE_STRING -> AppiumBy.iOSNsPredicateString(locator.value)
            null -> By.xpath(locator.value)
            else -> throw UnsupportedOperationException("Locator type not supported: ${locator.type.value}")
        }
    }
}