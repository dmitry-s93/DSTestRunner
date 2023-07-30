# Configuration example

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
    "templateScreenshotDir": "/home/user/Screenshots/template",
    "currentScreenshotDir": "/home/user/Screenshots/current"
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