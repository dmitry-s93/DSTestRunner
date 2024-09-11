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
import io.appium.java_client.AppiumBy.ByAccessibilityId
import io.appium.java_client.AppiumBy.ByAndroidUIAutomator
import io.appium.java_client.Location
import io.appium.java_client.android.AndroidDriver
import logger.Logger
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
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt


@Suppress("unused")
class AndroidAppiumDriver : Driver {
    private lateinit var driver: AndroidDriver
    private val pageLoadTimeout: Long = AppiumDriverConfig.pageLoadTimeout
    private val elementTimeout: Long = AppiumDriverConfig.elementTimeout
    private val poolDelay: Long = 50
    private val maxSwipeCount = 20
    private val preloaderElements: List<Locator> = PreloaderConfig.elements
    private val unsupportedOperationMessage = "Operation not supported"
    private val viewportArea: Coords
    private var device: Device? = null
    private val numberOfAttempts = 3

    init {
        importDeviceAndStartSession()
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
        viewportArea = getViewportRect()
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
            driver = AndroidDriver(remoteAddress, capabilities)
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
        if (longScreenshot && screenshotAreas.isEmpty())
            return getLongScreenshot(ignoredElements, ignoredRectangles, screenshotAreas)
        return getSingleScreenshot(ignoredElements, ignoredRectangles, screenshotAreas)
    }

