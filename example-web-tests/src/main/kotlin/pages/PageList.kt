package pages

import pages.saucedemo.InventoryPage
import pages.saucedemo.LoginPage
import test.element.Locator
import test.page.PageBuilder

@Suppress("unused")
class PageList : PageBuilder() {
    init {
        page("LoginPage") {
            urlPath = ""
            identifier = Locator("//input[@id='login-button']")
            description = "Login page"
            elements = LoginPage().getElements()
        }
        group("Products") {
            page("InventoryPage") {
                urlPath = "inventory.html"
                identifier = Locator("//span[@class='title' and text()='Products']")
                description = "Product List Page"
                elements = InventoryPage().getElements()
                ignoredElements = setOf(
                    Locator("//span[@class='title' and text()='Products']"),
                    Locator("//span[@class='title' and text()='Products']")
                )
            }
            page("InventoryItemPage") {
                urlPath = "inventory-item.html"
                identifier = Locator("//button[@id='back-to-products']']")
                description = "Product detail page"
            }
        }
    }
}