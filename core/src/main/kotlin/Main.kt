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
import storage.SessionParametersStorage
import test.TestBuilder
import test.TestListFactory
import java.util.concurrent.Executors
import kotlin.reflect.KClass


fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")

    val argsHashMap = parseArguments(args)
    ConfigLoader().loadConfiguration(getConfigurationFile(argsHashMap))

    val threadCount = getThreadCount(argsHashMap)
    val testList = getTestList(getSpecifiedTests(argsHashMap))

    SessionParametersStorage.threadCount = threadCount
    SessionParametersStorage.testCount = testList.size

    executeTests(testList, threadCount)
}

private fun parseArguments(args: Array<String>): HashMap<String, String> {
    val argsHashMap = HashMap<String, String>()
    args.forEach {
        val delimiterIndex = it.indexOf("=")
        argsHashMap[it.substring(0, delimiterIndex)] = it.substring(delimiterIndex + 1)
    }
    return argsHashMap
}

private fun getConfigurationFile(argsHashMap: HashMap<String, String>): String {
    val configurationFile = argsHashMap["-configuration"]
    if (configurationFile.isNullOrEmpty())
        return "configuration.json"
    return configurationFile
}

private fun getThreadCount(argsHashMap: HashMap<String, String>): Int {
    return argsHashMap["-threads"]?.toInt() ?: MainConfig.threads
}

private fun getSpecifiedTests(argsHashMap: HashMap<String, String>):  List<String>? {
    return argsHashMap["-tests"]?.split(",")
}

private fun getTestList(specifiedTests: List<String>?): List<KClass<out TestBuilder>> {
    val fullTestList = TestListFactory().getTestSource(MainConfig.testSource).getTestList()
    if (specifiedTests == null)
        return fullTestList
    val resTestList: MutableList<KClass<out TestBuilder>> = ArrayList()
    specifiedTests.forEach { test ->
        fullTestList.forEach {
            if (it.simpleName == test)
                resTestList.add(it)
        }
    }
    return resTestList
}

private fun executeTests(testList: List<KClass<out TestBuilder>>, threads: Int) {
    Logger.info("Number of tests to run: ${testList.size}")
    val executor = Executors.newFixedThreadPool(threads)
    testList.forEach {
        val worker: Runnable = WorkerThread(it.qualifiedName!!)
        executor.execute(worker)
    }
    executor.shutdown()
    while (!executor.isTerminated) { }
    Logger.info("Finished all threads")
}