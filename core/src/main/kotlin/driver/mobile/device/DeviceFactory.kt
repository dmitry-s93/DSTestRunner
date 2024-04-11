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

package driver.mobile.device

import logger.Logger
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import java.time.Duration

class DeviceFactory {
    companion object {
        private val freeDevices: MutableSet<Device> = mutableSetOf()
        private val usedDevices: MutableSet<Device> = mutableSetOf()
        private val blockedDevices: MutableSet<Device> = mutableSetOf()
        private const val LOG_SOURCE = "DeviceFactory"

        @Synchronized
        fun addDevice(device: Device) {
            freeDevices.add(device)
            Logger.info("Device added: '${device.name}'", LOG_SOURCE)
        }

        fun importDevice(): Device {
            var device = getDevice()
            if (device == null && usedDevices.isNotEmpty()) {
                Logger.info("No free devices. Waiting for device.", LOG_SOURCE)
                try {
                    Awaitility.await()
                        .pollInterval(Duration.ofSeconds(1))
                        .atMost(Duration.ofMinutes(30))
                        .until {
                            device = getDevice()
                            device != null || usedDevices.isEmpty()
                        }
                } catch (_: ConditionTimeoutException) {
                    Logger.info("No free devices appeared within the allotted time", LOG_SOURCE)
                }
            }
            if (device == null)
                throw NoDeviceException("No devices available")
            return device!!
        }

        @Synchronized
        fun getDevice(): Device? {
            if (freeDevices.isEmpty())
                return null
            val device = freeDevices.first()
            usedDevices.add(device)
            freeDevices.remove(device)
            Logger.info("Device imported: '${device.name}'", LOG_SOURCE)
            return device
        }

        @Synchronized
        fun returnDevice(device: Device) {
            freeDevices.add(device)
            usedDevices.remove(device)
            Logger.info("Device returned: '${device.name}'", LOG_SOURCE)
        }

        @Synchronized
        fun addDeviceToBlocklist(device: Device) {
            freeDevices.remove(device)
            usedDevices.remove(device)
            blockedDevices.add(device)
            Logger.info("Device added to blocklist: '${device.name}'", LOG_SOURCE)
        }
    }
}