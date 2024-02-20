package pages.saucedemo

import test.element.Locator
import test.page.Element
import test.page.Page
import test.page.PageData

class LoginPage(pageData: PageData) : Page(pageData) {
    val usernameEdit = Element(Locator("//input[@id='user-name']"), displayName = "Username")
    val passwordEdit = Element(Locator("//input[@id='password']"), displayName = "Password")
    val loginButton = Element(Locator("//input[@id='login-button']"), displayName = "Login")

    val errorMessage = Element(Locator("//div[contains(@class,'error-message-container')]/h3"), displayName = "Error message")
}