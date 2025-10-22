package action.actions.mobile

import action.Action
import action.ActionData
import action.ActionResult
import action.ActionReturn
import config.Localization
import driver.DriverSession

class DisableDataConnectionAction() : ActionReturn(), Action {
    override fun getName(): String {
        return Localization.get("DisableDataConnectionAction.DefaultName")
    }

    override fun execute(): ActionResult {
        try {
            DriverSession.getSession().disableDataConnection()
        } catch (e: Exception) {
            return broke(Localization.get("DisableDataConnectionAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        return parameters
    }
}

fun disableDataConnection(function: (DisableDataConnectionAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = DisableDataConnectionAction()
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}