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

open class PageBuilder {
    var urlPath: String? = null
    var identifier: String? = null
    var description: String? = null
    var elements: HashMap<String, String>? = null

    fun page(name: String, function: () -> Unit) {
        urlPath = null
        identifier = null
        description = null
        elements = null
        function()
        PageStorage.putPage(name, PageData(name, urlPath, identifier, description, elements))
    }
}