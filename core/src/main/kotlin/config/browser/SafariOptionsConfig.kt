/* Copyright 2024 DSTestRunner Contributors
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

package config.browser

import logger.Logger
import org.json.JSONObject

class SafariOptionsConfig {
    companion object {
        var arguments: MutableList<String> = mutableListOf()
            private set

        @Synchronized
        fun load(config: JSONObject) {
            try {
                if (config.has("SafariOptions")) {
                    Logger.debug("Reading parameters from config", "SafariOptionsConfig")
                    config.getJSONArray("SafariOptions").forEach {
                        arguments.add(it.toString())
                    }
                }
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "SafariOptionsConfig")
                throw e
            }
        }
    }
}