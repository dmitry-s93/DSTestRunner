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

open class PageElements {
    private var elements = HashMap<String, String>()
    private var parentName: String = ""
    private var parentXpath: String = ""

    fun webElement(name: String, xpath: String, function: (() -> Unit)? = null) {
        if (parentName.isEmpty())
            elements[name] = xpath
        else
            elements["$parentName.$name"] = parentXpath + xpath
        if (function != null) {
            val currentParentName = parentName
            val currentParentXpath = parentXpath
            if (parentName.isNotEmpty())
                parentName += "."
            parentName += name
            parentXpath += xpath
            function()
            parentName = currentParentName
            parentXpath = currentParentXpath
        }
    }

    fun group(@Suppress("UNUSED_PARAMETER") name: String, function: () -> Unit) {
        function()
    }

    fun getElements(): HashMap<String, String> {
        return elements
    }
}