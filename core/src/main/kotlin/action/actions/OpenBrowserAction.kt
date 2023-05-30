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

package action.actions

import action.Action
import action.ActionData
import action.ActionResult
import action.ActionReturn
import config.Localization
import driver.DriverSession
import storage.PageStorage

class OpenBrowserAction(pageName: String) : ActionReturn(), Action {
    private val pageName: String
    private val pageUrl: String?

    init {
        this.pageName = pageName
        this.pageUrl = PageStorage.getPage(pageName)?.getUrl()
    }

    override fun getName(): String {
        return Localization.get("OpenBrowserAction.DefaultName", pageName)
    }

    override fun execute(): ActionResult {
        if (pageUrl.isNullOrEmpty())
            return fail(Localization.get("OpenBrowserAction.PageUrlNotSpecified"))
        try {
            DriverSession.createSession()
            DriverSession.getSession().setPage(pageUrl)
        } catch (e: Exception) {
            return fail(Localization.get("OpenBrowserAction.GeneralError", e.message))
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

fun openBrowser(pageName: String): ActionData {
    val action = OpenBrowserAction(pageName)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    return ActionData(result, parameters, name)
}