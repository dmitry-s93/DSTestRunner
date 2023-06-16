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

import config.MainConfig
import org.json.JSONObject
import logger.Logger
import utils.ResourceUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AllureReporterConfig {
    companion object {
        private lateinit var reportDir: Path
        private var isLoaded: Boolean = false

        private fun readConfig() {
            if (isLoaded) return
            val logSource = "AllureReporterConfig"
            try {
                Logger.debug("Reading parameters from config", logSource)
                val config = JSONObject(ResourceUtils().getResourceByName(MainConfig.getConfiguration()))
                if (config.has("AllureReporter")) {
                    val reporterConfig = config.getJSONObject("AllureReporter")
                    reportDir = if (reporterConfig.has("reportDir"))
                        Paths.get(reporterConfig.getString("reportDir"))
                    else
                        getDefaultPath()
                } else {
                    reportDir = getDefaultPath()
                    Logger.debug("Allure Reporter config is not specified. The default settings are used.", logSource)
                }
                Files.createDirectories(reportDir)
                Logger.info("The test results will be saved to the \"$reportDir\" directory", logSource)
                isLoaded = true
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", logSource)
                throw e
            }
        }

        private fun getDefaultPath(): Path {
            return Paths.get(System.getProperty("user.dir"), "build", "allure-results")
        }

        fun getReportDir(): String {
            if (!isLoaded) readConfig()
            return reportDir.toString()
        }
    }
}