# Action: isNotExist

Checks for the absence of an element. Returns pass if the element is not found on the page.

```kotlin
step("<step_id>", isNotExist("<element_to_check>"))
```

### Usage example

```kotlin
step("01", isNotExist("LoginButton"))
```