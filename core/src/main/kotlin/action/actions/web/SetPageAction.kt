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

class SetPageAction(private val pageName: String) : ActionReturn(), Action {
    @SuppressWarnings("WeakerAccess")
    var pageUrl: String? = null
    private val urlParameters: MutableList<NameValuePair> = mutableListOf()
    private val urlArguments: HashMap<String, String> = HashMap()

    override fun getName(): String {
        return Localization.get("SetPageAction.DefaultName", pageName)
    }

    override fun execute(): ActionResult {
        if (!PageStorage.isPageExist(pageName))
            return broke(Localization.get("General.PageIsNotSpecifiedInPageList", pageName))
        PageStorage.setCurrentPage(pageName)
        if (pageUrl == null)
            pageUrl = PageStorage.getPage(pageName)?.getUrl(urlArguments)
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
 * Goes to page [pageName]
 */
fun setPage(pageName: String, function: (SetPageAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = SetPageAction(pageName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}