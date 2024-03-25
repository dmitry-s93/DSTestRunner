# Available Actions

| Action                                        | Description                                                                                                        | Web | Android | iOS |
|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------|:---:|:-------:|:---:|
| openBrowser                                   | Starts the driver session and opens the browser                                                                    |  ✅  |    ❌    |  ❌  |
| closeBrowser                                  | Closes the browser and ends the driver session                                                                     |  ✅  |    ❌    |  ❌  |
| [openApp](actions/open_app.md)                | Starts the driver session and opens the mobile application                                                         |  ❌  |    ✅    |  ✅  |
| [closeApp](actions/close_app.md)              | Closes the mobile app and ends the driver session                                                                  |  ❌  |    ✅    |  ✅  |
| installApp                                    | Installs the application                                                                                           |  ❌  |    ✅    |  ✅  |
| activateApp                                   | Activates the application                                                                                          |  ❌  |    ✅    |  ✅  |
| terminateApp                                  | Terminates the application                                                                                         |  ❌  |    ✅    |  ✅  |
| removeApp                                     | Removes the application                                                                                            |  ❌  |    ✅    |  ✅  |
| isAppInstalled                                | Checks if the application is installed                                                                             |  ❌  |    ✅    |  ✅  |
| [setPage](actions/set_page.md)                | Goes to the specified page                                                                                         |  ✅  |    ❌    |  ❌  |
| [importUser](actions/import_user.md)          | Imports the user from the config into the test storage                                                             |  ✅  |    ✅    |  ✅  |
| setValueToStorage                             | Adds a value to the test storage                                                                                   |  ✅  |    ✅    |  ✅  |
| [switchToWindow](actions/switch_to_window.md) | Switches to the first other window or to the window with the specified page                                        |  ✅  |    ❌    |  ❌  |
| [closeWindow](actions/close_window.md)        | Closes the active window or the window with the specified page                                                     |  ✅  |    ❌    |  ❌  |
| checkLoadPage                                 | Checks that the specified page has loaded and takes a screenshot (if required)                                     |  ✅  |    ✅    |  ✅  |
| getUrlValue                                   | Gets the URL of the current page                                                                                   |  ✅  |    ❌    |  ❌  |
| click                                         | Clicks on an element                                                                                               |  ✅  |    ✅    |  ✅  |
| setFieldValue                                 | Enters a value into an input field                                                                                 |  ✅  |    ✅    |  ✅  |
| [setSelectValue](actions/set_select_value.md) | Selects the specified value from the dropdown list                                                                 |  ✅  |    ❌    |  ❌  |
| checkElementValue                             | Checks that the element's value matches the expected value                                                         |  ✅  |    ✅    |  ✅  |
| checkInputField                               | Checks the input field for compliance with certain parameters (maximum size, allowed characters, pattern matching) |  ✅  |    ✅    |  ✅  |
| navigateBack                                  | Goes back                                                                                                          |  ✅  |    ✅    |  ❌  |
| [isExist](actions/is_exist.md)                | Checks for the presence of an element. Returns fail if the element is not found on the page.                       |  ✅  |    ✅    |  ✅  |
| [isNotExist](actions/is_not_exist.md)         | Checks for the absence of an element. Returns pass if the element is not found on the page.                        |  ✅  |    ✅    |  ✅  |
| isEnabled                                     | Checks that the element is enabled                                                                                 |  ✅  |    ✅    |  ✅  |
| isDisabled                                    | Checks that the element is disabled                                                                                |  ✅  |    ✅    |  ✅  |
| hoverOverElement                              | Moves the cursor over an element                                                                                   |  ✅  |    ❌    |  ❌  |
| [waitTime](actions/wait_time.md)              | Waits for the specified time (in milliseconds)                                                                     |  ✅  |    ✅    |  ✅  |
| [uploadFile](actions/upload_file.md)          | Uploads a file in the file upload field                                                                            |  ✅  |    ❌    |  ❌  |
| [executeSql](actions/execute_sql.md)          | Executes an SQL query on the database                                                                              |  ✅  |    ✅    |  ✅  |
| swipeElement                                  | Swipes an element in the specified direction                                                                       |  ❌  |    ✅    |  ✅  |
| setLocation                                   | Sets the location of the mobile device (latitude and longitude)                                                    |  ❌  |    ✅    |  ✅  |
| checkLocation                                 | Checks the current location of the mobile device (latitude and longitude)                                          |  ❌  |    ✅    |  ✅  |
| checkAlertText                                | Checks the alert text                                                                                              |  ❌  |    ✅    |  ✅  |
| acceptAlert                                   | Accepts an alert                                                                                                   |  ❌  |    ✅    |  ✅  |
| dismissAlert                                  | Dismisses an alert                                                                                                 |  ❌  |    ✅    |  ✅  |