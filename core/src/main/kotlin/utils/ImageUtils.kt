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

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.*
import javax.imageio.ImageIO


class ImageUtils {
    fun saveImage(bufferedImage: BufferedImage, output: File) {
        Files.createDirectories(output.toPath())
        ImageIO.write(bufferedImage, "png", output)
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