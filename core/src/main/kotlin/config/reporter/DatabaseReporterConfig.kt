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
import storage.SessionParametersStorage
import java.util.UUID

class DatabaseReporterConfig {
    companion object : ReporterConfig {
        var projectId: UUID? = null
        var runId: UUID? = null

        private var driver: String? = null
        private var url: String? = null
        private var user: String? = null
        private var password: String? = null

        @Synchronized
        override fun load(config: JSONObject) {
            val logSource = "DatabaseReporterConfig"
            try {
                Logger.debug("Reading parameters from config", logSource)
                if (config.has("DatabaseReporter")) {
                    val reporterConfig = config.getJSONObject("DatabaseReporter")
                    driver = reporterConfig.getString("driver")
                    url = reporterConfig.getString("url")
                    user = reporterConfig.getString("user")
                    password = reporterConfig.getString("password")
                    val configuredProjectId = readProjectId(reporterConfig)

                    this.projectId = configuredProjectId
                } else {
                    Logger.warning("Database Reporter config is not specified", logSource)
                }
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", logSource)
                throw e
            }
        }

        @Synchronized
        override fun prepare() {
            if (SessionParametersStorage.threadCount <= 0)
                throw IllegalStateException(
                    "Reporter preparation must run after session parameters are initialized"
                )

            val reporterUtils = DatabaseReporterUtils()
            reporterUtils.connectDatabase(url!!, driver!!, user!!, password!!)
            transaction {
                val resolvedProjectId = reporterUtils.resolveProjectId(projectId!!)
                reporterUtils.createSession(resolvedProjectId)
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
