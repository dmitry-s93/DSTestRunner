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

import logger.Logger


class DriverFactory {
    fun createDriver(className: String): Driver {
        try {
            val driverClass = Class.forName(className)
            return driverClass.getDeclaredConstructor().newInstance() as Driver
        } catch (e: ClassNotFoundException) {
            Logger.error("Could not find class '$className'", "createDriver")
            throw ClassNotFoundException()
        }
    }
}