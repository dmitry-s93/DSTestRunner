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
import storage.PageStorage

class OpenBrowserAction(private val pageName: String) : ActionReturn(), Action {
    private var pageUrl: String? = null

    override fun getName(): String {
        return Localization.get("OpenBrowserAction.DefaultName", pageName)
    }

    override fun execute(): ActionResult {
        if (!PageStorage.isPageExist(pageName))
            return broke(Localization.get("General.PageIsNotSpecifiedInPageList", pageName))
        PageStorage.setCurrentPage(pageName)
        pageUrl = PageStorage.getPage(pageName)?.getUrl()
        if (pageUrl.isNullOrEmpty())
            return broke(Localization.get("General.PageUrlNotSpecified"))
        try {
            DriverSession.createSession()
            DriverSession.getSession().setPage(pageUrl!!)
        } catch (e: Exception) {
            return broke(Localization.get("OpenBrowserAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["pageName"] = pageName
        parameters["pageUrl"] = pageUrl.toString()
        return parameters
    }
}

fun openBrowser(pageName: String, function: (OpenBrowserAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = OpenBrowserAction(pageName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}