# Configuration example

### Sample Configuration for Web Tests

```json
{
  "Main": {
    "name": "WebTest",
    "threads": 5,
    "driverImpl": "driver.web.ChromeDriver",
    "reporterImpl": "reporter.allure.AllureReporter",
    "testSource": "TestList",
    "pageSource": "pages.PageList",
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
    "url": "https://www.example.com/",
    "remoteAddress": "http://localhost:4444/wd/hub",
    "pageLoadTimeout": 20000,
    "elementTimeout": 10000
  },
  "BrowserOptions": [
    "window-size=1280,1024",
    "force-device-scale-factor=1"
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
    "threads": 1,
    "driverImpl": "driver.mobile.AndroidAppiumDriver",
    "reporterImpl": "reporter.allure.AllureReporter",
    "testSource": "TestList",
    "pageSource": "pages.PageList",
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
    "remoteAddress": "http://localhost:4723",
    "pageLoadTimeout": 20000,
    "elementTimeout": 10000,
    "DesiredCapabilities": {
      "appium:deviceName": "Android_13",
      "appium:platformName": "Android",
      "appium:platformVersion": "13.0",
      "appium:udid": "emulator-5554",
      "appium:app": "/home/user/Android/apk/some-app.apk",
      "appium:automationName": "UIAutomator2",
      "appium:fullReset": true,
      "appium:enableMultiWindows": true,
      "appium:newCommandTimeout": 90
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