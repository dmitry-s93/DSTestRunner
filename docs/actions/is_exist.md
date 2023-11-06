# Action: isExist

Checks for the presence of an element. Returns fail if the element is not found on the page

```kotlin
step("<step_id>", isExist("<element_to_check>"))
```

### Usage example

```kotlin
step("01", isExist("LoginButton"))
```