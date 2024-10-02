package tests.authentication.loginAndLogout

import action.actions.checkLoadPage
import action.actions.click
import action.actions.setFieldValue
import action.actions.web.closeBrowser
import action.actions.web.openBrowser
import pages.AuthorizationPageList.Companion.loginPage
import pages.ProductsPageList.Companion.inventoryPage
import reporter.allure.SeverityLevel
import reporter.allure.allure
import test.TestBuilder
import users.standardUser

class LoginAndLogoutTest : TestBuilder("TEST_01", "Login and logout") {
    init {
        allure {
            epic("Authentication")
            feature("User Login")
            description("User Login/Logout Direct Test")
            severity(SeverityLevel.BLOCKER)
        }
        before {
            step("01_before", openBrowser(loginPage))
            step("02_before", checkLoadPage(loginPage))
        }
        steps {
            step("01", setFieldValue(loginPage.usernameEdit, standardUser.login))
            step("02", setFieldValue(loginPage.passwordEdit, standardUser.password))
            step("03", click(loginPage.loginButton))
            step("04", checkLoadPage(inventoryPage))
            step("05", click(inventoryPage.burgerMenuButton))
            step("06", click(inventoryPage.logoutLink))
            step("07", checkLoadPage(loginPage))
        }
        after {
            step("01_after", closeBrowser())
        }
    }
}