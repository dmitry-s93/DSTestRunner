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

import config.reporter.AllureReporterConfig
import config.reporter.CsvReporterConfig
import logger.Logger
import org.json.JSONObject
import utils.ResourceUtils

class ConfigLoader {
    @Synchronized
    fun loadConfiguration(configName: String) {
        try {
            Logger.info("The settings from the \"$configName\" file are used")
            val config = JSONObject(ResourceUtils().getResourceByName(configName))

            MainConfig.load(config)
            if (MainConfig.driverImpl.contains("driver.web"))
                WebDriverConfig.load(config)
            if (MainConfig.driverImpl.contains("driver.mobile"))
                AppiumDriverConfig.load(config)
            ScreenshotConfig.load(config)
            BrowserOptionsConfig.load(config)
            PreloaderConfig.load(config)
            DatabaseListConfig.load(config)
            UserListConfig.load(config)

            if (MainConfig.reporterImpl == "reporter.allure.AllureReporter")
                AllureReporterConfig.load(config)
            if (MainConfig.reporterImpl == "reporter.allure.CsvReporter")
                CsvReporterConfig.load(config)
        } catch (e: Exception) {
            Logger.error("An error occurred while reading the config", "ConfigLoader")
            throw e
        }
    }
}