package tests.authentication.loginAndLogout

import action.actions.*
import reporter.allure.SeverityLevel
import reporter.allure.allure
import test.TestBuilder

class LoginAndLogoutTest : TestBuilder("TEST_01", "Login and logout") {
    init {
        allure {
            epic("Authentication")
            feature("User Login")
            description("User Login/Logout Direct Test")
            severity(SeverityLevel.BLOCKER)
        }
        before {
            step("01_before", openBrowser("LoginPage"))
            step("02_before", checkLoadPage("LoginPage"))
            step("03_before", importUser("StandardUser", "user"))
        }
        steps {
            step("01", setFieldValue("UsernameEdit", "{user_login}"))
            step("02", setFieldValue("PasswordEdit", "{user_password}"))
            step("03", click("LoginButton"))
            step("04", checkLoadPage("InventoryPage"))
            step("05", click("BurgerMenuButton"))
            step("06", click("LogoutLink"))
            step("07", checkLoadPage("LoginPage"))
        }
        after {
            step("01_after", closeBrowser())
        }
    }
}