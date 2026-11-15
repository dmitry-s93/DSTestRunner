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

package reporter.database

import config.reporter.DatabaseReporterConfig
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import storage.SessionParametersStorage
import java.util.UUID

class DatabaseReporterUtils {
    fun connectDatabase(url: String, driver: String, user: String, password: String) {
        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )
    }

    /**
     * Verifies that the project referenced by the runner exists.
     * Projects are created in the report application only, so a missing project is an error.
     */
    fun resolveProjectId(projectId: UUID): UUID {
        val project = ProjectsTable.selectAll()
            .where(ProjectsTable.idRow eq projectId)
            .singleOrNull()
            ?: throw IllegalStateException("Project not found: $projectId")
        return project[ProjectsTable.idRow]
    }

    /**
     * Registers the run for the current session and exposes its identifier to the reporters.
     */
    @Synchronized
    fun createSession(projectId: UUID) {
        val now = Clock.System.now()
        val runId = UUID.randomUUID()
        RunsTable.insert {
            it[idRow] = runId
            it[projectIdRow] = projectId
            it[startedAtRow] = now
            it[finishedAtRow] = null
            it[threadsRow] = SessionParametersStorage.threadCount
            it[testsRow] = SessionParametersStorage.testCount
            it[versionRow] = 1
            it[createdAtRow] = now
            it[updatedAtRow] = now
        }
        DatabaseReporterConfig.runId = runId
    }
}
