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


package utils

import action.ActionResult
import action.ActionStatus
import action.ScreenData
import config.Localization
import config.MainConfig
import config.ScreenshotConfig
import pazone.ashot.Screenshot
import pazone.ashot.comparison.ImageDiffer
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import javax.swing.GrayFilter


class ImageUtils {
    fun compare(stepId: String, testId: String, parentId: String, screenData: ScreenData): Pair<ActionResult, ScreenData> {
        try {
            val currentScreenshotDir = ScreenshotConfig.currentScreenshotDir
            val templateScreenshotDir = ScreenshotConfig.templateScreenshotDir
            if (currentScreenshotDir.isEmpty() || templateScreenshotDir.isEmpty())
                return Pair(ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ScreenshotPathsNotSpecified")), screenData)

            val fileName = "${parentId.replace(".", "_")}_${stepId}"
            val templateImageRelativePath = Paths.get(testId, "${fileName}.png")
            val currentImageRelativePath = Paths.get(MainConfig.sessionId, testId, "${fileName}.png")
            val markedImageRelativePath = Paths.get(MainConfig.sessionId, testId, "${fileName}_marked.png")

            val currentImageFile = Paths.get(currentScreenshotDir, currentImageRelativePath.toString()).toFile()
            val templateImageFile = Paths.get(templateScreenshotDir, templateImageRelativePath.toString()).toFile()

            val currentImage = screenData.getCurrentImage()
            markIgnoredAreas(currentImage)
            saveImage(currentImage.image, currentImageFile)
            screenData.currentImagePath = currentImageRelativePath.toString()

            if (templateImageFile.exists()) {
                val templateImage = Screenshot(readImage(templateImageFile))
                templateImage.coordsToCompare = currentImage.coordsToCompare
                templateImage.ignoredAreas = currentImage.ignoredAreas
                val imageDiff = ImageDiffer().makeDiff(currentImage, templateImage)
                if (imageDiff.hasDiff() && imageDiff.diffSize > ScreenshotConfig.allowableDifference) {
                    screenData.templateImage = templateImage.image
                    screenData.templateImagePath = templateImageRelativePath.toString()

                    val markedImage = imageDiff.markedImage
                    val markedImageFile = Paths.get(currentScreenshotDir, markedImageRelativePath.toString()).toFile()
                    saveImage(markedImage, markedImageFile)
                    screenData.markedImage = markedImage
                    screenData.markedImagePath = markedImageRelativePath.toString()

                    return Pair(ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.CurrentScreenshotNotMatchReference")), screenData)
                }
            } else {
                if (ScreenshotConfig.saveTemplateIfMissing) {
                    saveImage(currentImage.image, templateImageFile)
                    screenData.templateImagePath = templateImageRelativePath.toString()
                    return Pair(ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ReferenceScreenshotMissingWithSave")), screenData)
                }
                return Pair(ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ReferenceScreenshotMissing")), screenData)
            }
            return Pair(ActionResult(ActionStatus.PASSED, null), screenData)
        } catch (e: Exception) {
            return Pair(ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.GeneralError", e.message), e.stackTraceToString()), screenData)
        }
    }

    fun saveImage(bufferedImage: BufferedImage, output: File) {
        Files.createDirectories(output.toPath())
        ImageIO.write(bufferedImage, "png", output)
    }

    private fun readImage(input: File): BufferedImage {
        return ImageIO.read(input)
    }

    private fun markIgnoredAreas(screenshot: Screenshot) {
        if (screenshot.ignoredAreas.isEmpty())
            return
        val graphics: Graphics = screenshot.image.createGraphics()
        screenshot.ignoredAreas.forEach { area ->
            try {
                val ignoredImage = screenshot.image.getSubimage(area.x, area.y, area.width, area.height)
                graphics.drawImage(GrayFilter.createDisabledImage(ignoredImage), area.x, area.y, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        graphics.dispose()
    }

    fun concatImageList(imageList: LinkedList<BufferedImage>): BufferedImage {
        var currHeight = 0
        var totalHeight = 0
        imageList.forEach { totalHeight += it.height }
        val concatImage = BufferedImage(imageList.first().width, totalHeight, BufferedImage.TYPE_INT_RGB)
        val g2d = concatImage.createGraphics()
        imageList.forEach {
            g2d.drawImage(it, 0, currHeight, null)
            currHeight += it.height
        }
        g2d.dispose()
        return concatImage
    }
}