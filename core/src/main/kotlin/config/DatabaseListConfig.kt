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

class DatabaseListConfig {
    companion object {
        private var databases: HashMap<String, DataBase> = HashMap()

        @Synchronized
        fun load(config: JSONObject) {
            try {
                if (config.has("DatabaseList")) {
                    Logger.debug("Reading databases from config", "DatabaseListConfig")
                    config.getJSONObject("DatabaseList").toMap().forEach { (name, data) ->
                        data as HashMap<*, *>
                        databases[name] = DataBase(
                            data["url"].toString(),
                            data["username"].toString(),
                            data["password"].toString(),
                            data["description"].toString()
                        )
                        Logger.debug("Database loaded from config: \"$name\"", "DatabaseListConfig")
                    }
                }
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "DatabaseListConfig")
                throw e
            }
        }

        fun getDatabase(name: String): DataBase? {
            return databases[name]
        }
    }
}

class DataBase(
    private val url: String,
    private val username: String,
    private val password: String,
    private val description: String
) {
    fun getUrl(): String {
        return url
    }

    fun getUsername(): String {
        return username
    }

    fun getPassword(): String {
        return password
    }

    fun getDescription(): String {
        return description
    }
}