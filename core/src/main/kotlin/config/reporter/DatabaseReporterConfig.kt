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

import logger.Logger
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import reporter.database.DatabaseReporterUtils
import java.util.UUID

class DatabaseReporterConfig {
    companion object {
        var projectId: UUID? = null
        var runId: UUID? = null

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
                    val configuredProjectId = readProjectId(reporterConfig)

                    this.projectId = configuredProjectId

                    DatabaseReporterUtils().connectDatabase(url, driver, user, password)
                    transaction {
                        val resolvedProjectId = DatabaseReporterUtils().resolveProjectId(configuredProjectId)
                        DatabaseReporterUtils().createSession(resolvedProjectId)
                    }
                } else {
                    Logger.warning("Database Reporter config is not specified", logSource)
                }
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", logSource)
                throw e
            }
        }

        /**
         * The runner only writes data into an existing project created in the report application,
         * therefore a valid project identifier is mandatory.
         */
        private fun readProjectId(reporterConfig: JSONObject): UUID {
            val projectIdValue = reporterConfig.opt("projectId")
            if (projectIdValue !is String || projectIdValue.isBlank())
                throw IllegalArgumentException(
                    "Database Reporter config must contain \"projectId\": the UUID of a project created in the report application"
                )
            val rawProjectId = projectIdValue
            return try {
                UUID.fromString(rawProjectId)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid \"projectId\" value in Database Reporter config: \"$rawProjectId\" is not a valid UUID",
                    e
                )
            }
        }
    }
}
