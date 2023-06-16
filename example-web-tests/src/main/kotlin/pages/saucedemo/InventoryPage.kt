package pages.saucedemo

import test.page.PageElements

class InventoryPage : PageElements() {
    init {
        webElement("BurgerMenuButton", "//button[@id='react-burger-menu-btn']")
        webElement("LogoutLink", "//a[@id='logout_sidebar_link']")
    }
}