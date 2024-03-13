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

import config.WebDriverConfig
import test.element.Locator
import java.awt.Rectangle
import java.net.URI

open class Page(val pageData: PageData)

class PageData(
    val pageName: String,
    val urlPath: String? = null,
    val identifier: Locator? = null,
    val description: String? = null,
    private val elements: HashMap<String, Element>? = null,
    val workArea: Locator? = null,
    val ignoredElements: Set<Locator> = HashSet(),
    val ignoredRectangles: Set<Rectangle> = HashSet(),
    val waitTimeBeforeScreenshot: Long? = null
) {
    fun getUrl(urlArguments: HashMap<String, String>? = null): String {
        if (urlPath != null) {
            var resUrlPath = urlPath
            urlArguments?.forEach {
                resUrlPath = resUrlPath!!.replace("{${it.key}}", it.value)
            }
            val argStartIndex = resUrlPath!!.indexOf("{")
            if (argStartIndex != -1)
                resUrlPath = resUrlPath!!.substring(0, argStartIndex)
            return URI(WebDriverConfig.url).resolve(resUrlPath!!).toString()
        }
        return WebDriverConfig.url
    }

    fun getElement(elementName: String): Element? {
        if (elements != null && elements.containsKey(elementName))
            return elements[elementName]
        return null
    }
}