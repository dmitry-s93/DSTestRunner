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

class UserListConfig {
    companion object {
        private var users: HashMap<String, HashMap<String, String>> = HashMap()
        private var isLoaded: Boolean = false

        @Synchronized
        private fun readConfig() {
            if (isLoaded) return
            try {
                Logger.debug("Reading users from config", "UserListConfig")
                val config = JSONObject(ResourceUtils().getResourceByName(MainConfig.getConfiguration()))
                config.getJSONObject("UserList").toMap().forEach { (name, data) ->
                    val userData: HashMap<String, String> = HashMap()
                    (data as HashMap<*, *>).forEach { (fieldName, fieldValue) ->
                        userData[fieldName.toString()] = fieldValue.toString()
                    }
                    users[name] = userData
                    Logger.debug("User loaded from config: \"$name\"", "UserListConfig")
                }

                isLoaded = true
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "UserListConfig")
                throw e
            }
        }

        fun getUser(name: String): HashMap<String, String>? {
            if (!isLoaded) readConfig()
            return users[name]
        }
    }
}