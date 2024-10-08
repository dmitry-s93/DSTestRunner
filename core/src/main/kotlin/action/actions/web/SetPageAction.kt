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
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.URIBuilder
import storage.PageStorage
import storage.ValueStorage
import test.page.Page
import test.page.PageData

class SetPageAction(private val page: Page) : ActionReturn(), Action {
    @SuppressWarnings("WeakerAccess")
    var pageUrl: String? = null
    private val pageName: String = page.pageData.pageName
    private val urlParameters: MutableList<NameValuePair> = mutableListOf()
    private val urlArguments: HashMap<String, String> = HashMap()

    override fun getName(): String {
        return Localization.get("SetPageAction.DefaultName", pageName)
    }

    override fun execute(): ActionResult {
        PageStorage.setCurrentPage(page.pageData)
        if (pageUrl == null)
            pageUrl = page.pageData.getUrl(urlArguments)
        if (pageUrl.isNullOrEmpty())
            return broke(Localization.get("General.PageUrlNotSpecified"))
        try {
            pageUrl = URIBuilder(pageUrl).addParameters(urlParameters).toString()
            DriverSession.getSession().setPage(pageUrl!!)
        } catch (e: Exception) {
            return broke(Localization.get("SetPageAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["pageName"] = pageName
        parameters["pageUrl"] = pageUrl.toString()
        return parameters
    }

    /**
     * Adds a query parameter to the URL
     */
    fun addUrlParameter(param: String, value: String) {
        urlParameters.add(BasicNameValuePair(param, ValueStorage.replace(value)))
    }

    /**
     * Sets the value of the argument in the URL
     */
    fun setUrlArgument(arg: String, value: String) {
        urlArguments[arg] = ValueStorage.replace(value)
    }
}

/**
 * Goes to page [page]
 */
fun setPage(page: Page, function: (SetPageAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = SetPageAction(page)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

/**
 * Goes to page [page]
 */
@Deprecated("Use Page class object instead of PageData")
fun setPage(page: PageData, function: (SetPageAction.() -> Unit)? = null): ActionData {
    return  setPage(Page(page), function)
}