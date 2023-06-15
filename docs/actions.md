# Available Actions

---

## setPage

Goes to the specified page.

### Usage

```kotlin
step("<step_id>", setPage("<page_to_go>") {
    urlParameter("<param_name_1>", "param_value_1") // optional
    urlParameter("<param_name_2>", "param_value_2") // optional
    ...
    urlParameter("<param_name_N>", "param_value_N") // optional
})
```

### For example

```kotlin
// Goes to the specified page
step("01", setPage("LoginPage"))
// Goes to the specified page with URL parameters added
step("02", setPage("LoginPage") {
    urlParameter("param1", "value1")
    urlParameter("param2", "value2")
})
```

---

## switchToWindow

Switches to the first other window or to the window with the specified page.

### Usage

```kotlin
step("<step_id>", switchToWindow("<window_to_switch>"))
```

### For example

```kotlin
// Switches to the first other window
step("01", switchToWindow())
// Switches to the window with the specified page 
step("02", switchToWindow("LoginPage"))
```

---

## closeWindow

Closes the active window or the window with the specified page.

### Usage

```kotlin
step("<step_id>", closeWindow("<window_to_close>"))
```

### For example

```kotlin
// Closes the active window
step("01", closeWindow())
// Closes the window with the specified page 
step("02", closeWindow("LoginPage"))
```

---

## importUser

Imports the user from the config into the test storage.

### Usage

```kotlin
step("<step_id>", importUser("<name_from_config>", "<prefix_to_import>"))
```

### For example

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