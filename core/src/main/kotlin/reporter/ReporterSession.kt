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
        private var reporterSession = ThreadLocal<Reporter>()

        fun createSession() {
            try {
                reporterSession.set(ReporterFactory().createReporter(MainConfig.getReporterImpl()))
            } catch (e: Exception) {
                Logger.error("Failed to create reporter session\n${e.cause}", "createSession")
            }
        }

        fun getSession(): Reporter {
            if (reporterSession.get() == null)
                Logger.error("Reporter session not created", "getSession")
            return reporterSession.get()
        }

        fun closeSession() {
            if (reporterSession.get() != null) {
                reporterSession.get().quit()
                reporterSession.set(null)
            } else
                Logger.warning("Reporter session not created", "closeSession")
        }
    }
}