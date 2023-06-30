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

package action.actions

import action.Action
import action.ActionData
import action.ActionResult
import action.ActionReturn
import config.DatabaseListConfig
import config.Localization
import storage.ValueStorage
import utils.ResourceUtils
import java.sql.DriverManager


class ExecuteSqlAction(private val databaseName: String) : ActionReturn(), Action {
    var sql: String? = null
    var sqlFile: String? = null
    var resultAlias: String? = null
    private var result: String? = null
    private var updateCount: Int? = null

    override fun getName(): String {
        return Localization.get("ExecuteSqlAction.DefaultName", databaseName)
    }

    override fun execute(): ActionResult {
        try {
            if (sql == null && sqlFile != null)
                sql = ResourceUtils().getResourceByName("sql/$sqlFile")
            if (sql.isNullOrEmpty())
                return broke(Localization.get("ExecuteSqlAction.SqlQueryIsEmpty"))

            sql = ValueStorage.replace(sql!!)

            val database = DatabaseListConfig.getDatabase(databaseName)
                ?: return broke(Localization.get("ExecuteSqlAction.DatabaseNotFoundInConfig", databaseName))

            val connection = DriverManager.getConnection(
                database.getUrl(),
                database.getUsername(),
                database.getPassword()
            )
            val statement = connection.createStatement()
            statement.execute(sql)

            updateCount = statement.updateCount

            if (resultAlias != null) {
                val resultSet = statement.resultSet
                if (resultSet != null && resultSet.next()) {
                    result = resultSet.getString(1)
                    ValueStorage.setValue(resultAlias!!, result.toString())
                } else {
                    return broke(Localization.get("ExecuteSqlAction.SqlQueryReturnedNoResults"))
                }
            }
        } catch (e: Exception) {
            return broke(Localization.get("ExecuteSqlAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["database"] = databaseName
        parameters["sql"] = sql.toString()
        if (result != null)
            parameters["result"] = result.toString()
        if (updateCount != null && updateCount != -1)
            parameters["updateCount"] = updateCount.toString()
        return parameters
    }
}

/**
 * Executes an SQL query on the database [databaseName].
 */
fun executeSql(databaseName: String, function: (ExecuteSqlAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = ExecuteSqlAction(databaseName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}