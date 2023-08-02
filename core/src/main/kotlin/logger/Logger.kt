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

package logger

import config.MainConfig
import java.text.SimpleDateFormat
import java.util.*

class Logger {
    companion object {
        fun debug(logText: String, logSource: String? = null) {
            printLog(logText, LogLevel.DEBUG, logSource)
        }

        fun info(logText: String, logSource: String? = null) {
            printLog(logText, LogLevel.INFO, logSource)
        }

        fun warning(logText: String, logSource: String? = null) {
            printLog(logText, LogLevel.WARN, logSource)
        }

        fun error(logText: String, logSource: String? = null) {
            printLog(logText, LogLevel.ERROR, logSource)
        }

        private fun printLog(logText: String, logLevel: LogLevel, logSource: String?) {
            if (MainConfig.consoleLogLevel > logLevel.value)
                return

            val ansiColor = when (logLevel) {
                LogLevel.DEBUG -> Color.ANSI_WHITE
                LogLevel.INFO -> Color.ANSI_BLUE
                LogLevel.WARN -> Color.ANSI_YELLOW
                LogLevel.ERROR -> Color.ANSI_RED
            }
            var source = ""
            if (logSource != null)
                source = "[$logSource] "
            println("${getTime()} ${getThreadId()} $ansiColor$logLevel --> $source$logText${Color.ANSI_RESET}")
        }

        fun getTime(): String? {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
        }

        fun getThreadId(): String {
            return "[#${String.format("%02d", Thread.currentThread().id)}]"
        }
    }
}