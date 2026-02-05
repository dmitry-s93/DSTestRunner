/* Copyright 2024 DSTestRunner Contributors
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

package utils

import action.*
import config.Localization
import config.MainConfig
import config.ScreenshotConfig
import pazone.ashot.Screenshot
import pazone.ashot.comparison.ImageDiffer
import pazone.ashot.coordinates.Coords
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.nio.file.Paths
import javax.imageio.ImageIO
import javax.swing.GrayFilter

class ImageComparison(stepId: String, private val testId: String, parentId: String, screenshotData: ScreenshotData) {
    private val currentScreenshotDir = ScreenshotConfig.currentScreenshotDir
    private val templateScreenshotDir = ScreenshotConfig.templateScreenshotDir

    private val fileName = "${parentId.replace(".", "_")}_${stepId}"
    private val templateImageRelativePath = Paths.get(testId, "${fileName}.png").toString()
    private val currentImageRelativePath = Paths.get(MainConfig.sessionId.toString(), testId, "${fileName}.png").toString()
    private val markedImageRelativePath = Paths.get(MainConfig.sessionId.toString(), testId, "${fileName}_marked.png").toString()

    private var currentImage = screenshotData.image
    private var templateImage: BufferedImage? = null
    private var markedImage: BufferedImage? = null

    private val ignoredAreas = screenshotData.ignoredAreas
    private val coordsToCompare = screenshotData.coordsToCompare

    fun compare(): Pair<ActionResult, ImageComparisonData> {
        if (currentScreenshotDir.isEmpty() || templateScreenshotDir.isEmpty()) {
            return Pair(
                ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ScreenshotPathsNotSpecified")),
                ImageComparisonData(currentImage)
            )
        }
        try {
            markIgnoredAreas(currentImage, ignoredAreas)
            saveCurrentImage(currentImage)
            templateImage = readTemplateImage()
            if (templateImage != null) {
                compareImages()
                if (markedImage != null) {
                    return Pair(
                        ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.CurrentScreenshotNotMatchReference")),
                        ImageComparisonData(
                            currentImage = currentImage, currentImagePath = currentImageRelativePath,
                            templateImage = templateImage, templateImagePath = templateImageRelativePath,
                            markedImage = markedImage, markedImagePath = markedImageRelativePath
                        )
                    )
                }
            } else {
                if (ScreenshotConfig.saveTemplateIfMissing) {
                    saveTemplateImage(currentImage)
                    return Pair(
                        ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ReferenceScreenshotMissingWithSave")),
                        ImageComparisonData(
                            currentImage = currentImage, currentImagePath = currentImageRelativePath,
                            templateImage = templateImage, templateImagePath = templateImageRelativePath
                        )
                    )
                }
                return Pair(
                    ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ReferenceScreenshotMissing")),
                    ImageComparisonData(currentImage = currentImage, currentImagePath = currentImageRelativePath)
                )
            }
            return Pair(
                ActionResult(ActionStatus.PASSED, null),
                ImageComparisonData(
                    currentImage = currentImage, currentImagePath = currentImageRelativePath,
                    templateImage = templateImage, templateImagePath = templateImageRelativePath
                )
            )
        } catch (e: Exception) {
            return Pair(
                ActionResult(
                    ActionStatus.BROKEN,
                    Localization.get("ScreenshotCompare.GeneralError", e.message),
                    e.stackTraceToString()
                ),
                ImageComparisonData(currentImage)
            )
        }
    }

    private fun saveCurrentImage(image: BufferedImage) {
        val file = Paths.get(currentScreenshotDir, currentImageRelativePath).toFile()
        ImageUtils().saveImage(image, file)
    }

    private fun readTemplateImage(): BufferedImage? {
        val file = Paths.get(templateScreenshotDir, templateImageRelativePath).toFile()
        if (file.exists())
            return ImageIO.read(file)
        return null
    }

    private fun saveTemplateImage(image: BufferedImage) {
        val file = Paths.get(templateScreenshotDir, templateImageRelativePath).toFile()
        ImageUtils().saveImage(image, file)
        templateImage = currentImage
    }

    private fun compareImages() {
        if (templateImage != null) {
            val expandedIgnoredAreas = expandAreas(ignoredAreas, ScreenshotConfig.expandIgnoredAreasByPixels)

            val templateScreenshot = Screenshot(templateImage)
            templateScreenshot.coordsToCompare = coordsToCompare
            templateScreenshot.ignoredAreas = expandedIgnoredAreas

            val currentScreenshot = Screenshot(currentImage)
            currentScreenshot.coordsToCompare = coordsToCompare
            currentScreenshot.ignoredAreas = expandedIgnoredAreas

            val imageDiff = ImageDiffer().makeDiff(templateScreenshot, currentScreenshot)
            if (imageDiff.hasDiff() && imageDiff.diffSize > ScreenshotConfig.allowableDifference) {
                markedImage = imageDiff.markedImage
                ImageUtils().saveImage(markedImage!!, Paths.get(currentScreenshotDir, markedImageRelativePath).toFile())
            }
        }
    }

    private fun expandAreas(areas: Set<Coords>, expandByPixels: Int): Set<Coords> {
        val resultAreas: MutableSet<Coords> = HashSet()
        if (expandByPixels > 0) {
            areas.forEach { area ->
                val x = area.x - expandByPixels
                val y = area.y - expandByPixels
                val width = area.width + (expandByPixels * 2)
                val height = area.height + (expandByPixels * 2)
                resultAreas.add(Coords(x, y, width, height))
            }
        } else {
            resultAreas.addAll(areas)
        }
        return resultAreas
    }

    private fun markIgnoredAreas(image: BufferedImage, ignoredAreas: Set<Coords>) {
        if (ignoredAreas.isEmpty())
            return
        val graphics: Graphics = image.createGraphics()
        ignoredAreas.forEach { area ->
            try {
                val ignoredImage = image.getSubimage(area.x, area.y, area.width, area.height)
                graphics.drawImage(GrayFilter.createDisabledImage(ignoredImage), area.x, area.y, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        graphics.dispose()
    }
}
