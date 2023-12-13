import pages.saucedemo.InventoryPage
import pages.saucedemo.LoginPage
import test.element.Locator
import test.page.PageData

val authorizationPages = AuthorizationPages()
val productsPages = ProductsPages()

class AuthorizationPages {
    val login = PageData(
        pageName = "Login",
        description = "Login page",
        urlPath = "",
        identifier = Locator("//input[@id='login-button']"),
        elements = LoginPage().getElements()
    )
}

class ProductsPages {
    val inventory = PageData(
        pageName = "Inventory",
        urlPath = "inventory.html",
        identifier = Locator("//span[@class='title' and text()='Products']"),
        description = "Product List Page",
        elements = InventoryPage().getElements()
    )
    val inventoryItem = PageData(
        pageName = "Inventory Item",
        urlPath = "inventory-item.html",
        identifier = Locator("//button[@id='back-to-products']']"),
        description = "Product detail page"
    )
}