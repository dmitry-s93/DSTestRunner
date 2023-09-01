package tests.authentication.loginFormValidation.steps

import action.actions.checkElementValue
import action.actions.checkLoadPage
import action.actions.click
import action.actions.setFieldValue
import action.actions.web.setPage
import test.TestBuilder

fun inputFieldValidation(id: String, stepName: String, test: Any) {
    with(test as TestBuilder) {
        step(id, stepName) {
            steps {
                step("01", "Login and password are empty") {
                    optional()
                    steps {
                        step("01", setPage("LoginPage"))
                        step("02", checkLoadPage("LoginPage"))
                        step("03", click("LoginButton"))
                        step("04", checkElementValue("ErrorMessage", "{UsernameRequiredMessage}"))
                    }
                }
                step("02", "Empty login") {
                    optional()
                    steps {
                        step("01", setPage("LoginPage"))
                        step("02", checkLoadPage("LoginPage"))
                        step("03", setFieldValue("PasswordEdit", "{user_password}"))
                        step("04", click("LoginButton"))
                        step("05", checkElementValue("ErrorMessage", "{UsernameRequiredMessage}"))
                    }
                }
                step("03", "Empty password") {
                    optional()
                    steps {
                        step("01", setPage("LoginPage"))
                        step("02", checkLoadPage("LoginPage"))
                        step("03", setFieldValue("UsernameEdit", "{user_login}"))
                        step("04", click("LoginButton"))
                        step("05", checkElementValue("ErrorMessage", "{PasswordRequiredMessage}"))
                    }
                }
            }
        }
    }
}