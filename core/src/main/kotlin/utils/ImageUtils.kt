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

import pazone.ashot.coordinates.Coords
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.roundToInt


class ImageUtils {
    fun saveImage(bufferedImage: BufferedImage, output: File) {
        Files.createDirectories(output.toPath())
        ImageIO.write(bufferedImage, "png", output)
    }

    fun cropImage(image: BufferedImage, coords: Coords): BufferedImage {
        val x = coords.x
        val y = coords.y
        var width = coords.width
        var height = coords.height

        if (x + width > image.width)
            width = image.width - x
        if (y + height > image.height)
            height = image.height - y

        return image.getSubimage(x, y, width, height)
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

    fun recognizeScrollSize(currImage: BufferedImage, prevImage: BufferedImage, prevImagePosition: Int, ignoredAreas: Set<Rectangle>): Int {
        val subImageHeight = (prevImage.height * 0.25).roundToInt()
        val prevSubimage = prevImage.getSubimage(0, prevImage.height - subImageHeight, prevImage.width, subImageHeight)
        val subImagePos = findSubImageYPosition(
            largeImage = currImage,
            smallImage = prevSubimage,
            tolerance = 1,
            ignoredRects = ignoredAreas.toList()
        )
        if (subImagePos != -1) {
            return prevImagePosition + prevImage.height - subImageHeight - subImagePos
        }
        return -1
    }

    fun findSubImageYPosition(
        largeImage: BufferedImage,
        smallImage: BufferedImage,
        tolerance: Int = 0,
        ignoredRects:  List<Rectangle> = emptyList()
    ): Int {
        val largeWidth = largeImage.width
        val largeHeight = largeImage.height
        val smallHeight = smallImage.height

        if (smallHeight > largeHeight) {
            return -1
        }

        val smallImageData = Array(smallHeight) { y ->
            Array(largeWidth) { x ->
                smallImage.getRGB(x, y)
            }
        }

        if (tolerance == 0) {
            for (startY in 0..largeHeight - smallHeight) {
                var match = true
                for (smallY in 0 until smallHeight) {
                    val largeY = startY + smallY
                    for (x in 0 until largeWidth) {
                        var ignored = false
                        for (rect in ignoredRects) {
                            if (rect.contains(x, largeY)) {
                                ignored = true
                                break
                            }
                        }
                        if (ignored) continue

                        if (largeImage.getRGB(x, largeY) != smallImageData[smallY][x]) {
                            match = false
                            break
                        }
                    }
                    if (!match) break
                }
                if (match) {
                    return startY
                }
            }
        } else {
            val toleranceSquared = tolerance * tolerance
            fun comparePixels(pixel1: Int, pixel2: Int): Boolean {
                if (pixel1 == pixel2) return true

                val a1 = (pixel1 shr 24) and 0xFF
                val r1 = (pixel1 shr 16) and 0xFF
                val g1 = (pixel1 shr 8) and 0xFF
                val b1 = pixel1 and 0xFF

                val a2 = (pixel2 shr 24) and 0xFF
                val r2 = (pixel2 shr 16) and 0xFF
                val g2 = (pixel2 shr 8) and 0xFF
                val b2 = pixel2 and 0xFF

                val diffR = r1 - r2
                val diffG = g1 - g2
                val diffB = b1 - b2
                val diffA = a1 - a2

                return (diffR * diffR <= toleranceSquared &&
                        diffG * diffG <= toleranceSquared &&
                        diffB * diffB <= toleranceSquared &&
                        diffA * diffA <= toleranceSquared)
            }

            for (startY in 0..largeHeight - smallHeight) {
                var match = true
                for (smallY in 0 until smallHeight) {
                    val largeY = startY + smallY
                    for (x in 0 until largeWidth) {
                        var ignored = false
                        for (rect in ignoredRects) {
                            if (rect.contains(x, largeY)) {
                                ignored = true
                                break
                            }
                        }
                        if (ignored) continue
                        if (!comparePixels(largeImage.getRGB(x, largeY), smallImageData[smallY][x])) {
                            match = false
                            break
                        }
                    }
                    if (!match) break
                }
                if (match) {
                    return startY
                }
            }
        }
        return -1
    }
}