    private fun getSingleScreenshot(ignoredElements: Set<Locator>, ignoredRectangles: Set<Rectangle>, screenshotAreas: Set<Locator>): Screenshot {
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
            screenshot = Screenshot(takeScreenshot(viewportArea))
            screenshot.originShift = viewportArea
        }
        val ignoredAreas: MutableSet<Coords> = HashSet()
        ignoredAreas.addAll(getIgnoredAreas(ignoredElements, screenshot.originShift))
        ignoredAreas.addAll(DriverHelper().rectanglesToCoords(ignoredRectangles))
        screenshot.ignoredAreas = Coords.intersection(screenshot.coordsToCompare, ignoredAreas)
        return screenshot
    }

    private fun getLongScreenshot(ignoredElements: Set<Locator>, ignoredRectangles: Set<Rectangle>, screenshotAreas: Set<Locator>): Screenshot {
        val pauseAtExtremePoints: Long = 250
        val scrollableArea = getScrollableArea() ?: return getSingleScreenshot(ignoredElements, ignoredRectangles, screenshotAreas)
        scrollToTop()
        Thread.sleep(pauseAtExtremePoints)

        val maxImageHeight = viewportArea.height * 4
        val bufferedImageList: LinkedList<BufferedImage> = LinkedList()
        val ignoredAreas: MutableSet<Coords> = HashSet()

        var imageHeight = scrollableArea.y + scrollableArea.height - viewportArea.y
        var originShift = Coords(viewportArea.x, viewportArea.y, viewportArea.width,  imageHeight)
        bufferedImageList.add(takeScreenshot(originShift))
        ignoredAreas.addAll(getIgnoredAreas(ignoredElements, originShift))
        ignoredAreas.addAll(DriverHelper().rectanglesToCoords(ignoredRectangles))

        do {
            val elementPositionsBefore = getElementPositions()
            if (!scroll(Direction.UP))
                break
            val scrollSize = getScrollSize(elementPositionsBefore, getElementPositions())
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
        return image.getSubimage(coords.x, coords.y, coords.width, coords.height)
    }

    private fun getIgnoredAreas(locators: Set<Locator>, originShift: Coords, yOffset: Int = 0): Set<Coords> {
        val ignoredAreas: HashSet<Coords> = HashSet()
        locators.forEach { locator ->
            DriverHelper().handleStaleElementReferenceException("getIgnoredAreas", numberOfAttempts) {
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
        }
        return ignoredAreas
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
                    val elements = getWebElements(locator, scrollToFind = scrollToFind)
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
        val nodes = DriverHelper().getNodesByXpath(driver.pageSource, "//*[string-length(@text) > 0]")
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
            LocatorType.CLASS_NAME -> By.ByClassName(locator.value)
            LocatorType.ID -> By.id(locator.value)
            LocatorType.ACCESSIBILITY_ID -> ByAccessibilityId(locator.value)
            LocatorType.ANDROID_UI_AUTOMATOR -> ByAndroidUIAutomator(locator.value)
            null -> By.xpath(locator.value)
            else -> throw UnsupportedOperationException("Locator type not supported: ${locator.type.value}")
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

        swipe(centerX, startY, centerX, endY)
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

    private fun swipe(startX: Int, startY: Int, endX: Int, endY: Int) {
        val duration = countSwipeDuration(startX, startY, endX, endY)
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
        val swipe = Sequence(finger, 1)
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
        swipe.addAction(finger.createPointerDown(0))
        swipe.addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), endX, endY))
        // To prevent inertial scrolling from working
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(150), PointerInput.Origin.viewport(), endX, endY))
        swipe.addAction(finger.createPointerUp(0))
        driver.perform(listOf(swipe))
    }

    private fun countSwipeDuration(x1: Int, y1: Int, x2: Int, y2: Int): Duration {
        val fullAreaSwipeDuration = 1.2 // in seconds
        val distanceInPx = sqrt((x2 - x1).toDouble().pow(2) + (y2 - y1).toDouble().pow(2))
        val duration = ceil(fullAreaSwipeDuration / viewportArea.height * distanceInPx * 10) * 100
        return Duration.ofMillis(duration.toLong())
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

    override fun setValue(locator: Locator, value: String, sequenceMode: Boolean, hideKeyboard: Boolean) {
        DriverHelper().handleStaleElementReferenceException("setValue", numberOfAttempts) {
            val webElement = getWebElement(locator)
            webElement.clear()
            if (sequenceMode) {
                webElement.click()
                driver.executeScript("mobile: type", mapOf(Pair("text", value)))
                if (hideKeyboard)
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

    override fun isExist(locator: Locator, scrollToFindElement: Boolean?, waitAtMostMillis: Long?): Boolean {
        var scrollToFind = if (scrollToFindElement != null) { scrollToFindElement } else { false }
        var waitAtMost = elementTimeout
        if (waitAtMostMillis != null) {
            if (waitAtMostMillis > 0)
                waitAtMost = waitAtMostMillis
            else
                return getWebElements(locator, scrollToFind = false).isNotEmpty()
        }
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(waitAtMost))
                .until { getWebElements(locator, scrollToFind = scrollToFind).isNotEmpty() }
            true
        } catch (e: ConditionTimeoutException) {
            getWebElements(locator, scrollToFind = false).isNotEmpty()
        }
    }

    override fun isNotExist(locator: Locator, scrollToFindElement: Boolean?, waitAtMostMillis: Long?): Boolean {
        var scrollToFind = if (scrollToFindElement != null) { scrollToFindElement } else { true }
        var waitAtMost = elementTimeout
        if (waitAtMostMillis != null) {
            if (waitAtMostMillis > 0)
                waitAtMost = waitAtMostMillis
            else
                return getWebElements(locator, scrollToFind = false).isEmpty()
        }
        return try {
            Awaitility.await()
                .ignoreException(StaleElementReferenceException::class.java)
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(waitAtMost))
                .until { getWebElements(locator, scrollToFind = scrollToFind).isEmpty() }
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

    override fun navigateBack() {
        driver.navigate().back()
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        driver.location = Location(latitude, longitude)
    }

    override fun getLocation(): Location {
        return driver.location
    }

    override fun installApp(appPath: String?) {
        if (appPath.isNullOrEmpty())
            driver.installApp(device?.capabilities?.getCapability("appium:app").toString())
        else
            driver.installApp(appPath)
    }

    override fun activateApp(bundleId: String?) {
        if (bundleId.isNullOrEmpty())
            driver.activateApp(getAppPackage().toString())
        else
            driver.activateApp(bundleId)
    }

    override fun terminateApp(bundleId: String?) {
        if (bundleId.isNullOrEmpty())
            driver.terminateApp(getAppPackage().toString())
        else
            driver.terminateApp(bundleId)
    }

    override fun removeApp(bundleId: String?) {
        if (bundleId.isNullOrEmpty())
            driver.removeApp(getAppPackage().toString())
        else
            driver.removeApp(bundleId)
    }

    override fun isAppInstalled(bundleId: String?): Boolean {
        if (bundleId.isNullOrEmpty())
            return driver.isAppInstalled(getAppPackage().toString())
        return driver.isAppInstalled(bundleId)
    }

    private fun getAppPackage(): Any? {
        return device?.capabilities?.getCapability("appium:appPackage")
    }

    override fun getAlertText(): String {
        return getAlert().text
    }

    override fun acceptAlert(buttonLabel: String?) {
        if (buttonLabel != null)
            Logger.warning("The 'buttonLabel' parameter is not supported on Android", "acceptAlert")
        getAlert().accept()
    }

    override fun dismissAlert(buttonLabel: String?) {
        if (buttonLabel != null)
            Logger.warning("The 'buttonLabel' parameter is not supported on Android", "dismissAlert")
        getAlert().dismiss()
    }

    private fun getAlert(): Alert {
        try {
            Awaitility.await()
                .atLeast(Duration.ofMillis(0))
                .pollDelay(Duration.ofMillis(poolDelay))
                .atMost(Duration.ofMillis(pageLoadTimeout))
                .until { isAlertPresent() }
        } catch (_: ConditionTimeoutException) {
            return driver.switchTo().alert()
        }
        return driver.switchTo().alert()
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
                            val x = center.x + point.x
                            val y = center.y + point.y

                            var duration = Duration.ZERO
                            if (action.millis != null) {
                                duration = Duration.ofMillis(action.millis)
                            } else if (lastPoint != null) {
                                duration = countSwipeDuration(lastPoint!!.x, lastPoint!!.y, x, y)
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
}