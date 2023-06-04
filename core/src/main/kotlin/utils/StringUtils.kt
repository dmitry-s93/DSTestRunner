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

package utils

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class StringUtils {
    fun md5sum(input: String): String? {
        val messageDigest: MessageDigest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Unable to compute md5sum for string", e)
        }
        messageDigest.update(input.toByteArray())
        val hash = BigInteger(1, messageDigest.digest())
        return hash.toString(16)
    }
}