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

import config.ConfigLoader
import config.MainConfig
import logger.Logger
import test.TestListFactory
import java.util.concurrent.Executors


fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")

    val argsHashMap = parseArguments(args)
    var configurationFile = argsHashMap["-configuration"]
    if (configurationFile.isNullOrEmpty())
        configurationFile = "configuration.json"
    ConfigLoader().loadConfiguration(configurationFile)

    Class.forName(MainConfig.pageSource).getDeclaredConstructor().newInstance()

    val executor = Executors.newFixedThreadPool(MainConfig.threads)

    val testList = TestListFactory().getTestSource(MainConfig.testSource).getTestList()
    testList.forEach {
        val worker: Runnable = WorkerThread(it)
        executor.execute(worker)
    }

    executor.shutdown()
    while (!executor.isTerminated) {
    }
    Logger.info("Finished all threads")
}

fun parseArguments(args: Array<String>): HashMap<String, String> {
    val argsHashMap = HashMap<String, String>()
    args.forEach {
        val delimiterIndex = it.indexOf("=")
        argsHashMap[it.substring(0, delimiterIndex)] = it.substring(delimiterIndex + 1)
    }
    return argsHashMap
}