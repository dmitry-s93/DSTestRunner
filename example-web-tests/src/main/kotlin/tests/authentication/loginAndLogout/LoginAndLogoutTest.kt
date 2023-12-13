package tests.authentication.loginAndLogout

import action.actions.checkLoadPage
import action.actions.click
import action.actions.importUser
import action.actions.setFieldValue
import action.actions.web.closeBrowser
import action.actions.web.openBrowser
import authorizationPages
import productsPages
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
            step("01_before", openBrowser(authorizationPages.login))
            step("02_before", checkLoadPage(authorizationPages.login))
            step("03_before", importUser("StandardUser", "user"))
        }
        steps {
            step("01", setFieldValue("UsernameEdit", "{user_login}"))
            step("02", setFieldValue("PasswordEdit", "{user_password}"))
            step("03", click("LoginButton"))
            step("04", checkLoadPage(productsPages.inventory))
            step("05", click("BurgerMenuButton"))
            step("06", click("LogoutLink"))
            step("07", checkLoadPage(authorizationPages.login))
        }
        after {
            step("01_after", closeBrowser())
        }
    }
}