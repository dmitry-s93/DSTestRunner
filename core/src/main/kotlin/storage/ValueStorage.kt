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

class ValueStorage {
    companion object {
        private val storedValues = ThreadLocal<HashMap<String, String>>()

        fun setValue(name: String, value: String) {
            var storedValue = storedValues.get()
            if (storedValue == null)
                storedValue = HashMap()
            storedValue[name] = value
            storedValues.set(storedValue)
        }

        fun getValue(name: String): String? {
            if (storedValues.get() != null)
                return storedValues.get()[name]
            return null
        }

        fun replace(value: String): String {
            var result = value
            storedValues.get()?.forEach {
                result = result.replace("{${it.key}}", it.value)
            }
            return result
        }

        fun clear() {
            storedValues.remove()
        }
    }
}