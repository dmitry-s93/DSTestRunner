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

package config.reporter

import config.MainConfig
import logger.Logger
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import reporter.database.DatabaseReporterUtils

class DatabaseReporterConfig {
    companion object {
        var projectId: Long? = null

        @Synchronized
        fun load(config: JSONObject) {
            val logSource = "DatabaseReporterConfig"
            try {
                Logger.debug("Reading parameters from config", logSource)
                if (config.has("DatabaseReporter")) {
                    val reporterConfig = config.getJSONObject("DatabaseReporter")
                    val driver = reporterConfig.getString("driver")
                    val url = reporterConfig.getString("url")
                    val user = reporterConfig.getString("user")
                    val password = reporterConfig.getString("password")

                    var createSchema = false
                    if (reporterConfig.has("createSchema"))
                        createSchema = reporterConfig.getBoolean("createSchema")

                    DatabaseReporterUtils().connectDatabase(url, driver, user, password)
                    transaction {
                        if (createSchema)
                            DatabaseReporterUtils().createSchema()
                        projectId = DatabaseReporterUtils().getProjectId((MainConfig.name))
                        DatabaseReporterUtils().createSession(projectId!!)
                    }
                } else {
                    Logger.warning("Database Reporter config is not specified", logSource)
                }
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", logSource)
                throw e
            }
        }
    }
}