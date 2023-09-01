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


package action

import pazone.ashot.Screenshot
import java.awt.image.BufferedImage

class ScreenData(private val currentImage: Screenshot) {
    private var _templateImage: BufferedImage? = null
    private var _markedImage: BufferedImage? = null

    private var _templateImagePath: String? = null
    private var _currentImagePath: String? = null
    private var _markedImagePath: String? = null

    fun getCurrentImage(): Screenshot {
        return currentImage
    }

    var templateImage: BufferedImage?
        get() = _templateImage
        set(value) {
            _templateImage = value
        }

    var markedImage: BufferedImage?
        get() = _markedImage
        set(value) {
            _markedImage = value
        }

    var currentImagePath: String?
        get() = _currentImagePath
        set(value) {
            _currentImagePath = value
        }

    var templateImagePath: String?
        get() = _templateImagePath
        set(value) {
            _templateImagePath = value
        }

    var markedImagePath: String?
        get() = _markedImagePath
        set(value) {
            _markedImagePath = value
        }
}