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

package config

import driver.mobile.device.Device
import driver.mobile.device.DeviceFactory
import logger.Logger
import org.json.JSONObject
import org.openqa.selenium.remote.DesiredCapabilities

class AppiumDriverConfig {
    companion object {
        var pageLoadTimeout: Long = 0
            private set
        var elementTimeout: Long = 0
            private set

        @Synchronized
        fun load(config: JSONObject) {
            try {
                Logger.debug("Reading parameters from config", "AppiumDriverConfig")
                val appiumDriverConfig = config.getJSONObject("AppiumDriver")
                pageLoadTimeout = appiumDriverConfig.getLong("pageLoadTimeout")
                elementTimeout = appiumDriverConfig.getLong("elementTimeout")
                val generalCapabilities = appiumDriverConfig.getJSONObject("DesiredCapabilities").toMap()
                val devices = appiumDriverConfig.getJSONObject("devices")
                devices.keys().forEach { deviceName ->
                    val device = devices.getJSONObject(deviceName)
                    val remoteAddress = device.getString("remoteAddress")
                    val deviceCapabilities = device.getJSONObject("DesiredCapabilities").toMap()
                    val capabilities: MutableMap<String, Any> = mutableMapOf()
                    capabilities.putAll(generalCapabilities)
                    capabilities.putAll(deviceCapabilities)
                    DeviceFactory.addDevice(Device(deviceName, remoteAddress, DesiredCapabilities(capabilities)))
                }
            } catch (e: Exception) {
                Logger.error("An error occurred while reading the config", "AppiumDriverConfig")
                throw e
            }
        }
    }
}