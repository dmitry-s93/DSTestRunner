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

import action.actions.ActionType
import action.actions.TouchAction
import action.helper.Direction
import config.AppiumDriverConfig
import config.PreloaderConfig
import config.ScreenshotConfig
import driver.Driver
import driver.DriverHelper
import driver.mobile.device.Device
import driver.mobile.device.DeviceFactory
import driver.mobile.device.NoDeviceException
import io.appium.java_client.AppiumBy
import io.appium.java_client.AppiumDriver
import io.appium.java_client.Location
import io.appium.java_client.ios.IOSDriver
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.openqa.selenium.*
import org.openqa.selenium.interactions.Pause
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.w3c.dom.Element
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
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URL
import java.time.Duration
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.abs
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
        importDeviceAndStartSession()
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
        viewportArea = getViewportRect()
        screenScale = getScreenScale()
    }

    private fun importDeviceAndStartSession() {
        try {
            device = DeviceFactory.importDevice()
            startSession(retry = true)
        } catch (e: NoDeviceException) {
            throw e
        } catch (e: Exception) {
            importDeviceAndStartSession()
        }
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
            device = null
            throw e
        }
    }

    private fun getViewportRect(): Coords {
        AppiumDriverConfig.viewportRect?.let { return it }
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

    override fun click(locator: Locator, scrollToFindElement: Boolean?) {
        DriverHelper().handleStaleElementReferenceException("click", numberOfAttempts) {
            if (scrollToFindElement != null) {
                getWebElement(locator, scrollToFindElement).click()
            } else {
                getWebElement(locator).click()
            }
        }
    }

    override fun checkLoadPage(url: String?, identifier: Locator?, skipPreloaderCheck: Boolean): Boolean {
        var scrollToFind = false
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(pageLoadTimeout))
                .conditionEvaluationListener { condition ->
                    if (condition.elapsedTimeInMS > 3000)
                        scrollToFind = true
                }
                .until {
                    (skipPreloaderCheck || !isPreloaderDisplayed()) && (identifier == null || getWebElements(identifier, onlyDisplayed = false, scrollToFind).isNotEmpty())
                }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    private fun isPreloaderDisplayed(): Boolean {
        preloaderElements.forEach { locator ->
            if (getWebElements(locator, onlyDisplayed = true, scrollToFind = false).isNotEmpty())
                return true
        }
        return false
    }

    override fun getElementValue(locator: Locator, scrollToFindElement: Boolean?, attributeName: String?): String {
        var value = ""
        DriverHelper().handleStaleElementReferenceException("getElementValue", numberOfAttempts) {
            val element =
                if (scrollToFindElement != null) {
                    getWebElement(locator, scrollToFindElement)
                } else {
                    getWebElement(locator)
                }
            if (attributeName.isNullOrEmpty()) {
                value = element.text
            } else {
                value = element.getAttribute(attributeName)
            }
        }
        return value
    }

    override fun getScreenshot(
        longScreenshot: Boolean,
        ignoredElements: Set<Locator>,
        ignoredRectangles: Set<Rectangle>,
        screenshotAreas: Set<Locator>
    ): Screenshot {
        hideKeyboard()
        val waitTime = ScreenshotConfig.waitTimeBeforeScreenshot
        if (waitTime > 0)
            Thread.sleep(waitTime)
        if (longScreenshot && screenshotAreas.isEmpty() && isPageScrollable())
            return getLongScreenshot(ignoredElements, ignoredRectangles, screenshotAreas)
        return getSingleScreenshot(ignoredElements, ignoredRectangles, screenshotAreas)
    }

    private fun getSingleScreenshot(ignoredElements: Set<Locator>, ignoredRectangles: Set<Rectangle>, screenshotAreas: Set<Locator>): Screenshot {
        val screenshot: Screenshot
        if (screenshotAreas.isNotEmpty()) {
            screenshot = with(AShot()) {
                shootingStrategy(ShootingStrategies.simple())
                imageCropper(IndentCropper().addIndentFilter(IndentFilerFactory.blur()))
                takeScreenshot(driver, getElementCoordinates(screenshotAreas))
            }
        } else {
            screenshot = Screenshot(takeScreenshot(viewportArea))
            screenshot.originShift = viewportArea
        }
        val ignoredAreas: MutableSet<Coords> = HashSet()
        ignoredAreas.addAll(getElementCoordinates(ignoredElements, screenshot.originShift))
        ignoredAreas.addAll(DriverHelper().rectanglesToCoords(ignoredRectangles))
        screenshot.ignoredAreas = Coords.intersection(screenshot.coordsToCompare, ignoredAreas)
        return screenshot
    }

    private fun getLongScreenshot(ignoredElements: Set<Locator>, ignoredRectangles: Set<Rectangle>, screenshotAreas: Set<Locator>): Screenshot {
        val pauseAtExtremePoints: Long = 250
        val scrollableArea = DriverHelper().changeAreaSize(getScrollableArea(screenScale), coefficient = 0.7)
            ?: return getSingleScreenshot(ignoredElements, ignoredRectangles, screenshotAreas)
        scrollToTop()
        Thread.sleep(pauseAtExtremePoints)

        val maxImageHeight = viewportArea.height * 4
        val bufferedImageList: LinkedList<BufferedImage> = LinkedList()
        val ignoredAreas: MutableSet<Coords> = HashSet()

        var imageHeight = scrollableArea.y + scrollableArea.height - viewportArea.y
        var originShift = Coords(viewportArea.x, viewportArea.y, viewportArea.width,  imageHeight)
        bufferedImageList.add(takeScreenshot(originShift))
        ignoredAreas.addAll(getElementCoordinates(ignoredElements, originShift))
        ignoredAreas.addAll(DriverHelper().rectanglesToCoords(ignoredRectangles))

        fun adjustScrollSize(scrollSize: Int, recognizedScrollSize: Int, maxDiff: Int): Int {
            if (scrollSize != recognizedScrollSize && abs(scrollSize - recognizedScrollSize) <= maxDiff) {
                return recognizedScrollSize
            }
            return scrollSize
        }

        do {
            val elementPositionsBefore = getElementPositions()
            if (!scroll(Direction.UP))
                break
            var scrollSize = getScrollSize(elementPositionsBefore, getElementPositions()) * screenScale
            if (scrollSize > 0) {
                val currentImage = takeScreenshot()
                val recognizedScrollSize = ImageUtils().recognizeScrollSize(currentImage, bufferedImageList.last(), originShift.y, ignoredRectangles)
                scrollSize = adjustScrollSize(scrollSize, recognizedScrollSize, 20)

                val y = originShift.y + originShift.height - scrollSize
                originShift = Coords(viewportArea.x, y, viewportArea.width, scrollSize)
                bufferedImageList.add(cropImage(currentImage, originShift))
                ignoredAreas.addAll(getElementCoordinates(ignoredElements, originShift, imageHeight))
                imageHeight += scrollSize
            }
        } while (scrollSize > 0 && imageHeight < maxImageHeight)

        if (imageHeight < maxImageHeight) {
            Thread.sleep(pauseAtExtremePoints)
            val y = originShift.y + originShift.height
            val height = viewportArea.y + viewportArea.height - y
            if (height > 0) {
                originShift = Coords(viewportArea.x, y, viewportArea.width, height)
                bufferedImageList.add(takeScreenshot(originShift))
                ignoredAreas.addAll(getElementCoordinates(ignoredElements, originShift, imageHeight))
            }
        }

        var bufferedImage = ImageUtils().concatImageList(bufferedImageList)
        if (bufferedImage.height > maxImageHeight)
            bufferedImage = bufferedImage.getSubimage(0, 0, bufferedImage.width, maxImageHeight)

        val screenshot = Screenshot(bufferedImage)
        screenshot.ignoredAreas = Coords.intersection(screenshot.coordsToCompare, ignoredAreas)

        return screenshot
    }

    private fun isPageScrollable(): Boolean {
        val pageSource = getPageSource()
        val scrollableElementXpath = "//*[self::XCUIElementTypeScrollView or self::XCUIElementTypeCollectionView or self::XCUIElementTypeTable]"
        val nodes = DriverHelper().getNodesByXpath(pageSource, scrollableElementXpath)
        for (i in 0 until nodes.length) {
            val scrollableElement = nodes.item(i) as Element
            val scrollableElementY = scrollableElement.getAttribute("y").toInt()
            val scrollableElementHeight = scrollableElement.getAttribute("height").toInt()
            val childNodes = scrollableElement.childNodes
            for (j in 0 until childNodes.length) {
                if (childNodes.item(j).nodeType.toInt() == 1) {
                    val childNode = childNodes.item(j) as Element
                    val childNodeY = childNode.getAttribute("y").toInt()
                    val childNodeHeight = childNode.getAttribute("height").toInt()
                    if (childNodeY + childNodeHeight > scrollableElementY + scrollableElementHeight || childNodeY < scrollableElementY)
                        return true
                }
            }
        }
        return false
    }

    private fun takeScreenshot(area: Coords? = null): BufferedImage {
        val inputStream = ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES))
        if (area != null) {
            return cropImage(ImageIO.read(inputStream), area)
        }
        return ImageIO.read(inputStream)
    }

    private fun cropImage(image: BufferedImage, coords: Coords): BufferedImage {
        val x = coords.x
        val y = coords.y
        var width = coords.width
        var height = coords.height

        if (x + width > image.width)
            width = image.width - x
        if (y + height > image.height)
            height = image.height - y

        return image.getSubimage(x, y, width, height)
    }

    private fun getElementCoordinates(locators: Set<Locator>, originShift: Coords? = null, yOffset: Int = 0): Set<Coords> {
        val coordinates: HashSet<Coords> = HashSet()
        var originShiftX = 0
        var originShiftY = 0
        if (originShift != null) {
            originShiftX = originShift.x
            originShiftY = originShift.y
        }
        locators.forEach { locator ->
            DriverHelper().handleStaleElementReferenceException("getElementCoordinates", numberOfAttempts) {
                val webElements = getWebElements(locator, onlyDisplayed = true, scrollToFind = false)
                webElements.forEach { webElement ->
                    val elementLocation = webElement.location
                    val elementSize = webElement.size
                    if ((elementLocation.y * screenScale) + (elementSize.height * screenScale) >= originShiftY) {
                        val x = elementLocation.x * screenScale - originShiftX
                        val y = elementLocation.y * screenScale - originShiftY + yOffset
                        val width = elementSize.width * screenScale
                        val height = elementSize.height * screenScale
                        coordinates.add(Coords(x, y, width, height))
                    }
                }
            }
        }
        return coordinates
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
            if (getWebElements(locator, onlyDisplayed = true, scrollToFind = false).isEmpty())
                return
            getWebElement(locator).click()
        }
    }

    override fun setSelectValue(locator: Locator, value: String) {
        TODO("Not yet implemented")
    }

    override fun isExist(locator: Locator, scrollToFindElement: Boolean?, waitAtMostMillis: Long?): Boolean {
        var scrollToFind = if (scrollToFindElement != null) { scrollToFindElement } else { false }
        var waitAtMost = elementTimeout
        if (waitAtMostMillis != null) {
            if (waitAtMostMillis > 0)
                waitAtMost = waitAtMostMillis
            else
                return getWebElements(locator, onlyDisplayed = false, scrollToFind = false).isNotEmpty()
        }
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(waitAtMost))
                .until { getWebElements(locator, onlyDisplayed = false, scrollToFind = scrollToFind).isNotEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            getWebElements(locator, onlyDisplayed = false, scrollToFind = false).isNotEmpty()
        }
    }

    override fun isNotExist(locator: Locator, scrollToFindElement: Boolean?, waitAtMostMillis: Long?): Boolean {
        var scrollToFind = if (scrollToFindElement != null) { scrollToFindElement } else { false }
        var waitAtMost = elementTimeout
        if (waitAtMostMillis != null) {
            if (waitAtMostMillis > 0)
                waitAtMost = waitAtMostMillis
            else
                return getWebElements(locator, onlyDisplayed = false, scrollToFind = false).isEmpty()
        }
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(waitAtMost))
                .until { getWebElements(locator, onlyDisplayed = false, scrollToFind = scrollToFind).isEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            getWebElements(locator, onlyDisplayed = false, scrollToFind = false).isEmpty()
        }
    }

    override fun isEnabled(locator: Locator): Boolean {
        return getWebElement(locator).isEnabled
    }

    override fun navigateBack() {
        TODO("Not yet implemented")
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        driver.location = Location(latitude, longitude)
    }

    override fun getLocation(): Location {
        return driver.location
    }

    override fun swipeElement(locator: Locator, direction: Direction) {
        val elementCenter = DriverHelper().getElementCenter(getWebElement(locator))
        val endX: Int
        val endY: Int
        when (direction) {
            Direction.UP -> {
                endX = elementCenter.x
                endY = viewportArea.y / screenScale
            }
            Direction.DOWN -> {
                endX = elementCenter.x
                endY = (viewportArea.y + viewportArea.height) / screenScale
            }
            Direction.LEFT -> {
                endX = viewportArea.x / screenScale
                endY = elementCenter.y
            }
            Direction.RIGHT -> {
                endX = (viewportArea.x + viewportArea.width) / screenScale
                endY = elementCenter.y
            }
        }
        swipe(elementCenter.x, elementCenter.y, endX, endY)
    }

    override fun installApp(appPath: String?) {
        if (appPath.isNullOrEmpty())
            driver.installApp(device?.capabilities?.getCapability("appium:app").toString())
        else
            driver.installApp(appPath)
    }

    override fun activateApp(bundleId: String?) {
        if (bundleId.isNullOrEmpty())
            driver.activateApp(getBundleId().toString())
        else
            driver.activateApp(bundleId)
    }

    override fun terminateApp(bundleId: String?) {
        if (bundleId.isNullOrEmpty())
            driver.terminateApp(getBundleId().toString())
        else
            driver.terminateApp(bundleId)
    }

    override fun removeApp(bundleId: String?) {
        if (bundleId.isNullOrEmpty())
            driver.removeApp(getBundleId().toString())
        else
            driver.removeApp(bundleId)
    }

    override fun isAppInstalled(bundleId: String?): Boolean {
        if (bundleId.isNullOrEmpty())
            return driver.isAppInstalled(getBundleId().toString())
        return driver.isAppInstalled(bundleId)
    }

    private fun getBundleId(): Any? {
        return device?.capabilities?.getCapability("appium:bundleId")
    }

    override fun getAlertText(): String {
        waitForAlert()
        return driver.switchTo().alert().text
    }

    override fun acceptAlert(buttonLabel: String?) {
        waitForAlert()
        val args = HashMap<String, String>();
        args["action"] = "accept"
        if (buttonLabel != null)
            args["buttonLabel"] = buttonLabel
        driver.executeScript("mobile:alert", args)
    }

    override fun dismissAlert(buttonLabel: String?) {
        waitForAlert()
        val args = HashMap<String, String>();
        args["action"] = "dismiss"
        if (buttonLabel != null)
            args["buttonLabel"] = buttonLabel
        driver.executeScript("mobile:alert", args)
    }

    private fun waitForAlert() {
        try {
            Awaitility.await()
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(pageLoadTimeout))
                .until { isAlertPresent() }
        } catch (_: ConditionTimeoutException) {
            driver.switchTo().alert() // Will throw an exception
        }
    }

    private fun isAlertPresent(): Boolean {
        try {
            driver.switchTo().alert()
            return true
        } catch (e: NoAlertPresentException) {
            return false
        }
    }

    override fun performTouchAction(locator: Locator, actionList: MutableList<MutableList<TouchAction>>) {
        DriverHelper().handleStaleElementReferenceException("executeTouchAction", numberOfAttempts) {
            val element = getWebElement(locator)
            val center = DriverHelper().getElementCenter(element)
            actionList.forEach { actionSequence ->
                val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
                val sequence = Sequence(finger, 1)
                var lastPoint: Point? = null
                actionSequence.forEach { action ->
                    when (action.actionType) {
                        ActionType.POINTER_DOWN -> {
                            sequence.addAction(finger.createPointerDown(0))
                        }
                        ActionType.POINTER_UP -> {
                            sequence.addAction(finger.createPointerUp(0))
                        }
                        ActionType.POINTER_MOVE -> {
                            val point = action.point!!
                            val x = center.x + (point.x / screenScale)
                            val y = center.y + (point.y / screenScale)

                            var duration = Duration.ZERO
                            if (action.millis != null) {
                                duration = Duration.ofMillis(action.millis)
                            } else if (lastPoint != null) {
                                duration = countSwipeDuration(
                                    lastPoint!!.x * screenScale,
                                    lastPoint!!.y * screenScale,
                                    x * screenScale,
                                    y * screenScale
                                )
                            }

                            sequence.addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), x, y))
                            lastPoint = Point(x, y)
                        }
                        ActionType.PAUSE -> {
                            sequence.addAction(Pause(finger, Duration.ofMillis(action.millis!!)))
                        }
                    }
                }
                driver.perform(listOf(sequence))
                Thread.sleep(250)
            }
        }
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

    private fun getWebElement(locator: Locator, scrollToFind: Boolean = true): WebElement {
        var element: WebElement? = null
        try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(elementTimeout))
                .until {
                    val elements = getWebElements(locator, onlyDisplayed = false, scrollToFind = scrollToFind)
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

    private fun getWebElements(locator: Locator, onlyDisplayed: Boolean, scrollToFind: Boolean): List<WebElement> {
        var onlyDisplayedVar = onlyDisplayed
        if (locator.ignoreVisibility)
            onlyDisplayedVar = false
        var elements = filterElements(driver.findElements(byDetect(locator)), onlyDisplayedVar)
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
                    Direction.DOWN -> {
                        swipeCount = 0
                        Direction.UP
                    }
                    else -> break
                }
            }
            elements = filterElements(driver.findElements(byDetect(locator)), onlyDisplayedVar)
            swipeCount++
        }

        return elements
    }

    private fun filterElements(elements: List<WebElement>, onlyDisplayed: Boolean): List<WebElement> {
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
        val scrollableArea = DriverHelper().changeAreaSize(getScrollableArea(), coefficient = 0.5) ?: return false

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
        val scrollSizes = getScrollSizes(elementCoords1, elementCoords2)

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

    private fun getScrollSizes(elementCoords1: Map<String, Coords>, elementCoords2: Map<String, Coords>): HashMap<Int, Int> {
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

        return scrollSizes
    }

    private fun getElementPositions(): Map<String, Coords> {
        val nodes = DriverHelper().getNodesByXpath(getPageSource(), "//*[string-length(@name) > 0 or string-length(@label) > 0]")
        val result: MutableMap<String, Coords> = HashMap()
        val duplicateKeys: MutableSet<String> = HashSet()

        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            val elementName = element.getAttribute("name")
            val elementLabel = element.getAttribute("label")
            val key =
                if (elementName == elementLabel)
                    "${element.tagName}|$elementName"
                else
                    "${element.tagName}|$elementName|$elementLabel"
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

    private fun getPageSource(): String {
        return driver.executeScript(
            "mobile: source",
            mapOf(
                Pair("format", "xml"),
                Pair("excludedAttributes", "type,value,enabled,visible,accessible,index")
            )
        ).toString()
    }

    private fun swipe(startX: Int, startY: Int, endX: Int, endY: Int) {
        val duration = countSwipeDuration(
            startX * screenScale, startY * screenScale,
            endX * screenScale, endY * screenScale
        )
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val swipe = Sequence(finger, 1)
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
        swipe.addAction(finger.createPointerDown(0))
        swipe.addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), endX - 1, endY))
        // To prevent inertial scrolling from working
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
            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeScrollView' or type == 'XCUIElementTypeCollectionView' OR type == 'XCUIElementTypeTable'")
        )

        val screenHeight = (viewportArea.y + viewportArea.height) / screenScale * scale
        var scrollableElement: WebElement? = null

        scrollableElements.forEach { element ->
            val elementY = element.location.y * scale
            var elementHeight = element.size.height
            var scrollableElementHeight = scrollableElement?.size?.height ?: 0

            if (scrollableElement != null) {
                val invisibleHeight1 = elementY + elementHeight - screenHeight
                val invisibleHeight2 = scrollableElement.location.y + scrollableElementHeight - screenHeight

                if (invisibleHeight1 > 0)
                    elementHeight -= invisibleHeight1

                if (invisibleHeight2 > 0)
                    scrollableElementHeight -= invisibleHeight2
            }

            if (elementY < screenHeight && (scrollableElement == null || elementHeight > scrollableElementHeight)) {
                scrollableElement = element
            }
        }

        if (scrollableElement == null)
            return null

        val location = scrollableElement.location
        val size = scrollableElement.size

        val x = location.x * scale
        val y = location.y * scale
        val width = size.width * scale
        val height = size.height * scale

        return Coords(x, y, width, height)
    }

    override fun getMobileDriver(): AppiumDriver {
        return driver
    }
}