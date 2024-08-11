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

package reporter

import config.MainConfig
import logger.Logger


class ReporterSession {
    companion object {
        private val reporterSessions = ThreadLocal<List<Reporter>>()

        @Synchronized
        fun createSession() {
            val reporters: MutableList<Reporter> = mutableListOf()
            MainConfig.reporterImpl.forEach {
                try {
                    reporters.add(ReporterFactory().createReporter(it))
                } catch (e: Exception) {
                    Logger.error("Failed to create reporter session for '$it'", "createSession")
                }
            }
            if (reporters.isEmpty()) {
                Logger.error("Test results will not be saved", "createSession")
            }
            reporterSessions.set(reporters)
        }

        fun getSession(): List<Reporter> {
            if (reporterSessions.get() == null)
                Logger.error("Reporter session not created", "getSession")
            return reporterSessions.get()
        }

        fun closeSession() {
            if (reporterSessions.get() != null) {
                reporterSessions.get().forEach { session ->
                    session.quit()
                }
                reporterSessions.set(null)
            } else {
                Logger.warning("Reporter session not created", "closeSession")
            }
        }
    }
}