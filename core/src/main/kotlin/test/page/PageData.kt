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
import logger.Logger
import java.net.URI

class PageData(
    private val pageName: String,
    private val urlPath: String?,
    private val identifier: String?,
    private val description: String?,
    private val elements: HashMap<String, WebElement>?
) {
    fun getPageName(): String {
        return pageName
    }

    fun getUrl(urlArguments: HashMap<String, String>? = null): String {
        if (urlPath != null) {
            var resUrlPath = urlPath
            urlArguments?.forEach {
                resUrlPath = resUrlPath!!.replace("{${it.key}}", it.value)
            }
            val argStartIndex = resUrlPath!!.indexOf("{")
            if (argStartIndex != -1)
                resUrlPath = resUrlPath!!.substring(0, argStartIndex)
            return URI(WebDriverConfig.getUrl()).resolve(resUrlPath!!).toString()
        }
        return WebDriverConfig.getUrl()
    }

    fun getUrlPath(): String? {
        return urlPath
    }

    fun getIdentifier(): String? {
        return identifier
    }

    fun getDescription(): String? {
        return description
    }

    fun getElement(elementName: String): WebElement? {
        if (elements != null && isElementExist(elementName))
            return elements[elementName]
        Logger.warning("Element '$elementName' is not set in '$pageName' page")
        return null
    }

    fun isElementExist(elementName: String): Boolean {
        return elements?.containsKey(elementName) ?: false
    }
}