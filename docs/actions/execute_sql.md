# Action: executeSql

Executes an SQL query on the database

```kotlin
step("<step_id>", executeSql("<database_name_from_config>") {
    sql = "SQL Query" // SQL query text
    // or
    sqlFile = "<file_name>.sql" // File with SQL query text (should be in resources/sql/)
    resultAlias = "ResultStorageVar" // Optional. The result of the query execution will be stored in the storage under the specified name.
})
```

### Usage example

In config:

```json
{
  "DatabaseList": {
    "TestDatabase1": {
      "url": "jdbc:postgresql://172.0.0.1:5432/test_db_1",
      "username": "test_user",
      "password": "test_password",
      "description": "Test database 1"
    },
    "TestDatabase2": {
      "url": "jdbc:postgresql://172.0.0.1:5432/test_db_2",
      "username": "test_user",
      "password": "test_password",
      "description": "Test database 2"
    }
  }
}
```

In test:

```kotlin
step("01", executeSql("TestDatabase1") {
    sql = "SELECT id FROM user WHERE first_name = 'Alex';"
    resultAlias = "UserId"
})
// You can specify the text of the sql query directly in the test step
step("02", executeSql("TestDatabase2") {
    sql = "DELETE FROM documents WHERE user_id = '{UserId}';"
})
// Or you can create a sql file in resources/sql directory and specify its name
step("02", executeSql("TestDatabase2") {
    sqlFile = "delete_doc_by_user_id.sql"
    sqlFileParameter("user_id", "{UserId}")
})
```