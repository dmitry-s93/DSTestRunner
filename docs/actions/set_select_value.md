# Action: setSelectValue

Selects the specified value from the dropdown list

```kotlin
step("<step_id>", setSelectValue("<element_name>", "<value_for_selection>"))
```

### Usage example

```kotlin
step("01", setSelectValue("DayOfWeekSelect", "Tuesday"))
```