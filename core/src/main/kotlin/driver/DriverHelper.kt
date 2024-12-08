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

package driver

import logger.Logger
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import pazone.ashot.coordinates.Coords
import java.awt.Point
import java.awt.Rectangle
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class DriverHelper {
    fun handleStaleElementReferenceException(logSource: String, numberOfAttempts: Int, function: () -> Unit) {
        try {
            function()
        } catch (e: StaleElementReferenceException) {
            if (numberOfAttempts > 0) {
                Logger.info("Stale element reference. Retrying.", logSource)
                return handleStaleElementReferenceException(logSource, numberOfAttempts - 1) {
                    function()
                }
            }
            throw e
        }
    }

    fun getElementCenter(element: WebElement): Point {
        val elementLocation = element.location
        val elementSize = element.size
        return Point(
            elementLocation.x + (elementSize.width / 2),
            elementLocation.y + (elementSize.height / 2)
        )
    }

    fun getNodesByXpath(source: String, expression: String ): NodeList {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val `is` = InputSource(StringReader(source))
        val document: Document = builder.parse(`is`)

        val xPathFactory = XPathFactory.newInstance()
        val xpath = xPathFactory.newXPath()
        val expr = xpath.compile(expression)
        return expr.evaluate(document, XPathConstants.NODESET) as NodeList
    }

    fun rectanglesToCoords(rectangles: Set<Rectangle>): Set<Coords> {
        val coords: MutableSet<Coords> = HashSet()
        rectangles.forEach {
            coords.add(Coords(it))
        }
        return coords
    }

    fun reduceAreaByPercent(coords: Coords?, percent: Byte): Coords? {
        if (coords == null)
            return null

        val reduceWidthPx = (coords.width * percent * 0.01).toInt()
        val reduceHeightPx = (coords.height * percent * 0.01).toInt()

        val x = coords.x + (reduceWidthPx / 2)
        val y = coords.y + (reduceHeightPx / 2)
        val width = coords.width - reduceWidthPx
        val height = coords.height - reduceHeightPx

        return Coords(x, y, width, height)
    }
}