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
import com.github.curiousoddman.rgxgen.RgxGen
import config.Localization
import driver.DriverSession
import storage.PageStorage
import storage.ValueStorage


class CheckInputFieldAction(private val elementName: String) : ActionReturn(), Action {
    private var elementLocator: String? = null
    private val locatorArguments = ArrayList<String>()

    var maxSize: Int? = null
    var pattern: String? = null
    var allowedChars: String? = null

    override fun getName(): String {
        return Localization.get("CheckFieldAction.DefaultName", elementName)
    }

    override fun execute(): ActionResult {
        try {
            val pageData = PageStorage.getCurrentPage() ?: return broke(Localization.get("General.CurrentPageIsNotSet"))
            if (!pageData.isElementExist(elementName))
                return broke(Localization.get("General.ElementIsNotSetOnPage", elementName, pageData.getPageName()))
            val webElement = pageData.getElement(elementName)
            elementLocator = webElement?.getLocator()
            if (elementLocator.isNullOrEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            elementLocator = String.format(elementLocator!!, *locatorArguments.toArray())

            if (maxSize == null) maxSize = webElement?.getMaxSize()
            if (pattern == null) pattern = webElement?.getPattern()
            if (allowedChars.isNullOrEmpty()) allowedChars = webElement?.getAllowedChars()
            if (allowedChars.isNullOrEmpty())
                allowedChars = "."
            else
                allowedChars = "[$allowedChars]"

            val result = checkFieldSize() + checkAllowedChars() + checkNotAllowedChars() + checkPattern()
            if (result.isNotEmpty()) {
                return fail(result.substring(0, result.length - 1))
            }
        } catch (e: Exception) {
            return broke(Localization.get("CheckFieldAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    private fun checkFieldSize(): String {
        if (maxSize == null)
            return ""
        val generatedString = RgxGen("$allowedChars{${maxSize!! * 2}}").generate()
        DriverSession.getSession().setValue(elementLocator!!, generatedString)
        val value = DriverSession.getSession().getElementValue(elementLocator!!)
        if (value.length != maxSize)
            return Localization.get("CheckFieldAction.ValueLengthIsNotAsExpected", value.length, maxSize) + "\n"
        return ""
    }

    private fun checkAllowedChars(): String {
        val generatedString = removeDuplicateChars(RgxGen("$allowedChars{256}").generate())
        DriverSession.getSession().setValue(elementLocator!!, generatedString)
        val value = DriverSession.getSession().getElementValue(elementLocator!!)
        val valueLength = value.length
        if (value != generatedString.substring(0, valueLength)) {
            var notEnteredChars = ""
            var j = 0
            for (i in value.indices) {
                if (j > generatedString.length - 1) break
                while (value[i].toString() != generatedString[j].toString()) {
                    notEnteredChars += generatedString[j]
                    j += 1
                }
                j += 1
            }
            return Localization.get("CheckFieldAction.NotPossibleToEnterAllowedCharacters", notEnteredChars) + "\n"
        }
        return ""
    }

    private fun checkNotAllowedChars(): String {
        if (allowedChars == ".")
            return ""
        val generatedString = removeDuplicateChars(RgxGen("$allowedChars{256}").generateNotMatching())
        DriverSession.getSession().setValue(elementLocator!!, generatedString)
        val value = DriverSession.getSession().getElementValue(elementLocator!!)
        if (value.isNotEmpty())
            return Localization.get("CheckFieldAction.PossibleToEnterUnallowedCharacters", value) + "\n"
        return ""
    }

    private fun checkPattern(): String {
        if (pattern == null)
            return ""
        val generatedString = RgxGen(pattern).generate()
        DriverSession.getSession().setValue(elementLocator!!, generatedString)
        val value = DriverSession.getSession().getElementValue(elementLocator!!)
        if (value != generatedString)
            return Localization.get("CheckFieldAction.ValueDoesNotMatchExpected", value, generatedString) + "\n"
        return ""
    }

    private fun removeDuplicateChars(string: String): String {
        val charSet: MutableSet<Char> = LinkedHashSet()
        for (char in string.toCharArray()) {
            charSet.add(char)
        }
        val stringBuilder = StringBuilder()
        for (char in charSet) {
            stringBuilder.append(char)
        }
        return stringBuilder.toString()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = elementName
        parameters["elementLocator"] = elementLocator.toString()
        if (maxSize != null)
            parameters["maxSize"] = maxSize.toString()
        if (pattern != null)
            parameters["pattern"] = pattern.toString()
        if (allowedChars != ".")
            parameters["allowedCharsPattern"] = allowedChars.toString()
        return parameters
    }

    /**
     * Substitutes the argument into the element locator
     */
    fun locatorArgument(value: String) {
        locatorArguments.add(ValueStorage.replace(value))
    }
}

/**
 * Checks the value of the input field [elementName] against certain parameters
 */
fun checkInputField(elementName: String, function: (CheckInputFieldAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = CheckInputFieldAction(elementName)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}