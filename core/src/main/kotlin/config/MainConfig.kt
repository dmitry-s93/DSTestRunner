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

import logger.LogLevel
import logger.Logger
import org.json.JSONObject

class MainConfig {
    companion object {
        var name: String = ""
            private set
        var threads: Int = 0
            private set
        var driverImpl: String = ""
            private set
        var reporterImpl: String = ""
            private set
        var testSource: String = ""
            private set
        var consoleLogLevel: Int = 1
            private set
        val sessionId: String = System.currentTimeMillis().toString()

        @Synchronized
        fun load(config: JSONObject) {
            try {
                val mainConfig = config.getJSONObject("Main")
                name = mainConfig.getString("name")
                threads = mainConfig.getInt("threads")
                driverImpl = mainConfig.getString("driverImpl")
                reporterImpl = mainConfig.getString("reporterImpl")
                testSource = mainConfig.getString("testSource")
                val consoleLogLevelString = mainConfig.getString("consoleLogLevel").uppercase()
                try {
                    consoleLogLevel = LogLevel.valueOf(consoleLogLevelString).value
                } catch (e: IllegalArgumentException) {
                    Logger.warning(
                        "Invalid logging level specified (\"$consoleLogLevelString\"). The logging level will be set to \"WARN\".",
                        "MainConfig"
                    )
                }
                Logger.info("Session ID: $sessionId")
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "MainConfig")
                throw e
            }
        }
    }
}