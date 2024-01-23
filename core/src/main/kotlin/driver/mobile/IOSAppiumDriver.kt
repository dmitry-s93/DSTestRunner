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
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import pazone.ashot.AShot
import pazone.ashot.Screenshot
import pazone.ashot.ShootingStrategies
import pazone.ashot.coordinates.Coords
import pazone.ashot.cropper.indent.IndentCropper
import pazone.ashot.cropper.indent.IndentFilerFactory
import test.element.Locator
import test.element.LocatorType
import utils.ImageUtils
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.net.URL
import java.time.Duration
import java.util.*
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.collections.HashMap
import kotlin.collections.HashSet
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
    private val maxSwipeCount = 20


    init {
        device = DeviceFactory.importDevice()
        startSession(retry = true)
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
        viewportArea = getViewportRect()
        screenScale = getScreenScale()
        getLongScreenshot(HashSet(), HashSet())
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
        if (longScreenshot && screenshotAreas.isEmpty())
            return getLongScreenshot(ignoredElements, screenshotAreas)
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

    private fun getLongScreenshot(ignoredElements: Set<Locator>, screenshotAreas: Set<Locator>): Screenshot {
        val pauseAtExtremePoints: Long = 250
        val scrollableArea = getScrollableArea(screenScale) ?: return getSingleScreenshot(ignoredElements, screenshotAreas)
        scrollToTop()
        Thread.sleep(pauseAtExtremePoints)

        val maxImageHeight = viewportArea.height * 4
        val bufferedImageList: LinkedList<BufferedImage> = LinkedList()
        val ignoredAreas: MutableSet<Coords> = HashSet()

        var imageHeight = scrollableArea.y + scrollableArea.height - viewportArea.y
        var originShift = Coords(viewportArea.x, viewportArea.y, viewportArea.width,  imageHeight)
        bufferedImageList.add(takeScreenshot(originShift))
        ignoredAreas.addAll(getIgnoredAreas(ignoredElements, originShift))

        do {
            val elementPositionsBefore = getElementPositions()
            if (!scroll(Direction.UP))
                break
            val scrollSize = getScrollSize(elementPositionsBefore, getElementPositions()) * screenScale
            if (scrollSize > 0) {
                val y = scrollableArea.y + scrollableArea.height - scrollSize
                originShift = Coords(viewportArea.x, y, viewportArea.width, scrollSize)
                bufferedImageList.add(takeScreenshot(originShift))
                ignoredAreas.addAll(getIgnoredAreas(ignoredElements, originShift, imageHeight))
                imageHeight += scrollSize
            }
        } while (scrollSize > 0 && imageHeight < maxImageHeight)

        if (imageHeight < maxImageHeight) {
            Thread.sleep(pauseAtExtremePoints)
            val y = scrollableArea.y + scrollableArea.height
            val height = viewportArea.y + viewportArea.height - y
            originShift = Coords(viewportArea.x, y, viewportArea.width, height)
            bufferedImageList.add(takeScreenshot(originShift))
            ignoredAreas.addAll(getIgnoredAreas(ignoredElements, originShift, imageHeight))
        }

        var bufferedImage = ImageUtils().concatImageList(bufferedImageList)
        if (bufferedImage.height > maxImageHeight)
            bufferedImage = bufferedImage.getSubimage(0, 0, bufferedImage.width, maxImageHeight)

        val screenshot = Screenshot(bufferedImage)
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

    private fun scrollToTop() {
        var currSwipeCount = 0
        do {
            val elementPositionsBefore = getElementPositions()
            if (!scroll(Direction.DOWN))
                break
            currSwipeCount++
        } while (getScrollSize(elementPositionsBefore, getElementPositions()) != 0 && currSwipeCount < maxSwipeCount)
    }

    private fun scroll(direction: Direction): Boolean {
        val scrollableArea = getScrollableArea() ?: return false

        val centerX = (scrollableArea.width / 2) + scrollableArea.x
        val startY: Int
        val endY: Int

        when(direction) {
            Direction.UP -> {
                startY = scrollableArea.y + scrollableArea.height
                endY = scrollableArea.y
            }
            Direction.DOWN -> {
                startY = scrollableArea.y
                endY = scrollableArea.y + scrollableArea.height
            }
            else -> {
                return false
            }
        }

        swipe(centerX, startY, centerX, endY)
        return true
    }

    private fun getScrollSize(elementCoords1: Map<String, Coords>, elementCoords2: Map<String, Coords>): Int {
        val scrollSizes = HashMap<Int, Int>()

        elementCoords1.forEach {
            val coords1 = elementCoords1[it.key]
            val coords2 = elementCoords2[it.key]

            if (coords1 != null && coords2 != null && coords1.height == coords2.height) {
                val difference = coords1.y - coords2.y
                val count = scrollSizes[difference]
                if (count != null) {
                    scrollSizes[difference] = count.inc()
                } else {
                    scrollSizes[difference] = 1
                }
            }
        }

        var scrollSize = 0
        var maxCount = 0
        scrollSizes.forEach {
            if (it.key != 0 && it.value > maxCount) {
                scrollSize = it.key
                maxCount = it.value
            }
        }

        return scrollSize
    }

    private fun getElementPositions(): Map<String, Coords> {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val `is` = InputSource(StringReader(driver.pageSource))
        val document: Document = builder.parse(`is`)

        val xPathFactory = XPathFactory.newInstance()
        val xpath = xPathFactory.newXPath()
        val expr = xpath.compile("//*[string-length(@name) > 0]")
        val nodes = expr.evaluate(document, XPathConstants.NODESET) as NodeList

        val result: MutableMap<String, Coords> = HashMap()
        val duplicateKeys: MutableSet<String> = HashSet()

        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            val key = element.getAttribute("name")
            val x = element.getAttribute("x")
            val y = element.getAttribute("y")
            val width = element.getAttribute("width")
            val height = element.getAttribute("height")

            if (result.containsKey(key))
                duplicateKeys.add(key)
            if (x.isNotEmpty() && y.isNotEmpty() && width.isNotEmpty() && height.isNotEmpty())
                result[key] = Coords(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        }

        duplicateKeys.forEach {
            if (result.containsKey(it))
                result.remove(it)
        }

        return result
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

    private fun getScrollableArea(scale: Int = 1): Coords? {
        val scrollableElements = driver.findElements(
            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeScrollView' OR type == 'XCUIElementTypeTable'")
        )
        if (scrollableElements.isEmpty())
            return null
        var scrollable = scrollableElements[0]

        if (scrollableElements.size > 1) {
            for (i in 1..<scrollableElements.size) {
                if (scrollableElements[i].size.height > scrollable.size.height)
                    scrollable = scrollableElements[i]
            }
        }

        val location = scrollable.location
        val size = scrollable.size

        val x = location.x * scale
        val y = location.y + (size.height / 4) * scale
        val width = size.width * scale
        val height = size.height / 2 * scale

        return Coords(x, y, width, height)
    }
}