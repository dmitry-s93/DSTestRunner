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
import logger.Logger
import storage.PageStorage
import storage.ValueStorage
import test.element.Locator
import java.util.*


class CheckLoadPageAction(private val pageName: String) : ActionReturn(), Action {
    private var pageUrl: String? = null
    private var pageIdentifier: Locator? = null
    private var screenData: ScreenData? = null
    private var screenshotAreas: MutableList<Locator> = mutableListOf()
    private var takeScreenshotClass: TakeScreenshot? = null

    override fun getName(): String {
        return Localization.get("CheckLoadPageAction.DefaultName", pageName)
    }

    override fun execute(): ActionResult {
        try {
            if (!PageStorage.isPageExist(pageName))
                return broke(Localization.get("General.PageIsNotSpecifiedInPageList", pageName))
            PageStorage.setCurrentPage(pageName)
            val pageData = PageStorage.getPage(pageName)
            val urlPath = pageData!!.getUrlPath()
            if (urlPath != null) {
                pageUrl = pageData.getUrl()
                if (pageUrl.isNullOrEmpty())
                    return broke(Localization.get("General.PageUrlNotSpecified"))
            }
            pageIdentifier = pageData.getIdentifier()
            if (!DriverSession.getSession().checkLoadPage(pageUrl, pageIdentifier)) {
                if (!pageUrl.isNullOrEmpty() && !DriverSession.getSession().getCurrentUrl().startsWith(pageUrl.toString()))
                    return fail(Localization.get("CheckLoadPageAction.UrlDoesNotMatch"))
                return fail(Localization.get("CheckLoadPageAction.PageDidNotLoad"))
            }
            if (takeScreenshotClass != null) {
                val longScreenshot = takeScreenshotClass!!.longScreenshot
                val elements = takeScreenshotClass!!.elements
                if (elements.isNotEmpty()) {
                    elements.forEach { (elementName, locatorArguments) ->
                        val locator = pageData.getElement(elementName)?.getLocator()?.withReplaceArgs(*locatorArguments.toTypedArray())
                        if (locator != null)
                            screenshotAreas.add(locator)
                        else
                            Logger.warning(Localization.get("General.ElementIsNotSetOnPage", elementName, pageName))
                    }
                }
                if (screenshotAreas.isEmpty())
                    pageData.getWorkArea()?.let { screenshotAreas.add(it) }
                val screenshot = DriverSession.getSession().getScreenshot(
                    screenshotAreas = screenshotAreas,
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
        if (pageUrl != null)
            parameters["pageUrl"] = pageUrl.toString()
        parameters["pageIdentifier"] = pageIdentifier?.value.toString()
        if (screenshotAreas.isNotEmpty())
            parameters["workArea"] = screenshotAreas.toString()
        return parameters
    }

    fun getScreenData(): ScreenData? {
        return screenData
    }

    /**
     * Takes a screenshot of the page.
     *
     * [takeScreenshot] - determines whether a screenshot should be taken.
     */
    fun takeScreenshot(takeScreenshot: Boolean = true, function: (TakeScreenshot.() -> Unit)? = null) {
        if (takeScreenshot) {
            takeScreenshotClass = TakeScreenshot()
            function?.invoke(takeScreenshotClass!!)
        }
    }

    class TakeScreenshot {
        var longScreenshot: Boolean = true
        val elements = HashMap<String, List<String>>()

        /**
         * Specifies the element to take a screenshot.
         *
         * [elementName] - element name.
         *
         * [locatorArguments] -  arguments to Substitute in the Locator String.
         */
        fun elementScreenshot(elementName: String, vararg locatorArguments: String) {
            val arguments: MutableList<String> = mutableListOf()
            locatorArguments.forEach {
                arguments.add(ValueStorage.replace(it))
            }
            elements[elementName] = arguments
        }
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