# DSTestRunner

Framework for writing and running autotests

## Features

- Easy writing of autotests
- Screenshot comparison
- Support for testing web applications in browsers:
  - Google Chrome and Chromium-based
  - Mozilla Firefox
- Support for mobile application testing:
  - Android
  - iOS
- Support for saving results in formats:
  - Allure Report
  - CSV

## Quick start

### Configuration file example

[See docs/config_example.md](docs/config_example.md)

### List of available actions

[See docs/actions.md](docs/actions.md)

### Run from command line

```shell
./gradlew :<module_name>:run --args="<arguments>"
```

Available arguments (optional):

- -configuration — configuration file name
- -threads — number of simultaneously running tests
- -tests — comma separated list of tests to run

#### Example

```shell
./gradlew :example-web-tests:run --args="-configuration=configuration.json -threads=12 -tests=LoginAndLogoutTest,LoginFormValidationTest"
```

## Examples

See sample tests in the "example-web-tests" directory of the project
