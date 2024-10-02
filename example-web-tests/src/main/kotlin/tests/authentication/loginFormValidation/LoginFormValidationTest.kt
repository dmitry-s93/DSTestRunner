package tests.authentication.loginFormValidation

import action.actions.web.closeBrowser
import action.actions.web.openBrowser
import pages.AuthorizationPageList.Companion.loginPage
import reporter.allure.SeverityLevel
import reporter.allure.allure
import test.TestBuilder
import tests.authentication.loginFormValidation.steps.inputFieldValidation
import tests.authentication.loginFormValidation.steps.unsuccessfulLoginAttempts

class LoginFormValidationTest : TestBuilder("TEST_02", "Login Form Validation") {
    init {
        allure {
            epic("Authentication")
            feature("User Login")
            description("Test for checking negative cases at user login")
            severity(SeverityLevel.CRITICAL)
        }
        before {
            step("01_before", openBrowser(loginPage))
        }
        steps {
            inputFieldValidation("01", "Input field validation", this)
            unsuccessfulLoginAttempts("02", "Unsuccessful login attempts", this)
        }
        after {
            step("01_after", closeBrowser())
        }
    }
}