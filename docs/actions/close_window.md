# Action: closeWindow

Closes the active window or the window with the specified page

**Usage:**

```kotlin
step("<step_id>", closeWindow("<window_to_close>"))
```

## Usage example

```kotlin
// Closes the active window
step("01", closeWindow())
// Closes the window with the specified page 
step("02", closeWindow("LoginPage"))
```