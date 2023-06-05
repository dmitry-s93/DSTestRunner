# Available Actions

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