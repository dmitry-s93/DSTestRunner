package utils

import action.ActionResult
import action.ActionStatus
import action.ScreenData
import config.Localization
import config.MainConfig.Companion.getSessionId
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
            val currentScreenshotDir = ScreenshotConfig.getCurrentScreenshotDir()
            val templateScreenshotDir = ScreenshotConfig.getTemplateScreenshotDir()
            if (currentScreenshotDir.isEmpty() || templateScreenshotDir.isEmpty())
                return Pair(ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.ScreenshotPathsNotSpecified")), screenData)

            val currentImageDir = Paths.get(currentScreenshotDir, getSessionId(), parentId.replace(".", "/"))
            val currentImageFile = Paths.get(currentImageDir.toString(), "$id.png").toFile()
            val templateImageFile = Paths.get(templateScreenshotDir, parentId.replace(".", "/"), "$id.png").toFile()

            val currentImage = screenData.getCurrentImage()
            saveImage(currentImage.image, currentImageFile)
            screenData.currentImagePath = currentImageFile.path

            if (templateImageFile.exists()) {
                val templateImage = Screenshot(readImage(templateImageFile))
                templateImage.coordsToCompare = currentImage.coordsToCompare
                templateImage.ignoredAreas = currentImage.ignoredAreas
                val imageDiff = ImageDiffer().makeDiff(currentImage, templateImage)
                if (imageDiff.hasDiff() && imageDiff.diffSize > ScreenshotConfig.getAllowableDifference()) {
                    screenData.templateImage = templateImage.image
                    screenData.templateImagePath = templateImageFile.path

                    val markedImage = imageDiff.markedImage
                    val markedImageFile = Paths.get(currentImageDir.toString(), "${id}_marked.png").toFile()
                    saveImage(markedImage, markedImageFile)
                    screenData.markedImage = markedImage
                    screenData.markedImagePath = markedImageFile.path

                    return Pair(ActionResult(ActionStatus.BROKEN, Localization.get("ScreenshotCompare.CurrentScreenshotNotMatchReference")), screenData)
                }
            } else {
                if (ScreenshotConfig.getSaveTemplateIfMissing()) {
                    saveImage(currentImage.image, templateImageFile)
                    screenData.templateImagePath = templateImageFile.path
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