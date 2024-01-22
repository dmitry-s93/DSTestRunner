package driver.mobile

import action.helper.Direction
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
import org.openqa.selenium.interactions.PointerInput
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
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("unused")
class IOSAppiumDriver : Driver {
    private lateinit var driver: IOSDriver
    private var device: Device? = null
    private val viewportArea: Coords
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
        viewportArea = getViewportRect()
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
                shootingStrategy(ShootingStrategies.scaling(screenScale.toFloat())) // TODO: Do not use scaling
                imageCropper(IndentCropper().addIndentFilter(IndentFilerFactory.blur()))
                takeScreenshot(driver, webElements)
            }
        } else {
            screenshot = Screenshot(takeScreenshot(viewportArea))
            screenshot.originShift = viewportArea
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

    override fun setValue(locator: Locator, value: String, sequenceMode: Boolean, hideKeyboard: Boolean) {
        DriverHelper().handleStaleElementReferenceException("setValue", numberOfAttempts) {
            val webElement = getWebElement(locator)
            webElement.clear()
            webElement.sendKeys(value)
            if (hideKeyboard)
                hideKeyboard()
        }
    }

    override fun hideKeyboard() {
        if (driver.isKeyboardShown) {
            val locator = Locator("type='XCUIElementTypeButton' AND name='keyboard hide'", LocatorType.IOS_PREDICATE_STRING)
            if (getWebElements(locator, onlyDisplayed = true).isEmpty())
                return
            getWebElement(locator).click()
        }
    }

    override fun setSelectValue(locator: Locator, value: String) {
        TODO("Not yet implemented")
    }

    override fun isExist(locator: Locator): Boolean {
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, onlyDisplayed = false).isNotEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            getWebElements(locator, onlyDisplayed = false).isNotEmpty()
        }
    }

    override fun isNotExist(locator: Locator): Boolean {
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, onlyDisplayed = false).isEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            getWebElements(locator, onlyDisplayed = false).isEmpty()
        }
    }

    override fun isEnabled(locator: Locator): Boolean {
        return getWebElement(locator).isEnabled
    }

    override fun navigateBack() {
        TODO("Not yet implemented")
    }

    override fun swipeElement(locator: Locator, direction: Direction) {
        val elementCenter = DriverHelper().getElementCenter(getWebElement(locator))
        val endX: Int
        val endY: Int
        when (direction) {
            Direction.UP -> {
                endX = elementCenter.x
                endY = viewportArea.y
            }
            Direction.DOWN -> {
                endX = elementCenter.x
                endY = viewportArea.y + viewportArea.height
            }
            Direction.LEFT -> {
                endX = viewportArea.x
                endY = elementCenter.y
            }
            Direction.RIGHT -> {
                endX = viewportArea.x + viewportArea.width
                endY = elementCenter.y
            }
        }
        swipe(elementCenter.x, elementCenter.y, endX, endY)
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

    private fun swipe(startX: Int, startY: Int, endX: Int, endY: Int) {
        val duration = countSwipeDuration(
            startX * screenScale, startY * screenScale,
            endX * screenScale, endY * screenScale
        )
        println(duration)
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val swipe = org.openqa.selenium.interactions.Sequence(finger, 1)
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
        swipe.addAction(finger.createPointerDown(0))
        swipe.addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), endX - 1, endY))
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(100), PointerInput.Origin.viewport(), endX + 1, endY))
        swipe.addAction(finger.createPointerUp(0))
        driver.perform(listOf(swipe))
    }

    private fun countSwipeDuration(x1: Int, y1: Int, x2: Int, y2: Int): Duration {
        val fullAreaSwipeDuration = 1.2 // in seconds
        val distanceInPx = sqrt((x2 - x1).toDouble().pow(2) + (y2 - y1).toDouble().pow(2))
        val duration = ceil(fullAreaSwipeDuration / viewportArea.height * distanceInPx * 10) * 100
        return Duration.ofMillis(duration.toLong())
    }
}