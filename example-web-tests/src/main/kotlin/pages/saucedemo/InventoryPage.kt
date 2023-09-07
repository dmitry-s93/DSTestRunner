package pages.saucedemo

import test.page.PageElements

class InventoryPage : PageElements() {
    init {
        element("BurgerMenuButton", "//button[@id='react-burger-menu-btn']")
        element("LogoutLink", "//a[@id='logout_sidebar_link']")
    }
}