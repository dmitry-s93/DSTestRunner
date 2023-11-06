# Action: setPage

Goes to the specified page

```kotlin
step("<step_id>", setPage("<page_to_go>") {
    addUrlParameter("<param_name>", "<param_value>") // optional
    setUrlArgument("<argument_name>", "<argument_value>") // optional
})
```

### Usage example

```kotlin
// Goes to the specified page
step("01", setPage("LoginPage"))
// Goes to the specified page with URL parameters added
step("02", setPage("LoginPage") {
    addUrlParameter("param1", "value1")
    addUrlParameter("param2", "value2")
})
// For example, the ProductDetailPage in the PageList has the url "http://example.com/product/{productId}"
step("03", setPage("ProductDetailPage") {
    setUrlArgument("productId", "123") // Replaces "{productId}" with "123"
})
```