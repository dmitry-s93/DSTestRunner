package driver.mobile

import config.AppiumDriverConfig
import driver.Driver
import io.appium.java_client.android.AndroidDriver
import pazone.ashot.Screenshot
import java.awt.Point
import java.net.URL
import java.time.Duration


@Suppress("unused")
class AndroidAppiumDriver : Driver {
    private val driver: AndroidDriver

    init {
        driver = AndroidDriver(URL(AppiumDriverConfig.remoteAddress), AppiumDriverConfig.desiredCapabilities)
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0))
    }

    override fun click(locator: String, points: ArrayList<Point>?) {
        TODO("Not yet implemented")
    }

    override fun checkLoadPage(url: String, identifier: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun switchToWindow(url: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun closeWindow(url: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCurrentUrl(): String {
        TODO("Not yet implemented")
    }

    override fun getElementValue(locator: String): String {
        TODO("Not yet implemented")
    }

    override fun getScreenshot(longScreenshot: Boolean, ignoredElements: Set<String>, screenshotAreas: List<String>): Screenshot {
        TODO("Not yet implemented")
    }

    override fun setPage(url: String) {
        TODO("Not yet implemented")
    }

    override fun setValue(locator: String, value: String, sequenceMode: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setSelectValue(locator: String, value: String) {
        TODO("Not yet implemented")
    }

    override fun uploadFile(locator: String, file: String) {
        TODO("Not yet implemented")
    }

    override fun isExist(locator: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun isNotExist(locator: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun executeJavaScript(script: String, vararg args: Any?): Any? {
        TODO("Not yet implemented")
    }

    override fun quit() {
        driver.quit()
    }
}