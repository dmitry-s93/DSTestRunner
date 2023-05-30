package pages

import pages.saucedemo.InventoryPage
import pages.saucedemo.LoginPage
import test.page.PageBuilder

class PageList : PageBuilder() {
    init {
        page("LoginPage") {
            urlPath = ""
            identifier = "//input[@id='login-button']"
            description = ""
            elements = LoginPage().getElements()
        }
        page("InventoryPage") {
            urlPath = "inventory.html"
            identifier = "//span[@class='title' and text()='Products']"
            description = ""
            elements = InventoryPage().getElements()
        }
        page("InventoryItemPage") {
            urlPath = "inventory-item.html"
            identifier = "//button[@id='back-to-products']']"
            description = ""
        }
    }
}