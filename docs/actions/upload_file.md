# Action: uploadFile

Uploads a file in the file upload field

```kotlin
step("<step_id>", uploadFile("<file_field>", "<file_path>"))
```

### Usage example

```kotlin
step("01", uploadFile("UploadImageInput", "/home/user/img/flower.png"))
```