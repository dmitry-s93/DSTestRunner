package pages

import pages.saucedemo.LoginPage
import test.element.Locator
import test.page.PageData

class AuthorizationPageList {
    companion object {
        val loginPage = LoginPage(
            PageData(
                pageName = "Login",
                description = "Login page",
                urlPath = "",
                identifier = Locator("//input[@id='login-button']")
            )
        )
    }
}