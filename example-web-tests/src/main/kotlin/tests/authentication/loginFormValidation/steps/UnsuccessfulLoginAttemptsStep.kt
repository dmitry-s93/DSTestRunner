package tests.authentication.loginFormValidation.steps

import action.actions.checkElementValue
import action.actions.checkLoadPage
import action.actions.click
import action.actions.setFieldValue
import action.actions.web.setPage
import pages.AuthorizationPageList.Companion.loginPage
import test.TestBuilder
import users.lockedUser
import users.standardUser

fun unsuccessfulLoginAttempts(id: String, stepName: String, test: Any) {
    with(test as TestBuilder) {
        step(id, stepName) {
            steps {
                val userPasswordNotMatchMessage = "Epic sadface: Username and password do not match any user in this service"
                val userLockedMessage = "Epic sadface: Sorry, this user has been locked out."
                step("01", "Incorrect password") {
                    optional()
                    steps {
                        step("01", setPage(loginPage))
                        step("02", checkLoadPage(loginPage))
                        step("03", setFieldValue(loginPage.usernameEdit, standardUser.login))
                        step("04", setFieldValue(loginPage.passwordEdit, "wrong_password"))
                        step("05", click(loginPage.loginButton))
                        step("06", checkLoadPage(loginPage) {
                            name = "Stayed on the login page"
                        })
                        step("07", checkElementValue(loginPage.errorMessage, userPasswordNotMatchMessage))
                    }
                }
                step("02", "Login by blocked user") {
                    optional()
                    steps {
                        step("01", setPage(loginPage))
                        step("02", checkLoadPage(loginPage))
                        step("03", setFieldValue(loginPage.usernameEdit, lockedUser.login))
                        step("04", setFieldValue(loginPage.passwordEdit, lockedUser.password))
                        step("05", click(loginPage.loginButton))
                        step("06", checkLoadPage(loginPage) {
                            name = "Stayed on the login page"
                        })
                        step("07", checkElementValue(loginPage.errorMessage, userLockedMessage))
                    }
                }
            }
        }
    }
}