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
import test.page.PageData

class CheckLoadPageAction(private val page: PageData) : ActionReturn(), Action {
    private var pageUrl: String? = null
    private var screenData: ScreenData? = null
    private val ignoredElements: MutableSet<Locator> = HashSet()
    private var screenshotAreas: MutableSet<Locator> = HashSet()
    private var takeScreenshotClass: TakeScreenshot? = null

    override fun getName(): String {
        return Localization.get("CheckLoadPageAction.DefaultName", page.pageName)
    }

    override fun execute(): ActionResult {
        try {
            PageStorage.setCurrentPage(page)
            if (page.urlPath != null) {
                pageUrl = page.getUrl()
                if (pageUrl.isNullOrEmpty())
                    return broke(Localization.get("General.PageUrlNotSpecified"))
            }
            if (!DriverSession.getSession().checkLoadPage(pageUrl, page.identifier)) {
                if (!pageUrl.isNullOrEmpty() && !DriverSession.getSession().getCurrentUrl().startsWith(pageUrl.toString()))
                    return fail(Localization.get("CheckLoadPageAction.UrlDoesNotMatch"))
                return fail(Localization.get("CheckLoadPageAction.PageDidNotLoad"))
            }
            if (takeScreenshotClass != null) {
                page.waitTimeBeforeScreenshot?.let { Thread.sleep(it) }
                val longScreenshot = takeScreenshotClass!!.longScreenshot
                ignoredElements.addAll(page.ignoredElements)
                ignoredElements.addAll(getElements(page, takeScreenshotClass!!.getIgnoredElements()))
                screenshotAreas.addAll(getElements(page, takeScreenshotClass!!.getElements()))
                if (screenshotAreas.isEmpty())
                    page.workArea?.let { screenshotAreas.add(it) }
                val screenshot = DriverSession.getSession().getScreenshot(
                    longScreenshot = longScreenshot,
                    ignoredElements = ignoredElements,
                    screenshotAreas = screenshotAreas
                )
                screenData = ScreenData(screenshot)
            }
        } catch (e: Exception) {
            return broke(Localization.get("CheckLoadPageAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    private fun getElements(pageData: PageData, elements: HashMap<String, List<String>>): Set<Locator> {
        val locators: MutableSet<Locator> = HashSet()
        elements.forEach { (elementName, locatorArguments) ->
            val locator = pageData.getElement(elementName)?.getLocator()?.withReplaceArgs(*locatorArguments.toTypedArray())
            if (locator != null)
                locators.add(locator)
            else
                Logger.warning(Localization.get("General.ElementIsNotSetOnPage", elementName, page.pageName))
        }
        return locators
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["pageName"] = page.pageName
        if (pageUrl != null)
            parameters["pageUrl"] = pageUrl.toString()
        parameters["pageIdentifier"] = page.identifier?.value.toString()
        if (ignoredElements.isNotEmpty()) {
            val elements: MutableSet<String> = HashSet()
            ignoredElements.forEach {
                elements.add(it.value)
            }
            parameters["ignoredElements"] = elements.toString()
        }
        if (screenshotAreas.isNotEmpty()) {
            val elements: MutableSet<String> = HashSet()
            screenshotAreas.forEach {
                elements.add(it.value)
            }
            parameters["screenshotAreas"] = elements.toString()
        }
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
        private val elements = HashMap<String, List<String>>()
        private val ignoredElements = HashMap<String, List<String>>()

        fun getElements(): HashMap<String, List<String>> {
            return elements
        }

        fun getIgnoredElements() : HashMap<String, List<String>> {
            return ignoredElements
        }

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

        /**
         * Specifies the element to ignore in the screenshot.
         *
         * [elementName] - element name.
         *
         * [locatorArguments] -  arguments to Substitute in the Locator String.
         */
        fun ignoreElement(elementName: String, vararg locatorArguments: String) {
            val arguments: MutableList<String> = mutableListOf()
            locatorArguments.forEach {
                arguments.add(ValueStorage.replace(it))
            }
            ignoredElements[elementName] = arguments
        }
    }
}

fun checkLoadPage(page: PageData, function: (CheckLoadPageAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = CheckLoadPageAction(page)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val screenData = action.getScreenData()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime, screenData = screenData)
}