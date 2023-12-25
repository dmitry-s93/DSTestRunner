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

import driver.web.BrowserType
import logger.Logger
import org.json.JSONObject

class WebDriverConfig {
    companion object {
        var url: String = ""
            private set
        var remoteAddress: String = ""
            private set
        var pageLoadTimeout: Long = 0
            private set
        var elementTimeout: Long = 0
            private set
        lateinit var browserType: BrowserType
            private set

        @Synchronized
        fun load(config: JSONObject) {
            try {
                Logger.debug("Reading parameters from config", "WebDriverConfig")
                val webDriverConfig = config.getJSONObject("WebDriver")
                browserType = BrowserType.valueOf(webDriverConfig.getString("browser").uppercase())
                url = webDriverConfig.getString("url")
                remoteAddress = webDriverConfig.getString("remoteAddress")
                pageLoadTimeout = webDriverConfig.getLong("pageLoadTimeout")
                elementTimeout = webDriverConfig.getLong("elementTimeout")
            } catch (e: Exception) {
                Logger.error("An error occurred while reading the config", "WebDriverConfig")
                throw e
            }
        }
    }
}