package pages.saucedemo

import test.page.PageElements

class LoginPage : PageElements() {
    init {
        webElement("UsernameEdit", "//input[@id='user-name']")
        webElement("PasswordEdit", "//input[@id='password']")
        webElement("LoginButton", "//input[@id='login-button']")

        webElement("ErrorMessage", "//div[contains(@class,'error-message-container')]/h3")
    }
}