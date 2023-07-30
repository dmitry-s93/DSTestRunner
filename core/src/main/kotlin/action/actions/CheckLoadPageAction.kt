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

import action.*
import config.Localization
import driver.DriverSession
import storage.PageStorage

class CheckLoadPageAction(private val pageName: String) : ActionReturn(), Action {
    private var pageUrl: String? = null
    private var pageIdentifier: String? = null
    private var takeScreenshot: Boolean = false
    private var longScreenshot: Boolean = true
    private var screenData: ScreenData? = null

    override fun getName(): String {
        return Localization.get("CheckLoadPageAction.DefaultName", pageName)
    }

    override fun execute(): ActionResult {
        try {
            if (!PageStorage.isPageExist(pageName))
                return broke(Localization.get("General.PageIsNotSpecifiedInPageList", pageName))
            PageStorage.setCurrentPage(pageName)
            val pageData = PageStorage.getPage(pageName)
            pageUrl = pageData!!.getUrl()
            pageIdentifier = pageData.getIdentifier()
            if (pageUrl.isNullOrEmpty())
                return broke(Localization.get("General.PageUrlNotSpecified"))
            if (!DriverSession.getSession().checkLoadPage(pageUrl!!, pageIdentifier)) {
                if (!DriverSession.getSession().getCurrentUrl().startsWith(pageUrl.toString()))
                    return fail(Localization.get("CheckLoadPageAction.UrlDoesNotMatch"))
                return fail(Localization.get("CheckLoadPageAction.PageDidNotLoad"))
            }
            if (takeScreenshot) {
                val screenshot = DriverSession.getSession().getScreenshot(
                    workArea = pageData.getWorkArea(),
                    longScreenshot = longScreenshot,
                    ignoredElements = pageData.getIgnoredElements()
                )
                screenData = ScreenData(screenshot)
            }
        } catch (e: Exception) {
            return broke(Localization.get("CheckLoadPageAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["pageName"] = pageName
        parameters["pageUrl"] = pageUrl.toString()
        parameters["pageIdentifier"] = pageIdentifier.toString()
        return parameters
    }

    fun getScreenData(): ScreenData? {
        return screenData
    }

    fun takeScreenshot(takeScreenshot: Boolean = true, longScreenshot: Boolean = true) {
        this.takeScreenshot = takeScreenshot
        this.longScreenshot = longScreenshot
    }
}

fun checkLoadPage(pageName: String, function: (CheckLoadPageAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = CheckLoadPageAction(pageName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val screenData = action.getScreenData()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime, screenData = screenData)
}