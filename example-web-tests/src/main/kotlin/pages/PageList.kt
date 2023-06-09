package pages

import pages.saucedemo.InventoryPage
import pages.saucedemo.LoginPage
import test.page.PageBuilder

@Suppress("unused")
class PageList : PageBuilder() {
    init {
        page("LoginPage") {
            urlPath = ""
            identifier = "//input[@id='login-button']"
            description = "Login page"
            elements = LoginPage().getElements()
        }
        group("Products") {
            page("InventoryPage") {
                urlPath = "inventory.html"
                identifier = "//span[@class='title' and text()='Products']"
                description = "Product List Page"
                elements = InventoryPage().getElements()
            }
            page("InventoryItemPage") {
                urlPath = "inventory-item.html"
                identifier = "//button[@id='back-to-products']']"
                description = "Product detail page"
            }
        }
    }
}