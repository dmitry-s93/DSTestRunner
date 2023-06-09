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

class PreloaderConfig {
    companion object {
        private var elements: MutableList<String> = mutableListOf()
        private var isLoaded: Boolean = false

        @Synchronized
        private fun readConfig() {
            if (isLoaded) return
            try {
                Logger.debug("Reading parameters from config", "PreloaderConfig")
                val config = JSONObject(ResourceUtils().getResourceByName(MainConfig.getConfiguration()))
                if (config.has("PreloaderElements")) {
                    config.getJSONArray("PreloaderElements").forEach { element ->
                        elements.add(element.toString())
                    }
                }
                isLoaded = true
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "PreloaderConfig")
                throw e
            }
        }

        fun getElements(): List<String> {
            if (!isLoaded) readConfig()
            return elements
        }
    }
}