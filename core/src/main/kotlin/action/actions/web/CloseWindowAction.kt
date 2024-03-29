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
import test.page.Page

class CloseWindowAction(private val page: Page?) : ActionReturn(), Action {
    private val pageUrl: String? = page?.pageData?.getUrl()

    override fun getName(): String {
        if (page == null)
            return Localization.get("CloseWindowAction.DefaultName")
        return Localization.get("CloseWindowAction.DefaultNameWithPage", page)
    }

    override fun execute(): ActionResult {
        try {
            if (!DriverSession.getSession().closeWindow(pageUrl))
                return fail(Localization.get("CloseWindowAction.UnableToFindWindow"))
        } catch (e: Exception) {
            return broke(Localization.get("CloseWindowAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        if (page != null)
            parameters["pageName"] = page.pageData.pageName
        if (!pageUrl.isNullOrEmpty())
            parameters["pageUrl"] = pageUrl
        return parameters
    }
}

/**
 * Closes the window with [page]
 *
 * Closes the active window (if [page] is not specified)
 */
fun closeWindow(page: Page? = null, function: (CloseWindowAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = CloseWindowAction(page)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}