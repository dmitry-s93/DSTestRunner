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

import org.json.JSONObject
import logger.Logger
import utils.ResourceUtils

class WebDriverConfig {
    companion object {
        private lateinit var url: String
        private lateinit var remoteAddress: String
        private var pageLoadTimeout: Long = 0
        private var implicitlyWait: Long = 0
        private var isLoaded: Boolean = false

        @Synchronized
        private fun readConfig() {
            if (isLoaded) return
            try {
                Logger.debug("Reading parameters from config", "WebDriverConfig")
                val config = JSONObject(ResourceUtils().getResourceByName(MainConfig.getConfiguration()))
                val webDriverConfig = config.getJSONObject("WebDriver")
                url = webDriverConfig.getString("url")
                remoteAddress = webDriverConfig.getString("remoteAddress")
                pageLoadTimeout = webDriverConfig.getLong("pageLoadTimeout")
                implicitlyWait = webDriverConfig.getLong("implicitlyWait")
                isLoaded = true
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "WebDriverConfig")
                throw e
            }
        }

        fun getUrl(): String {
            if (!isLoaded) readConfig()
            return url
        }

        fun getRemoteAddress(): String {
            if (!isLoaded) readConfig()
            return remoteAddress
        }

        fun getPageLoadTimeout(): Long {
            if (!isLoaded) readConfig()
            return pageLoadTimeout
        }

        fun getImplicitlyWait(): Long {
            if (!isLoaded) readConfig()
            return implicitlyWait
        }
    }
}