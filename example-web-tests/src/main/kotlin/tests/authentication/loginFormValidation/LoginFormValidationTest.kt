package tests.authentication.loginFormValidation

import action.actions.*
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
            step("01_before", openBrowser("LoginPage"))

            step("02_before", importUser("StandardUser", "user"))
            step("03_before", importUser("LockedUser", "locked_user"))

            step("04_before", setValueToStorage("UsernameRequiredMessage", "Epic sadface: Username is required"))
            step("05_before", setValueToStorage("PasswordRequiredMessage", "Epic sadface: Password is required"))
            step("06_before", setValueToStorage("UserPasswordNotMatchMessage", "Epic sadface: Username and password do not match any user in this service"))
            step("07_before", setValueToStorage("UserLockedMessage", "Epic sadface: Sorry, this user has been locked out."))
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