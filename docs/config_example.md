# Configuration example

### Sample Configuration for Web Tests

```json
{
  "Main": {
    "name": "WebTest",
    "threads": 5,
    "driverImpl": "driver.web.WebDriver",
    "reporterImpl": "reporter.allure.AllureReporter",
    "testSource": "TestList",
    "consoleLogLevel": "DEBUG"
  },
  "Screenshot": {
    "takeScreenshotOnError": true,
    "saveTemplateIfMissing": true,
    "allowableDifference": 20,
    "waitTimeBeforeScreenshot": 0,
    "executeJavaScriptBeforeScreenshot": "js/before_screenshot.js",
    "executeJavaScriptAfterScreenshot": "js/after_screenshot.js",
    "templateScreenshotDir": "/home/user/Screenshots/Web/template",
    "currentScreenshotDir": "/home/user/Screenshots/Web/current"
  },
  "WebDriver": {
    "browser": "Chrome",
    "url": "https://www.example.com/",
    "remoteAddress": "http://localhost:4444/wd/hub",
    "pageLoadTimeout": 20000,
    "elementTimeout": 10000
  },
  "ChromeOptions": [
    "window-size=1280,1024",
    "force-device-scale-factor=1"
  ],
  "FirefoxOptions": [
    "--width=1280",
    "--height=1024"
  ],
  "PreloaderElements": [
    "//div[contains(@class,'example-preloader')]"
  ],
  "DatabaseList": {
    "TestDatabase1": {
      "url": "jdbc:postgresql://db_host_1:5432/db_1",
      "username": "db_user_1",
      "password": "db_password_1",
      "description": "Test database 1"
    },
    "TestDatabase2": {
      "url": "jdbc:postgresql://db_host_2:5432/db_2",
      "username": "db_user_2",
      "password": "db_password_2",
      "description": "Test database 2"
    }
  },
  "UserList": {
    "User1": {
      "login": "user_login_1",
      "password": "user_password_1"
    },
    "User2": {
      "login": "user_login_2",
      "password": "user_password_2"
    }
  }
}
```

### Sample Configuration for Android Tests

```json
{
  "Main": {
    "name": "AndroidTest",
    "threads": 2,
    "driverImpl": "driver.mobile.AndroidAppiumDriver",
    "reporterImpl": "reporter.allure.AllureReporter",
    "testSource": "TestList",
    "consoleLogLevel": "DEBUG"
  },
  "Screenshot": {
    "takeScreenshotOnError": true,
    "saveTemplateIfMissing": true,
    "allowableDifference": 20,
    "waitTimeBeforeScreenshot": 0,
    "templateScreenshotDir": "/home/user/Screenshots/Android/template",
    "currentScreenshotDir": "/home/user/Screenshots/Android/current"
  },
  "AppiumDriver": {
    "pageLoadTimeout": 20000,
    "elementTimeout": 10000,
    "DesiredCapabilities": {
      "platformName": "ANDROID",
      "appium:platformVersion": "13.0",
      "appium:app": "/home/user/Android/apk/some-app.apk",
      "appium:automationName": "UIAutomator2",
      "appium:fullReset": true,
      "appium:enableMultiWindows": true,
      "appium:newCommandTimeout": 90
    },
    "devices": {
      "AndroidEmulator1": {
        "remoteAddress": "http://localhost:4723",
        "DesiredCapabilities": {
          "appium:deviceName": "Android_13_1",
          "appium:udid": "emulator-5554",
          "appium:systemPort": 8200
        }
      },
      "AndroidEmulator2": {
        "remoteAddress": "http://localhost:4724",
        "DesiredCapabilities": {
          "appium:deviceName": "Android_13_2",
          "appium:udid": "emulator-5556",
          "appium:systemPort": 8201
        }
      }
    }
  },
  "DatabaseList": {
    "TestDatabase1": {
      "url": "jdbc:postgresql://db_host_1:5432/db_1",
      "username": "db_user_1",
      "password": "db_password_1",
      "description": "Test database 1"
    },
    "TestDatabase2": {
      "url": "jdbc:postgresql://db_host_2:5432/db_2",
      "username": "db_user_2",
      "password": "db_password_2",
      "description": "Test database 2"
    }
  },
  "UserList": {
    "User1": {
      "login": "user_login_1",
      "password": "user_password_1"
    },
    "User2": {
      "login": "user_login_2",
      "password": "user_password_2"
    }
  }
}
```

### Sample Configuration for iOS Tests

```json
{
  "Main": {
    "name": "iOSTest",
    "threads": 2,
    "driverImpl": "driver.mobile.IOSAppiumDriver",
    "reporterImpl": "reporter.allure.AllureReporter",
    "testSource": "TestList",
    "consoleLogLevel": "DEBUG"
  },
  "Screenshot": {
    "takeScreenshotOnError": true,
    "saveTemplateIfMissing": true,
    "allowableDifference": 20,
    "waitTimeBeforeScreenshot": 0,
    "templateScreenshotDir": "/home/user/Screenshots/iOS/template",
    "currentScreenshotDir": "/home/user/Screenshots/iOS/current"
  },
  "AppiumDriver": {
    "pageLoadTimeout": 20000,
    "elementTimeout": 10000,
    "DesiredCapabilities": {
      "platformName": "iOS",
      "appium:platformVersion": "17.2",
      "appium:app": "/Users/user/iOS/SomeApplication.app",
      "appium:automationName": "XCUITest",
      "appium:fullReset": false,
      "appium:newCommandTimeout": 90
    },
    "devices": {
      "Simulator1": {
        "remoteAddress": "http://localhost:4723",
        "DesiredCapabilities": {
          "appium:deviceName": "iPhone 15 (1)",
          "appium:udid": "3e59b38f-578e-43ab-9c6d-f5758fbfc672"
        }
      },
      "Simulator2": {
        "remoteAddress": "http://localhost:4724",
        "DesiredCapabilities": {
          "appium:deviceName": "iPhone 15 (2)",
          "appium:udid": "af6aad40-b2e7-442c-b707-be3950d633d6"
        }
      }
    }
  },
  "DatabaseList": {
    "TestDatabase1": {
      "url": "jdbc:postgresql://db_host_1:5432/db_1",
      "username": "db_user_1",
      "password": "db_password_1",
      "description": "Test database 1"
    },
    "TestDatabase2": {
      "url": "jdbc:postgresql://db_host_2:5432/db_2",
      "username": "db_user_2",
      "password": "db_password_2",
      "description": "Test database 2"
    }
  },
  "UserList": {
    "User1": {
      "login": "user_login_1",
      "password": "user_password_1"
    },
    "User2": {
      "login": "user_login_2",
      "password": "user_password_2"
    }
  }
}
```