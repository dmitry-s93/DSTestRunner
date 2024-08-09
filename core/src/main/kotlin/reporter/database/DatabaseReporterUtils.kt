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

import config.MainConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import storage.SessionParametersStorage

class DatabaseReporterUtils {
    fun connectDatabase(url: String, driver: String, user: String, password: String) {
        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )
    }

    fun getProjectId(projectName: String): Long {
        val project = ProjectsTable.select(ProjectsTable.idRow).where(ProjectsTable.nameRow eq projectName).singleOrNull()
        if (project != null)
            return project[ProjectsTable.idRow]
        return createProject(projectName)
    }

    private fun createProject(projectName: String): Long {
        return ProjectsTable.insert {
            it[nameRow] = projectName
        } get ProjectsTable.idRow
    }

    fun createSchema() {
        SchemaUtils.create(ProjectsTable, SessionsTable, TestsTable, StepsTable, StepParametersTable)
    }

    @Synchronized
    fun createSession(projectId: Long) {
        val session = SessionsTable.select(SessionsTable.idRow).where(SessionsTable.idRow eq MainConfig.sessionId)
            .singleOrNull()
        if (session == null) {
            SessionsTable.insert {
                it[idRow] = MainConfig.sessionId
                it[projectIdRow] = projectId
                it[threadsRow] = SessionParametersStorage.threadCount
                it[testsRow] = SessionParametersStorage.testCount
            }
        }
    }
}