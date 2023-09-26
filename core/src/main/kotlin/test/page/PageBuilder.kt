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

import storage.PageStorage
import test.element.Locator

open class PageBuilder {
    var urlPath: String? = null
    var identifier: Locator? = null
    var description: String? = null
    var elements: HashMap<String, Element>? = null
    var workArea: Locator? = null
    var ignoredElements: Set<Locator> = HashSet()
    var waitTime: Long? = null

    fun page(name: String, function: () -> Unit) {
        urlPath = null
        identifier = null
        description = null
        elements = null
        workArea = null
        ignoredElements = HashSet()
        waitTime = null
        function()
        PageStorage.putPage(name, PageData(name, urlPath, identifier, description, elements, workArea, ignoredElements, waitTime))
    }

    fun group(name: String, function: () -> Unit) {
        function()
    }
}