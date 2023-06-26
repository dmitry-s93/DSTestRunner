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

package driver

import java.awt.Point
import java.util.ArrayList

interface Driver {
    fun click(locator: String, points: ArrayList<Point>? = null)
    fun checkLoadPage(url: String, identifier: String?): Boolean
    fun switchToWindow(url: String?): Boolean
    fun closeWindow(url: String?): Boolean
    fun getCurrentUrl(): String
    fun getElementValue(locator: String): String
    fun getScreenshot(): ByteArray
    fun setPage(url: String)
    fun setValue(locator: String, value: String, sequenceMode: Boolean = false)
    fun isExist(locator: String): Boolean
    fun isNotExist(locator: String): Boolean
    fun quit()
}