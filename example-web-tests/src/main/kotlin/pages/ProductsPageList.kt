package pages

import pages.saucedemo.InventoryItemPage
import pages.saucedemo.InventoryPage
import test.element.Locator
import test.page.PageData

class ProductsPageList {
    companion object {
        val inventoryPage = InventoryPage(
            PageData(
                pageName = "Inventory",
                urlPath = "inventory.html",
                identifier = Locator("//span[@class='title' and text()='Products']"),
                description = "Product List Page"
            )
        )
        val inventoryItemPage = InventoryItemPage(
            PageData(
                pageName = "Inventory Item",
                urlPath = "inventory-item.html",
                identifier = Locator("//button[@id='back-to-products']']"),
                description = "Product detail page"
            )
        )
    }
}