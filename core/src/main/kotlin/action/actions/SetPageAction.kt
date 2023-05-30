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
import driver.DriverSession
import storage.PageStorage

class SetPageAction(pageName: String) : ActionReturn(), Action {
    private val pageName: String
    private val pageUrl: String?

    init {
        this.pageName = pageName
        this.pageUrl = PageStorage.getPage(pageName)?.getUrl()
    }

    override fun getName(): String {
        return "Go to the '$pageName' page"
    }

    override fun execute(): ActionResult {
        if (pageUrl.isNullOrEmpty())
            return fail("Page url not specified")
        try {
            DriverSession.getSession().setPage(pageUrl)
        } catch (e: Exception) {
            return fail("An error occurred while opening the page: ${e.message}")
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

fun setPage(pageName: String, function: (SetPageAction.() -> Unit)? = null): ActionData {
    val action = SetPageAction(pageName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    return ActionData(result, parameters, name)
}