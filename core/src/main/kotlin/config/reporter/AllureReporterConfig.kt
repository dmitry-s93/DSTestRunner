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

package config.reporter

import logger.Logger
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AllureReporterConfig {
    companion object {
        private lateinit var _reportDir: Path

        @Synchronized
        fun load(config: JSONObject) {
            val logSource = "AllureReporterConfig"
            try {
                Logger.debug("Reading parameters from config", logSource)
                if (config.has("AllureReporter")) {
                    val reporterConfig = config.getJSONObject("AllureReporter")
                    _reportDir = if (reporterConfig.has("reportDir"))
                        Paths.get(reporterConfig.getString("reportDir"))
                    else
                        getDefaultPath()
                } else {
                    _reportDir = getDefaultPath()
                    Logger.debug("Allure Reporter config is not specified. The default settings are used.", logSource)
                }
                Files.createDirectories(_reportDir)
                Logger.info("The test results will be saved to the \"$_reportDir\" directory", logSource)
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", logSource)
                throw e
            }
        }

        private fun getDefaultPath(): Path {
            return Paths.get(System.getProperty("user.dir"), "build", "allure-results")
        }

        val reportDir: String
            get() = _reportDir.toString()
    }
}