package test.element

enum class LocatorType(val value: String) {
    XPATH("XPath"),
    CSS_SELECTOR("CSS Selector"),
    CLASS_NAME("Class Name"),
    ID("ID"),
    ACCESSIBILITY_ID("Accessibility ID"),
    ANDROID_UI_AUTOMATOR("Android UI Automator")
}

class Locator(value: String, val type: LocatorType? = null) {
    var value = value
        private set
    fun withReplaceArgs(vararg args: Any?):Locator {
        return Locator(String.format(value, *args), type)
    }
}