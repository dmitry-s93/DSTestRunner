package tests.authentication.loginFormValidation.steps

import action.actions.checkElementValue
import action.actions.checkLoadPage
import action.actions.click
import action.actions.setFieldValue
import action.actions.web.setPage
import pages.AuthorizationPageList.Companion.loginPage
import test.TestBuilder
import users.standardUser

fun inputFieldValidation(id: String, stepName: String, test: Any) {
    with(test as TestBuilder) {
        step(id, stepName) {
            steps {
                val usernameRequiredMessage = "Epic sadface: Username is required"
                val passwordRequiredMessage = "Epic sadface: Password is required"
                step("01", "Login and password are empty") {
                    optional()
                    steps {
                        step("01", setPage(loginPage))
                        step("02", checkLoadPage(loginPage))
                        step("03", click(loginPage.loginButton))
                        step("04", checkElementValue(loginPage.errorMessage, usernameRequiredMessage))
                    }
                }
                step("02", "Empty login") {
                    optional()
                    steps {
                        step("01", setPage(loginPage))
                        step("02", checkLoadPage(loginPage))
                        step("03", setFieldValue(loginPage.passwordEdit, standardUser.password))
                        step("04", click(loginPage.loginButton))
                        step("05", checkElementValue(loginPage.errorMessage, usernameRequiredMessage))
                    }
                }
                step("03", "Empty password") {
                    optional()
                    steps {
                        step("01", setPage(loginPage))
                        step("02", checkLoadPage(loginPage))
                        step("03", setFieldValue(loginPage.usernameEdit, standardUser.login))
                        step("04", click(loginPage.loginButton))
                        step("05", checkElementValue(loginPage.errorMessage, passwordRequiredMessage))
                    }
                }
            }
        }
    }
}