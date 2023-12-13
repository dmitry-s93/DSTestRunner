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

package storage

import logger.Logger
import test.page.PageData

class PageStorage {
    companion object {
        private var pages = HashMap<String, PageData>()
        private var currentPage = ThreadLocal<PageData>()

        fun putPage(name: String, pageData: PageData) {
            if (pages.containsKey(name)) {
                Logger.warning("A page called '$name' already exists")
                return
            }
            pages[name] = pageData
            Logger.debug("Page '$name' added to page storage")
        }

        fun getPage(name: String): PageData? {
            if (!pages.containsKey(name))
                Logger.warning("Page '$name' is not specified in the page list")
            return pages[name]
        }

        fun setCurrentPage(name: String) {
            currentPage.set(getPage(name))
        }

        fun setCurrentPage(page: PageData) {
            currentPage.set(page)
        }

        fun getCurrentPage(): PageData? {
            return currentPage.get()
        }

        fun isPageExist(name: String): Boolean {
            return pages.containsKey(name)
        }
    }
}