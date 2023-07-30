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