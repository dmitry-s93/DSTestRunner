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

import pazone.ashot.coordinates.Coords
import java.awt.image.BufferedImage

class ImageComparisonData(
    val actualImage: BufferedImage,
    val actualImagePath: String? = null,
    var baselineImage: BufferedImage? = null,
    var baselineImagePath: String? = null,
    var diffImage: BufferedImage? = null,
    var diffImagePath: String? = null,
    /** True when the step compares screenshots, regardless of the outcome and of saved paths. */
    val isComparisonStep: Boolean = false,
    /** True when the comparison was performed and found a difference; a missing baseline counts. */
    val mismatch: Boolean = false
)

class ScreenshotData(
    val image: BufferedImage,
    val ignoredAreas: Set<Coords>,
    val coordsToCompare: Set<Coords>
)