package action.helper

import action.ActionResult
import action.ActionReturn
import config.Localization
import storage.PageStorage
import test.page.Element

class ActionHelper {
    fun getElement(elementName: String): Pair<Element?, ActionResult?> {
        val pageData = PageStorage.getCurrentPage()
            ?: return Pair(null, ActionReturn().broke(Localization.get("General.CurrentPageIsNotSet")))
        val element = pageData.getElement(elementName)
            ?: return Pair(null, ActionReturn().broke(Localization.get("General.ElementIsNotSetOnPage", elementName, pageData.pageName)))
        return Pair(element, null)
    }
}