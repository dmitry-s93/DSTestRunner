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

package driver

import config.MainConfig
import logger.Logger


class DriverSession {
    companion object {
        private var driverSession = ThreadLocal<Driver>()

        fun createSession() {
            try {
                if (driverSession.get() != null) {
                    Logger.warning("An open driver session was detected. It will be closed.", "createSession")
                    driverSession.get().quit()
                    driverSession.set(null)
                }
                driverSession.set(DriverFactory().createDriver(MainConfig.driverImpl))
            } catch (e: Exception) {
                Logger.error("Failed to create driver session\n${e.cause}", "createSession")
                throw e
            }
        }

        fun getSession(): Driver {
            if (driverSession.get() == null)
                throw Exception("Driver session not found")
            return driverSession.get()
        }

        fun closeSession() {
            if (driverSession.get() != null) {
                driverSession.get().quit()
                driverSession.set(null)
            } else
                Logger.warning("Driver session not found", "closeSession")
        }
    }
}