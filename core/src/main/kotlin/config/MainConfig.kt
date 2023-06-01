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

import logger.Logger
import org.json.JSONObject
import utils.ResourceUtils

class MainConfig {
    companion object {
        private lateinit var name: String
        private lateinit var configuration: String
        private var threads: Int = 0
        private lateinit var driverImpl: String
        private lateinit var reporterImpl: String
        private lateinit var testSource: String
        private lateinit var pageSource: String
        private var consoleLogLevel: Int = 0

        fun setConfiguration(configName: String) {
            configuration = configName
            readConfig()
        }

        fun getConfiguration(): String {
            return configuration
        }

        @Synchronized
        private fun readConfig() {
            try {
                val config = JSONObject(ResourceUtils().getResourceByName(configuration))
                val mainConfig = config.getJSONObject("Main")
                name = mainConfig.getString("name")
                threads = mainConfig.getInt("threads")
                driverImpl = mainConfig.getString("driverImpl")
                reporterImpl = mainConfig.getString("reporterImpl")
                testSource = mainConfig.getString("testSource")
                pageSource = mainConfig.getString("pageSource")
                consoleLogLevel = mainConfig.getInt("consoleLogLevel")
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "MainConfig")
                throw e
            }
        }

        fun getThreads(): Int {
            return threads
        }

        fun getDriverImpl(): String {
            return driverImpl
        }

        fun getReporterImpl(): String {
            return reporterImpl
        }

        fun getTestSource(): String {
            return testSource
        }

        fun getPageSource(): String {
            return pageSource
        }

        fun getConsoleLogLevel(): Int {
            return consoleLogLevel
        }
    }
}