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
import config.Localization
import driver.DriverSession
import storage.ValueStorage
import test.element.Locator
import test.page.Element
import java.io.File

class UploadFileAction(private val element: Element, filePath: String) : ActionReturn(), Action {
    private val file: String = ValueStorage.replace(filePath)
    private lateinit var elementLocator: Locator
    private val locatorArguments = ArrayList<String>()

    override fun getName(): String {
        return Localization.get("UploadFileAction.DefaultName", File(file).name, element.displayName)
    }

    override fun execute(): ActionResult {
        try {
            elementLocator = element.locator.withReplaceArgs(*locatorArguments.toArray())
            if (elementLocator.value.isEmpty())
                return broke(Localization.get("General.ElementLocatorNotSpecified"))
            DriverSession.getSession().uploadFile(elementLocator, file)
        } catch (e: Exception) {
            return broke(Localization.get("UploadFileAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["elementName"] = element.displayName
        parameters["elementLocator"] = elementLocator.value
        parameters["file"] = file
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
 * Uploads the file in the file upload field
 */
fun uploadFile(element: Element, filePath: String, function: (UploadFileAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = UploadFileAction(element, filePath)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}

/**
 * Uploads the file in the file upload field
 */
fun uploadFile(elementName: String, filePath: String, function: (UploadFileAction.() -> Unit)? = null): ActionData {
    val (element, result) = ActionHelper().getElement(elementName)
    if (element != null)
        return uploadFile(element, filePath, function)
    val name = Localization.get("UploadFileAction.DefaultName", File(ValueStorage.replace(filePath)).name, elementName)
    val time = System.currentTimeMillis()
    return ActionData(result!!, HashMap(), name, time, time)
}