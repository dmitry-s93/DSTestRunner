/* Copyright 2023 DSTestRunner Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.page

import logger.Logger
import test.element.Locator
import test.element.LocatorType

open class Element(
    val locator: Locator,
    val displayName: String,
    val maxSize: Int? = null,
    val allowedChars: String? = null,
    val pattern: String? = null
)

open class PageElements {
    private var elements = HashMap<String, Element>()
    private var parentName: String = ""
    private var parentLocator: String = ""

    fun element(
        name: String,
        locator: String,
        locatorType: LocatorType = LocatorType.XPATH,
        displayName: String = name,
        maxSize: Int? = null,
        allowedChars: String? = null,
        pattern: String? = null,
        function: (() -> Unit)? = null
    ) {
        if (parentName.isEmpty())
            putElement(name, Element(Locator(locator, locatorType), displayName, maxSize, allowedChars, pattern))
        else
            putElement(
                "$parentName.$name",
                Element(Locator(parentLocator + locator, locatorType), displayName, maxSize, allowedChars, pattern)
            )
        if (function != null) {
            if (locatorType != LocatorType.XPATH) {
                Logger.warning("Nested elements are supported only when using XPath", name)
                return
            }
            val currentParentName = parentName
            val currentParentLocator = parentLocator
            if (parentName.isNotEmpty())
                parentName += "."
            parentName += name
            parentLocator += locator
            function()
            parentName = currentParentName
            parentLocator = currentParentLocator
        }
    }

    private fun putElement(name: String, element: Element) {
        if (elements.containsKey(name)) {
            Logger.warning("An element named '$name' already exists")
            return
        }
        elements[name] = element
    }

    fun group(@Suppress("UNUSED_PARAMETER") name: String, function: () -> Unit) {
        function()
    }

    fun getElements(): HashMap<String, Element> {
        return elements
    }
}