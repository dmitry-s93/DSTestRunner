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

plugins {
    kotlin("jvm") version "2.0.10"
    application
}

repositories {
    mavenCentral()
}

val seleniumJavaVersion = "4.23.0"
val appiumJavaClientVersion = "9.3.0"
val jsonVersion = "20240303"
val commonsIOVersion = "2.16.1"
val awaitilityVersion = "4.2.1"
val openCsvVersion = "5.9"
val httpClient5Version = "5.3.1"
val postgresqlVersion = "42.7.3"
val rgxgenVersion = "1.4"
val exposedVersion = "0.53.0"

dependencies {
    // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
    implementation("org.seleniumhq.selenium:selenium-java:$seleniumJavaVersion")
    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:$jsonVersion")
    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:$commonsIOVersion")
    // https://mvnrepository.com/artifact/org.awaitility/awaitility
    implementation("org.awaitility:awaitility:$awaitilityVersion")
    // https://mvnrepository.com/artifact/com.opencsv/opencsv
    implementation("com.opencsv:opencsv:$openCsvVersion")
    // https://mvnrepository.com/artifact/org.apache.httpcomponents.client5/httpclient5
    implementation("org.apache.httpcomponents.client5:httpclient5:$httpClient5Version")
    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    // https://mvnrepository.com/artifact/com.github.curious-odd-man/rgxgen
    implementation("com.github.curious-odd-man:rgxgen:$rgxgenVersion")
    // https://github.com/pazone/ashot
    implementation(files("libs/ashot-1.6.1-e1f84e5-modified.jar"))
    // https://mvnrepository.com/artifact/io.appium/java-client
    implementation("io.appium:java-client:$appiumJavaClientVersion")
    // https://mvnrepository.com/artifact/org.jetbrains.exposed/exposed-core
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    // https://mvnrepository.com/artifact/org.jetbrains.exposed/exposed-kotlin-datetime
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
}