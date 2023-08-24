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

import logger.Logger
import org.json.JSONObject

class ScreenshotConfig {
    companion object {
        var takeScreenshotOnError: Boolean = true
            private set
        var saveTemplateIfMissing: Boolean = false
            private set
        var allowableDifference: Int = 20
            private set
        var templateScreenshotDir: String = ""
            private set
        var currentScreenshotDir: String = ""
            private set
        var waitTimeBeforeTakingScreenshot: Long = 0
            private set

        @Synchronized
        fun load(config: JSONObject) {
            try {
                if (config.has("Screenshot")) {
                    Logger.debug("Reading parameters from config", "ScreenshotConfig")
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
                    if (screenshotConfig.has("waitTimeBeforeTakingScreenshot"))
                        waitTimeBeforeTakingScreenshot = screenshotConfig.getLong("waitTimeBeforeTakingScreenshot")
                }
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "ScreenshotConfig")
                throw e
            }
        }
    }
}