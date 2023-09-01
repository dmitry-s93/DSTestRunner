package test.element

enum class LocatorType(val value: String) {
    XPATH("XPath"),
    CSS_SELECTOR("CSS Selector");
}
class Locator(value: String, val type: LocatorType? = null) {
    var value = value
        private set
    fun withReplaceArgs(vararg args: Any?):Locator {
        return Locator(String.format(value, *args), type)
    }
}