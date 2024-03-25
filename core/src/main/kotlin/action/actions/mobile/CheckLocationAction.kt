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

package action.actions.mobile

import action.Action
import action.ActionData
import action.ActionResult
import action.ActionReturn
import config.Localization
import driver.DriverSession
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import java.time.Duration
import kotlin.math.*

class CheckLocationAction(private val latitude: Double, private val longitude: Double) : ActionReturn(), Action {
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var distance : Double = 0.0
    @Suppress("MemberVisibilityCanBePrivate")
    var maxDistanceInMeters: Double = 10.0

    override fun getName(): String {
        return Localization.get("CheckLocationAction.DefaultName")
    }

    override fun execute(): ActionResult {
        try {
            try {
                val driverSession = DriverSession.getSession()
                Awaitility.await()
                    .atLeast(Duration.ofMillis(0))
                    .pollDelay(Duration.ofMillis(100))
                    .atMost(Duration.ofMillis(10000))
                    .until {
                        val currentLocation = driverSession.getLocation()
                        currentLatitude = currentLocation.latitude
                        currentLongitude = currentLocation.longitude
                        distance = calculateDistance(currentLatitude, currentLongitude, latitude, longitude) * 1000
                        distance <= maxDistanceInMeters
                    }
            } catch (_: ConditionTimeoutException) {
                return fail(Localization.get("CheckLocationAction.LocationIsNotAsExpected", distance))
            }
        } catch (e: Exception) {
            return broke(Localization.get("CheckLocationAction.GeneralError", e.message), e.stackTraceToString())
        }
        return pass()
    }

    override fun getParameters(): HashMap<String, String> {
        val parameters = HashMap<String, String>()
        parameters["expectedLatitude"] = latitude.toString()
        parameters["expectedLongitude"] = longitude.toString()
        parameters["currentLatitude"] = currentLatitude.toString()
        parameters["currentLongitude"] = currentLongitude.toString()
        parameters["distanceBetweenPoints"] = distance.toString()
        parameters["maxDistanceInMeters"] = maxDistanceInMeters.toString()
        return parameters
    }

    /**
     * Determines distances using the haversine formula
     */
    private fun calculateDistance(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
        val radius = 6372.8 // in kilometers
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLon = Math.toRadians(longitude2 - longitude1)
        return 2 * radius * asin(sqrt(sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)))
    }
}

/**
 * Checks the current location of the mobile device ([latitude] and [longitude])
 */
fun checkLocation(latitude: Double, longitude: Double, function: (CheckLocationAction.() -> Unit)? = null): ActionData {
    val startTime = System.currentTimeMillis()
    val action = CheckLocationAction(latitude, longitude)
    function?.invoke(action)
    val result = action.execute()
    val parameters = action.getParameters()
    val name = action.getName()
    val stopTime = System.currentTimeMillis()
    return ActionData(result, parameters, name, startTime, stopTime)
}