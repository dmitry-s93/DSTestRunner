package test.element

enum class LocatorType(val value: String) {
    XPATH("XPath"),
    CSS_SELECTOR("CSS Selector"),
    CLASS_NAME("Class Name"),
    ID("ID"),
    ACCESSIBILITY_ID("Accessibility ID"),
    ANDROID_UI_AUTOMATOR("Android UI Automator"),
    IOS_CLASS_CHAIN("iOS Class Chain"),
    IOS_PREDICATE_STRING("iOS Predicate String")
}

class Locator(value: String, val type: LocatorType? = null, val ignoreVisibility: Boolean = false) {
    var value = value
        private set
    fun withReplaceArgs(vararg args: Any?):Locator {
        return Locator(String.format(value, *args), type, ignoreVisibility)
    }
}