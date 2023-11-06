# Action: switchToWindow

Switches to the first other window or to the window with the specified page.

```kotlin
step("<step_id>", switchToWindow("<window_to_switch>"))
```

### Usage example

```kotlin
// Switches to the first other window
step("01", switchToWindow())
// Switches to the window with the specified page 
step("02", switchToWindow("LoginPage"))
```