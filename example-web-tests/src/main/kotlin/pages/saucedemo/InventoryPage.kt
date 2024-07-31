package pages.saucedemo

import test.element.Locator
import test.page.Element
import test.page.Page
import test.page.PageData

class InventoryPage(pageData: PageData) : Page(pageData) {
    val burgerMenuButton = Element(Locator("//button[@id='react-burger-menu-btn']"), displayName = "Menu")
    val logoutLink = Element(Locator("//a[@id='logout_sidebar_link']"), displayName = "Logout")
}