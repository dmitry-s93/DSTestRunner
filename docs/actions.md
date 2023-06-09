# Available Actions

## Page Actions

### setPage action

Goes to the specified page.

**Usage:**

```kotlin
step("<step_id>", setPage("<page_to_go>") {
    urlParameter("<param_name_1>", "param_value_1") // optional
    urlParameter("<param_name_2>", "param_value_2") // optional
    ...
    urlParameter("<param_name_N>", "param_value_N") // optional
})
```

**For example:**

```kotlin
// Goes to the specified page
step("01", setPage("LoginPage"))
// Goes to the specified page with URL parameters added
step("02", setPage("LoginPage") {
    urlParameter("param1", "value1")
    urlParameter("param2", "value2")
})
```

### switchToWindow action

Switches to the first other window or to the window with the specified page.

**Usage:**

```kotlin
step("<step_id>", switchToWindow("<window_to_switch>"))
```

**For example:**

```kotlin
// Switches to the first other window
step("01", switchToWindow())
// Switches to the window with the specified page 
step("02", switchToWindow("LoginPage"))
```

### closeWindow action

Closes the active window or the window with the specified page.

**Usage:**

```kotlin
step("<step_id>", closeWindow("<window_to_close>"))
```

**For example:**

```kotlin
// Closes the active window
step("01", closeWindow())
// Closes the window with the specified page 
step("02", closeWindow("LoginPage"))
```

## Element Actions

### setSelectValue action

Selects the specified value from the dropdown list.

**Usage:**

```kotlin
step("<step_id>", setSelectValue("<element_name>", "<value_for_selection>"))
```

**For example:**

```kotlin
step("01", setSelectValue("DayOfWeekSelect", "Tuesday"))
```

### isExist action

Checks for the presence of an element. Returns fail if the element is not found on the page.

**Usage:**

```kotlin
step("<step_id>", isExist("<element_to_check>"))
```

**For example:**

```kotlin
step("01", isExist("LoginButton"))
```

### isNotExist action

Checks for the absence of an element. Returns fail if the element is found on the page.

**Usage:**

```kotlin
step("<step_id>", isNotExist("<element_to_check>"))
```

**For example:**

```kotlin
step("01", isNotExist("LoginButton"))
```

## Other actions

### importUser action

Imports the user from the config into the test storage.

**Usage:**

```kotlin
step("<step_id>", importUser("<name_from_config>", "<prefix_to_import>"))
```

**For example:**

In config:

```json
{
  "UserList": {
    "StandardUser": {
      "login": "standard_user",
      "password": "secret_sauce"
    }
  }
}
```

In test:

```kotlin
step("03_before", importUser("StandardUser", "user"))
```

This will add 2 entries to the test storage:

- "user_login" entry with value "standard_user"
- "user_password" entry with value "secret_sauce"

### waitTime action

Waits for the specified time (in milliseconds).

**Usage:**

```kotlin
step("<step_id>", waitTime(millis))
```

**For example:**

```kotlin
step("01", waitTime(2500))
```

### executeSql action

Executes an SQL query on the database.

**Usage:**

```kotlin
step("<step_id>", executeSql("<database_name_from_config>") {
    sql = "SQL Query" // SQL query text
    // or
    sqlFile = "<file_name>.sql" // File with SQL query text (should be in resources/sql/)
    resultAlias = "ResultStorageVar" // Optional. The result of the query execution will be stored in the storage under the specified name.
})
```

**For example:**

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
step("02", executeSql("TestDatabase2") {
    sql = "DELETE FROM documents WHERE user_id = '{UserId}';"
})
```