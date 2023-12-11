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

import action.helper.Direction
import config.AppiumDriverConfig
import config.PreloaderConfig
import config.ScreenshotConfig
import driver.Driver
import driver.DriverHelper
import driver.mobile.device.Device
import driver.mobile.device.DeviceFactory
import io.appium.java_client.AppiumBy.ByAccessibilityId
import io.appium.java_client.AppiumBy.ByAndroidUIAutomator
import io.appium.java_client.android.AndroidDriver
import logger.Logger
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.openqa.selenium.*
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
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


@Suppress("unused")
class AndroidAppiumDriver : Driver {
    private lateinit var driver: AndroidDriver
    private val pageLoadTimeout: Long = AppiumDriverConfig.pageLoadTimeout
    private val elementTimeout: Long = AppiumDriverConfig.elementTimeout
    private val poolDelay: Long = 50
    private val maxSwipeCount = 20
    private val preloaderElements: List<Locator> = PreloaderConfig.elements
    private val unsupportedOperationMessage = "Operation not supported"
    private val appArea: Coords
    private var device: Device? = null
    private val numberOfAttempts = 3

    init {
        device = DeviceFactory.importDevice()
        startSession(retry = true)
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
        appArea = calculateAppArea()
    }

    private fun startSession(retry: Boolean) {
        try {
            val remoteAddress = URL(device!!.remoteAddress)
            val capabilities = device!!.capabilities
            driver = AndroidDriver(remoteAddress, capabilities)
        } catch (e: Exception) {
            if (retry)
                return startSession(retry = false)
            DeviceFactory.addDeviceToBlocklist(device!!)
            throw e
        }
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
                points.forEach {
                    val point1 = it.first
                    val point2 = it.second
                    if (point2 != null) {
                        throw NotImplementedError("Not yet implemented")
                    } else {
                        tapOnPoint(
                            center.x + point1.x,
                            center.y + point1.y
                        )
                    }
                    Thread.sleep(300)
                }
            }
        }
    }

    override fun checkLoadPage(url: String?, identifier: Locator?): Boolean {
        var scrollToFind = false
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(pageLoadTimeout))
                .conditionEvaluationListener { condition ->
                    if (condition.elapsedTimeInMS > 1500)
                        scrollToFind = true
                }
                .until {
                    !isPreloaderDisplayed() && (identifier == null || getWebElements(identifier, scrollToFind).isNotEmpty())
                }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    private fun isPreloaderDisplayed(): Boolean {
        preloaderElements.forEach { locator ->
            if (getWebElements(locator, scrollToFind = false).isNotEmpty())
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
                webElements.addAll(getWebElements(locator, scrollToFind = false))
            }
            screenshot = with(AShot()) {
                shootingStrategy(ShootingStrategies.simple())
                imageCropper(IndentCropper().addIndentFilter(IndentFilerFactory.blur()))
                takeScreenshot(driver, webElements)
            }
        } else {
            screenshot = Screenshot(takeScreenshot(appArea))
            screenshot.originShift = appArea
        }
        val ignoredAreas = getIgnoredAreas(ignoredElements, screenshot.originShift)
        screenshot.ignoredAreas = Coords.intersection(screenshot.coordsToCompare, ignoredAreas)
        return screenshot
    }

    private fun getLongScreenshot(ignoredElements: Set<Locator>, screenshotAreas: Set<Locator>): Screenshot {
        val pauseAtExtremePoints: Long = 250
        val scrollableArea = getScrollableArea() ?: return getSingleScreenshot(ignoredElements, screenshotAreas)
        scrollToTop()
        Thread.sleep(pauseAtExtremePoints)

        val maxImageHeight = appArea.height * 4
        val bufferedImageList: LinkedList<BufferedImage> = LinkedList()
        val ignoredAreas: MutableSet<Coords> = HashSet()

        var imageHeight = scrollableArea.y + scrollableArea.height - appArea.y
        var originShift = Coords(appArea.x, appArea.y, appArea.width,  imageHeight)
        bufferedImageList.add(takeScreenshot(originShift))
        ignoredAreas.addAll(getIgnoredAreas(ignoredElements, originShift))

        do {
            val elementPositionsBefore = getElementPositions()
            if (!scroll(Direction.UP))
                break
            val scrollSize = getScrollSize(elementPositionsBefore, getElementPositions())
            if (scrollSize > 0) {
                val y = scrollableArea.y + scrollableArea.height - scrollSize
                originShift = Coords(appArea.x, y, appArea.width, scrollSize)
                bufferedImageList.add(takeScreenshot(originShift))
                ignoredAreas.addAll(getIgnoredAreas(ignoredElements, originShift, imageHeight))
                imageHeight += scrollSize
            }
        } while (scrollSize > 0 && imageHeight < maxImageHeight)

        if (imageHeight < maxImageHeight) {
            Thread.sleep(pauseAtExtremePoints)
            val y = scrollableArea.y + scrollableArea.height
            val height = appArea.y + appArea.height - y
            originShift = Coords(appArea.x, y, appArea.width, height)
            bufferedImageList.add(takeScreenshot(originShift))
            ignoredAreas.addAll(getIgnoredAreas(ignoredElements, originShift, imageHeight))
        }

        var bufferedImage = concatImageList(bufferedImageList)
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
        return image.getSubimage(coords.x, coords.y, coords.width, coords.height)
    }

    private fun concatImageList(imageList: LinkedList<BufferedImage>): BufferedImage {
        var currHeight = 0
        var totalHeight = 0
        imageList.forEach { totalHeight += it.height }
        val concatImage = BufferedImage(imageList.first().width, totalHeight, BufferedImage.TYPE_INT_RGB)
        val g2d = concatImage.createGraphics()
        imageList.forEach {
            g2d.drawImage(it, 0, currHeight, null)
            currHeight += it.height
        }
        g2d.dispose()
        return concatImage
    }

    private fun calculateAppArea(): Coords {
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

    private fun getIgnoredAreas(locators: Set<Locator>, originShift: Coords, yOffset: Int = 0): Set<Coords> {
        val ignoredAreas: HashSet<Coords> = HashSet()
        locators.forEach { locator ->
            val webElements = getWebElements(locator, scrollToFind = false)
            webElements.forEach { webElement ->
                val elementLocation = webElement.location
                val elementSize = webElement.size

                if (elementLocation.y + elementSize.height >= originShift.y) {
                    val x = elementLocation.x - originShift.x
                    val y = elementLocation.y - originShift.y + yOffset
                    val width = elementSize.width
                    val height = elementSize.height
                    ignoredAreas.add(Coords(x, y, width, height))
                }
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
                    val elements = getWebElements(locator, scrollToFind = true)
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

    private fun getWebElements(locator: Locator, scrollToFind: Boolean): List<WebElement> {
        var elements = driver.findElements(byDetect(locator))
        if (elements.isNotEmpty() || !scrollToFind)
            return elements
        var swipeCount = 0
        var direction = Direction.DOWN
        while (elements.isEmpty()) {
            if (swipeCount >= maxSwipeCount)
                break
            val elementPositionsBefore = getElementPositions()
            if (!scroll(direction))
                break
            if (getScrollSize(elementPositionsBefore, getElementPositions()) == 0) {
                direction = when(direction) {
                    Direction.UP -> break
                    Direction.DOWN -> Direction.UP
                    else -> break
                }
            }
            elements = driver.findElements(byDetect(locator))
            swipeCount++
        }

        return elements
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
        val expr = xpath.compile("//*[string-length(@text) > 0]")
        val nodes = expr.evaluate(document, XPathConstants.NODESET) as NodeList

        val result: MutableMap<String, Coords> = HashMap()
        val duplicateKeys: MutableSet<String> = HashSet()

        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            val resourceId = element.getAttribute("resource-id")
            val text = element.getAttribute("text")
            val bounds = element.getAttribute("bounds")
            val key = "$resourceId|$text"
            if (result.containsKey(key))
                duplicateKeys.add(key)
            if (bounds.isNotEmpty())
                result[key] = boundsToCoords(bounds)
        }

        duplicateKeys.forEach {
            if (result.containsKey(it))
                result.remove(it)
        }

        return result
    }

    private fun boundsToCoords(bounds: String): Coords {
        val x1y1x2y2 = bounds.substring(1, bounds.length - 1).split("][")

        val x1y1 = x1y1x2y2[0].split(",")
        val x2y2 = x1y1x2y2[0].split(",")

        val x1 = x1y1[0].toInt()
        val y1 = x1y1[1].toInt()
        val x2 = x2y2[0].toInt()
        val y2 = x2y2[1].toInt()

        return Coords(x1, y1, x2 - x1, y2 - y1)
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

        swipe(Duration.ofMillis(500), centerX, startY, centerX, endY)
        return true
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

    private fun swipe(duration: Duration, startX: Int, startY: Int, endX: Int, endY: Int) {
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val swipe = Sequence(finger, 1)
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
        swipe.addAction(finger.createPointerDown(0))
        swipe.addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), endX, endY))
        swipe.addAction(finger.createPointerUp(0))
        driver.perform(listOf(swipe))
    }

    private fun tapOnPoint(xCoordinate: Int, yCoordinate: Int) {
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val clickSequence = Sequence(finger, 1)
        clickSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), xCoordinate, yCoordinate))
        clickSequence.addAction(finger.createPointerDown(0))
        clickSequence.addAction(finger.createPointerUp(0))
        driver.perform(listOf(clickSequence))
    }

    private fun getScrollableArea(): Coords? {
        val scrollableElements = driver.findElements(By.xpath("//*[@scrollable='true']"))
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

        val x = location.x
        val y = location.y + (size.height / 4)
        val width = size.width
        val height = size.height / 2

        return Coords(x, y, width, height)
    }

    override fun setValue(locator: Locator, value: String, sequenceMode: Boolean) {
        DriverHelper().handleStaleElementReferenceException("setValue", numberOfAttempts) {
            val webElement = getWebElement(locator)
            webElement.clear()
            if (sequenceMode) {
                webElement.click()
                driver.executeScript("mobile: type", mapOf(Pair("text", value)))
                hideKeyboard()
            } else {
                webElement.sendKeys(value)
            }
        }
    }

    override fun hideKeyboard() {
        try {
            if (driver.isKeyboardShown)
                driver.hideKeyboard()
        } catch (e: WebDriverException) {
            if (e.rawMessage.contains("The software keyboard cannot be hidden")) {
                Logger.debug("The software keyboard cannot be hidden", "hideKeyboard")
            } else {
                throw e
            }
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
                .until { getWebElements(locator, scrollToFind = true).isNotEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            getWebElements(locator, scrollToFind = false).isNotEmpty()
        }
    }

    override fun isNotExist(locator: Locator): Boolean {
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until { getWebElements(locator, scrollToFind = true).isEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            getWebElements(locator, scrollToFind = false).isEmpty()
        }
    }

    override fun isEnabled(locator: Locator): Boolean {
        return getWebElement(locator).isEnabled
    }

    override fun swipeElement(locator: Locator, direction: Direction) {
        val elementCenter = DriverHelper().getElementCenter(getWebElement(locator))
        val duration = Duration.ofMillis(500)
        val endX: Int
        val endY: Int
        when (direction) {
            Direction.UP -> {
                endX = elementCenter.x
                endY = appArea.y
            }
            Direction.DOWN -> {
                endX = elementCenter.x
                endY = appArea.y + appArea.height
            }
            Direction.LEFT -> {
                endX = appArea.x
                endY = elementCenter.y
            }
            Direction.RIGHT -> {
                endX = appArea.x + appArea.width
                endY = elementCenter.y
            }
        }
        swipe(duration, elementCenter.x, elementCenter.y, endX, endY)
    }

    override fun navigateBack() {
        driver.navigate().back()
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
}