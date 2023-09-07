package pages.saucedemo

import test.page.PageElements

class LoginPage : PageElements() {
    init {
        element("UsernameEdit", "//input[@id='user-name']")
        element("PasswordEdit", "//input[@id='password']")
        element("LoginButton", "//input[@id='login-button']")

        element("ErrorMessage", "//div[contains(@class,'error-message-container')]/h3")
    }
}