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
    private val actualScreenshotDir = ScreenshotConfig.actualScreenshotDir
    private val baselineScreenshotDir = ScreenshotConfig.baselineScreenshotDir

    private val fileName = "${parentId.replace(".", "_")}_${stepId}"
    private val baselineImageRelativePath = Paths.get(testId, "${fileName}.png").toString()
    private val actualImageRelativePath = Paths.get(MainConfig.sessionId.toString(), testId, "${fileName}.png").toString()
    private val diffImageRelativePath = Paths.get(MainConfig.sessionId.toString(), testId, "${fileName}_marked.png").toString()

    private var actualImage = screenshotData.image
    private var baselineImage: BufferedImage? = null
    private var diffImage: BufferedImage? = null

    private val ignoredAreas = screenshotData.ignoredAreas
    private val coordsToCompare = screenshotData.coordsToCompare

    fun compare(): Pair<ActionResult, ImageComparisonData> {
        if (actualScreenshotDir.isEmpty() || baselineScreenshotDir.isEmpty()) {
            return Pair(
                ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ScreenshotPathsNotSpecified")),
                ImageComparisonData(actualImage, isComparisonStep = true)
            )
        }
        try {
            markIgnoredAreas(actualImage, ignoredAreas)
            saveActualImage(actualImage)
            baselineImage = readBaselineImage()
            if (baselineImage != null) {
                compareImages()
                if (diffImage != null) {
                    return Pair(
                        ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ActualScreenshotNotMatchBaseline")),
                        ImageComparisonData(
                            actualImage = actualImage, actualImagePath = actualImageRelativePath,
                            baselineImage = baselineImage, baselineImagePath = baselineImageRelativePath,
                            diffImage = diffImage, diffImagePath = diffImageRelativePath,
                            isComparisonStep = true, mismatch = true
                        )
                    )
                }
            } else {
                if (ScreenshotConfig.saveBaselineIfMissing) {
                    saveBaselineImage(actualImage)
                    return Pair(
                        ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.BaselineScreenshotMissingWithSave")),
                        ImageComparisonData(
                            actualImage = actualImage, actualImagePath = actualImageRelativePath,
                            baselineImage = baselineImage, baselineImagePath = baselineImageRelativePath,
                            isComparisonStep = true, mismatch = true
                        )
                    )
                }
                return Pair(
                    ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.BaselineScreenshotMissing")),
                    ImageComparisonData(
                        actualImage = actualImage, actualImagePath = actualImageRelativePath,
                        isComparisonStep = true, mismatch = true
                    )
                )
            }
            return Pair(
                ActionResult(ActionStatus.PASSED, null),
                ImageComparisonData(
                    actualImage = actualImage, actualImagePath = actualImageRelativePath,
                    baselineImage = baselineImage, baselineImagePath = baselineImageRelativePath,
                    isComparisonStep = true
                )
            )
        } catch (e: Exception) {
            return Pair(
                ActionResult(
                    ActionStatus.BROKEN,
                    Localization.get("ScreenshotCompare.GeneralError", e.message),
                    e.stackTraceToString()
                ),
                ImageComparisonData(actualImage, isComparisonStep = true)
            )
        }
    }

    private fun saveActualImage(image: BufferedImage) {
        val file = Paths.get(actualScreenshotDir, actualImageRelativePath).toFile()
        ImageUtils().saveImage(image, file)
    }

    private fun readBaselineImage(): BufferedImage? {
        val file = Paths.get(baselineScreenshotDir, baselineImageRelativePath).toFile()
        if (file.exists())
            return ImageIO.read(file)
        return null
    }

    private fun saveBaselineImage(image: BufferedImage) {
        val file = Paths.get(baselineScreenshotDir, baselineImageRelativePath).toFile()
        ImageUtils().saveImage(image, file)
        baselineImage = actualImage
    }

    private fun compareImages() {
        if (baselineImage != null) {
            val expandedIgnoredAreas = expandAreas(ignoredAreas, ScreenshotConfig.expandIgnoredAreasByPixels)

            val baselineScreenshot = Screenshot(baselineImage)
            baselineScreenshot.coordsToCompare = coordsToCompare
            baselineScreenshot.ignoredAreas = expandedIgnoredAreas

            val actualScreenshot = Screenshot(actualImage)
            actualScreenshot.coordsToCompare = coordsToCompare
            actualScreenshot.ignoredAreas = expandedIgnoredAreas

            val imageDiff = ImageDiffer().makeDiff(baselineScreenshot, actualScreenshot)
            if (imageDiff.hasDiff() && imageDiff.diffSize > ScreenshotConfig.allowableDifference) {
                diffImage = imageDiff.markedImage
                ImageUtils().saveImage(diffImage!!, Paths.get(actualScreenshotDir, diffImageRelativePath).toFile())
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
