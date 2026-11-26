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
import utils.ResourceUtils

class ScreenshotConfig {
    companion object {
        var takeScreenshotOnError: Boolean = true
            private set
        var saveBaselineIfMissing: Boolean = false
            private set
        var allowableDifference: Int = 20
            private set
        var baselineScreenshotDir: String = ""
            private set
        var actualScreenshotDir: String = ""
            private set
        var waitTimeBeforeScreenshot: Long = 0
            private set
        var executeJavaScriptBeforeScreenshot: String? = null
            private set
        var executeJavaScriptAfterScreenshot: String? = null
            private set
        var expandIgnoredAreasByPixels: Int = 0
            private set

        @Synchronized
        fun load(config: JSONObject) {
            try {
                if (config.has("Screenshot")) {
                    Logger.debug("Reading parameters from config", "ScreenshotConfig")
                    val screenshotConfig = config.getJSONObject("Screenshot")
                    if (screenshotConfig.has("takeScreenshotOnError"))
                        takeScreenshotOnError = screenshotConfig.getBoolean("takeScreenshotOnError")
                    saveBaselineIfMissing = readBooleanParameter(
                        screenshotConfig,
                        "saveBaselineIfMissing",
                        "saveTemplateIfMissing",
                        false
                    )
                    if (screenshotConfig.has("allowableDifference"))
                        allowableDifference = screenshotConfig.getInt("allowableDifference")
                    baselineScreenshotDir = readStringParameter(
                        screenshotConfig,
                        "baselineScreenshotDir",
                        "templateScreenshotDir",
                        ""
                    )
                    actualScreenshotDir = readStringParameter(
                        screenshotConfig,
                        "actualScreenshotDir",
                        "currentScreenshotDir",
                        ""
                    )
                    if (screenshotConfig.has("waitTimeBeforeScreenshot"))
                        waitTimeBeforeScreenshot = screenshotConfig.getLong("waitTimeBeforeScreenshot")
                    if (screenshotConfig.has("executeJavaScriptBeforeScreenshot")) {
                        val jsFile = screenshotConfig.getString("executeJavaScriptBeforeScreenshot")
                        executeJavaScriptBeforeScreenshot = ResourceUtils().getResourceByName(jsFile)
                    }
                    if (screenshotConfig.has("executeJavaScriptAfterScreenshot")) {
                        val jsFile = screenshotConfig.getString("executeJavaScriptAfterScreenshot")
                        executeJavaScriptAfterScreenshot = ResourceUtils().getResourceByName(jsFile)
                    }
                    if (screenshotConfig.has("expandIgnoredAreasByPixels"))
                        expandIgnoredAreasByPixels = screenshotConfig.getInt("expandIgnoredAreasByPixels")
                }
            } catch (e: org.json.JSONException) {
                Logger.error("An error occurred while reading the config", "ScreenshotConfig")
                throw e
            }
        }

        /**
         * Reads a string parameter, preferring [name] and falling back to the deprecated [deprecatedName].
         * The fallback keeps configs written before the parameter was renamed working.
         */
        private fun readStringParameter(
            config: JSONObject,
            name: String,
            deprecatedName: String,
            defaultValue: String
        ): String {
            if (config.has(name))
                return config.getString(name)
            if (config.has(deprecatedName)) {
                warnDeprecatedParameter(deprecatedName, name)
                return config.getString(deprecatedName)
            }
            return defaultValue
        }

        /**
         * Reads a boolean parameter, preferring [name] and falling back to the deprecated [deprecatedName].
         * The fallback keeps configs written before the parameter was renamed working.
         */
        private fun readBooleanParameter(
            config: JSONObject,
            name: String,
            deprecatedName: String,
            defaultValue: Boolean
        ): Boolean {
            if (config.has(name))
                return config.getBoolean(name)
            if (config.has(deprecatedName)) {
                warnDeprecatedParameter(deprecatedName, name)
                return config.getBoolean(deprecatedName)
            }
            return defaultValue
        }

        private fun warnDeprecatedParameter(deprecatedName: String, name: String) {
            Logger.warning("Parameter $deprecatedName is deprecated, use $name", "ScreenshotConfig")
        }
    }
}