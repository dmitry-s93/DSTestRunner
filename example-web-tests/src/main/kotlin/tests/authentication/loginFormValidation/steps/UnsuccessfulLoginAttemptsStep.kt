package tests.authentication.loginFormValidation.steps

import action.actions.*
import test.TestBuilder

fun unsuccessfulLoginAttempts(id: String, stepName: String, test: Any) {
    with(test as TestBuilder) {
        step(id, stepName) {
            steps {
                step("01", "Incorrect password") {
                    optional()
                    steps {
                        step("01", setPage("LoginPage"))
                        step("02", checkLoadPage("LoginPage"))
                        step("03", setFieldValue("UsernameEdit", "{user_login}"))
                        step("04", setFieldValue("PasswordEdit", "wrong_password"))
                        step("05", click("LoginButton"))
                        step("06", checkLoadPage("LoginPage") {
                            name = "Stayed on the login page"
                        })
                        step("07", checkElementValue("ErrorMessage", "{UserPasswordNotMatchMessage}"))
                    }
                }
                step("02", "Login by blocked user") {
                    optional()
                    steps {
                        step("01", setPage("LoginPage"))
                        step("02", checkLoadPage("LoginPage"))
                        step("03", setFieldValue("UsernameEdit", "{locked_user_login}"))
                        step("04", setFieldValue("PasswordEdit", "{locked_user_password}"))
                        step("05", click("LoginButton"))
                        step("06", checkLoadPage("LoginPage") {
                            name = "Stayed on the login page"
                        })
                        step("07", checkElementValue("ErrorMessage", "{UserLockedMessage}"))
                    }
                }
            }
        }
    }
}