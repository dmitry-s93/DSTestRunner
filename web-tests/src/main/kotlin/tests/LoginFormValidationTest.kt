package tests

import action.actions.*
import reporter.impl.allure
import test.TestBuilder

class LoginFormValidationTest : TestBuilder("TEST_02", "Login Form Validation") {
    init {
        allure {
            epic("Authentication")
            feature("User Login")
            description("Test for checking negative cases at user login")
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
            step("01", "Input field validation") {
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
            step("02", "Unsuccessful login attempts") {
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
        after {
            step("01_after", closeBrowser())
        }
    }
}