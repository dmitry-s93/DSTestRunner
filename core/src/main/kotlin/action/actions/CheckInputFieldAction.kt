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
import action.helper.ActionHelper
import com.github.curiousoddman.rgxgen.RgxGen
import config.Localization
import driver.DriverSession
import storage.ValueStorage
import test.element.Locator
import test.page.Element


class CheckInputFieldAction(private val element: Element) : ActionReturn(), Action {
    private var elementLocator: Locator = Locator(element.locator.value, element.locator.type)
    private val locatorArguments = ArrayList<String>()
    private var doNotCheckAllowedChars: Boolean = false

    var maxSize: Int? = null
    var pattern: String? = null
    var allowedChars: String? = null

    override fun getName(): String {
        return Localization.get("CheckFieldAction.DefaultName", element.displayName)
    }

    override fun execute(): ActionResult {
        try {
            elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))

            if (maxSize == null) maxSize = element.maxSize
            if (pattern == null) pattern = element.pattern
            if (allowedChars.isNullOrEmpty()) allowedChars = element.allowedChars
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
        val genCharsCount = maxSize!! * 2
        val generatedString = RgxGen("$allowedChars{$genCharsCount}").generate()
        DriverSession.getSession().setValue(elementLocator, generatedString)
        val value = DriverSession.getSession().getElementValue(elementLocator)
        if (value.length != maxSize) {
            val currentSize = if (value.length == genCharsCount) "≥${value.length}" else value.length
            return Localization.get("CheckFieldAction.ValueLengthIsNotAsExpected", currentSize, maxSize) + "\n"
        }
        return ""
    }

    private fun checkAllowedChars(): String {
        if (doNotCheckAllowedChars)
            return ""
        val generatedString = removeDuplicateChars(RgxGen("$allowedChars{256}").generate())
        DriverSession.getSession().setValue(elementLocator, generatedString)
        val value = DriverSession.getSession().getElementValue(elementLocator)
        var valueLength = value.length
        if (generatedString.length < valueLength)
            valueLength = generatedString.length
        if (value != generatedString.substring(0, valueLength)) {
            var notEnteredChars = ""
            generatedString.forEach { char ->
                if (!value.contains(char))
                    notEnteredChars += char
            }
            if (notEnteredChars.isNotEmpty())
                return Localization.get("CheckFieldAction.NotPossibleToEnterAllowedCharacters", notEnteredChars) + "\n"
        }
        return ""
    }

    private fun checkNotAllowedChars(): String {
        if (allowedChars == ".")
            return ""
        val generatedString = removeDuplicateChars(RgxGen("$allowedChars{256}").generateNotMatching())
        DriverSession.getSession().setValue(elementLocator, generatedString)
        val value = DriverSession.getSession().getElementValue(elementLocator)
        if (value.isNotEmpty())
            return Localization.get("CheckFieldAction.PossibleToEnterUnallowedCharacters", value) + "\n"
        return ""
    }

    private fun checkPattern(): String {
        if (pattern == null)
            return ""
        val generatedString = RgxGen(pattern).generate()
        DriverSession.getSession().setValue(elementLocator, generatedString)
        val value = DriverSession.getSession().getElementValue(elementLocator)
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
        parameters["elementName"] = element.displayName
        parameters["elementLocator"] = elementLocator.value
        if (maxSize != null)
            parameters["maxSize"] = maxSize.toString()
        if (pattern != null)
            parameters["pattern"] = pattern.toString()
        if (allowedChars != ".")
            parameters["allowedCharsPattern"] = allowedChars.toString()
        if (doNotCheckAllowedChars)
            parameters["doNotCheckAllowedChars"] = "true"
        return parameters
    }

    /**
     * Substitutes the argument into the element locator
     */
    fun locatorArgument(value: String) {
        locatorArguments.add(ValueStorage.replace(value))
    }

    fun doNotCheckAllowedChars() {
        doNotCheckAllowedChars = true
    }
}

/**
 * Checks the value of the input field [element] against certain parameters
 */
fun checkInputField(element: Element, function: (CheckInputFieldAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = CheckInputFieldAction(element)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

/**
 * Checks the value of the input field [elementName] against certain parameters
 */
@Deprecated("Use an Element class object instead of a string")
fun checkInputField(elementName: String, function: (CheckInputFieldAction.() -> Unit)? = null): ActionData {
    val (element, result) = ActionHelper().getElement(elementName)
    if (element != null)
        return checkInputField(element, function)
    val name = Localization.get("CheckFieldAction.DefaultName", elementName)
    val time = System.currentTimeMillis()
    return ActionData(result!!, HashMap(), name, time, time)
}