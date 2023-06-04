package tests

import action.actions.*
import reporter.impl.allure
import test.TestBuilder

class LoginAndLogoutTest : TestBuilder("TEST_01", "Login and logout") {
    init {
        allure {
            epic("Authentication")
            feature("User Login")
            description("User Login/Logout Direct Test")
        }
        before {
            step("01_before", openBrowser("LoginPage"))
            step("02_before", checkLoadPage("LoginPage"))
            step("03_before", setValueToStorage("StandardUserConst", "standard_user"))
            step("04_before", setValueToStorage("PasswordConst", "secret_sauce"))
        }
        steps {
            step("01", setFieldValue("UsernameEdit", "{StandardUserConst}"))
            step("02", setFieldValue("PasswordEdit", "{PasswordConst}"))
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