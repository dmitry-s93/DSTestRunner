package pages

import pages.saucedemo.InventoryPage
import test.element.Locator
import test.page.PageData

class ProductsPageList {
    companion object {
        val inventoryPage = PageData(
            pageName = "Inventory",
            urlPath = "inventory.html",
            identifier = Locator("//span[@class='title' and text()='Products']"),
            description = "Product List Page",
            elements = InventoryPage().getElements()
        )
        val inventoryItemPage = PageData(
            pageName = "Inventory Item",
            urlPath = "inventory-item.html",
            identifier = Locator("//button[@id='back-to-products']']"),
            description = "Product detail page"
        )
    }
}