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

package config

import org.json.JSONObject
import logger.Logger
import utils.ResourceUtils

class ScreenshotConfig {
    companion object {
        private var takeScreenshotOnError: Boolean = true
        private var saveTemplateIfMissing: Boolean = false
        private var allowableDifference: Int = 20
        private var templateScreenshotDir: String = ""
        private var currentScreenshotDir: String = ""
        private var isLoaded: Boolean = false

        @Synchronized
        private fun readConfig() {
            if (isLoaded) return
            try {
                Logger.debug("Reading parameters from config", "ScreenshotConfig")
                val config = JSONObject(ResourceUtils().getResourceByName(MainConfig.getConfiguration()))
                if (config.has("Screenshot")) {
                    val screenshotConfig = config.getJSONObject("Screenshot")
                    if (screenshotConfig.has("takeScreenshotOnError"))
                        takeScreenshotOnError = screenshotConfig.getBoolean("takeScreenshotOnError")
                    if (screenshotConfig.has("saveTemplateIfMissing"))
                        saveTemplateIfMissing = screenshotConfig.getBoolean("saveTemplateIfMissing")
                    if (screenshotConfig.has("allowableDifference"))
                        allowableDifference = screenshotConfig.getInt("allowableDifference")
                    if (screenshotConfig.has("templateScreenshotDir"))
                        templateScreenshotDir = screenshotConfig.getString("templateScreenshotDir")
                    if (screenshotConfig.has("currentScreenshotDir"))
                        currentScreenshotDir = screenshotConfig.getString("currentScreenshotDir")
                }
                isLoaded = true
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "ScreenshotConfig")
                throw e
            }
        }

        fun getTakeScreenshotOnError(): Boolean {
            if (!isLoaded) readConfig()
            return takeScreenshotOnError
        }

        fun getSaveTemplateIfMissing(): Boolean {
            if (!isLoaded) readConfig()
            return saveTemplateIfMissing
        }

        fun getTemplateScreenshotDir(): String {
            if (!isLoaded) readConfig()
            return templateScreenshotDir
        }

        fun getCurrentScreenshotDir(): String {
            if (!isLoaded) readConfig()
            return currentScreenshotDir
        }

        fun getAllowableDifference(): Int {
            if (!isLoaded) readConfig()
            return allowableDifference
        }
    }
}