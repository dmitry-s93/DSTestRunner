package utils

import action.ActionResult
import action.ActionStatus
import action.ScreenData
import config.Localization
import config.MainConfig
import config.ScreenshotConfig
import pazone.ashot.Screenshot
import pazone.ashot.comparison.ImageDiffer
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO


class ImageUtils {
    fun compare(id: String, parentId: String, screenData: ScreenData): Pair<ActionResult, ScreenData> {
        try {
            val currentScreenshotDir = ScreenshotConfig.currentScreenshotDir
            val templateScreenshotDir = ScreenshotConfig.templateScreenshotDir
            if (currentScreenshotDir.isEmpty() || templateScreenshotDir.isEmpty())
                return Pair(ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ScreenshotPathsNotSpecified")), screenData)

            val relativeDirPath = parentId.replace(".", "/")
            val templateImageRelativePath = Paths.get(relativeDirPath, "$id.png")
            val currentImageRelativePath = Paths.get(MainConfig.sessionId, relativeDirPath, "$id.png")
            val markedImageRelativePath = Paths.get(MainConfig.sessionId, relativeDirPath, "${id}_marked.png")

            val currentImageFile = Paths.get(currentScreenshotDir, currentImageRelativePath.toString()).toFile()
            val templateImageFile = Paths.get(templateScreenshotDir, templateImageRelativePath.toString()).toFile()

            val currentImage = screenData.getCurrentImage()
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
}