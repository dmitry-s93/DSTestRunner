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

package action.actions.web

import action.Action
import action.ActionData
import action.ActionResult
import action.ActionReturn
import config.Localization
import driver.DriverSession
import storage.ValueStorage
import java.util.regex.Matcher
import java.util.regex.Pattern

class GetUrlValueAction(private val alias: String) : ActionReturn(), Action {
    private var url: String? = null
    private var value: String? = null
    @SuppressWarnings("WeakerAccess")
    var pattern: String? = null

    override fun getName(): String {
        return Localization.get("GetUrlValueAction.DefaultName")
    }

    override fun execute(): ActionResult {
        try {
            url = DriverSession.getSession().getCurrentUrl()
            value =
                if (pattern != null) {
                    val pattern: Pattern = Pattern.compile(pattern!!)
                    val matcher: Matcher = pattern.matcher(url!!)
                    if (matcher.find()) {
                        if (matcher.groupCount()>0)
                            matcher.group(1)
                        else
                            url!!.substring(matcher.start(), matcher.end())
                    } else {
                        return broke(Localization.get("GetUrlValueAction.NoMatchFound", pattern))
                    }
                } else {
                    url
                }
            ValueStorage.setValue(alias, value!!)
        } catch (e: Exception) {
            return broke(Localization.get("GetUrlValueAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["url"] = url.toString()
        parameters["alias"] = alias
        if (pattern != null)
            parameters["pattern"] = pattern.toString()
        if (value != url)
            parameters["value"] = value.toString()
        return parameters
    }
}

/**
 * Gets the current URL and saves as [alias]
 */
fun getUrlValue(alias: String, function: (GetUrlValueAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = GetUrlValueAction(alias)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}