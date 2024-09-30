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

package reporter.database

import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp


object ProjectsTable : Table("projects") {
    val idRow: Column<Long> = long("id").autoIncrement()
    val nameRow: Column<String> = varchar("name", length = 100).uniqueIndex()
    val descriptionRow: Column<String?> = varchar("description", length = 500).nullable()

    override val primaryKey = PrimaryKey(idRow)
}

object LaunchesTable : Table("launches") {
    val idRow: Column<Long> = long("id")
    val projectIdRow: Column<Long> = (long("project_id") references ProjectsTable.idRow)
    val threadsRow: Column<Int> = integer("threads")
    val testsRow: Column<Int> = integer("tests")

    override val primaryKey = PrimaryKey(idRow)
}

object TestsTable : Table("tests") {
    val idRow: Column<Long> = long("id").autoIncrement()
    val projectIdRow: Column<Long> = (long("project_id") references ProjectsTable.idRow)
    val launchIdRow: Column<Long> = (long("launch_id")  references LaunchesTable.idRow)
    val identifierRow: Column<String> = varchar("identifier", length = 100).index()
    val nameRow: Column<String> = varchar("name", length = 200)
    val startTimeRow: Column<Instant> = timestamp("start_time")
    val endTimeRow: Column<Instant?> = timestamp("end_time").nullable()

    override val primaryKey = PrimaryKey(idRow)
}

object StepsTable : Table("steps") {
    val idRow: Column<Long> = long("id").autoIncrement()
    val testIdRow: Column<Long> = (long("test_id") references TestsTable.idRow)
    val parentStepIdRow: Column<Long?> = long("parent_step_id").nullable()
    val identifierRow: Column<String> = varchar("identifier", length = 200).index()
    val nameRow: Column<String> = varchar("name", length = 500)
    val statusRow: Column<String?> = varchar("status", length = 6).nullable()
    val messageRow: Column<String?> = varchar("message", length = 4000).nullable()
    val traceRow: Column<String?> = varchar("trace", length = 10000).nullable()
    val startTimeRow: Column<Instant> = timestamp("start_time")
    val endTimeRow: Column<Instant> = timestamp("end_time")

    override val primaryKey = PrimaryKey(idRow)
}

object StepParametersTable : Table("step_parameters") {
    val stepIdRow: Column<Long> = (long("step_id") references StepsTable.idRow).index()
    val nameRow: Column<String> = varchar("name", length = 100)
    val valueRow: Column<String> = varchar("value", length = 2000)
}

object ErrorImagesTable : Table("error_images") {
    val stepIdRow: Column<Long> = (long("step_id") references StepsTable.idRow).uniqueIndex()
    val imageRow: Column<ByteArray> = binary("image")
}

object ImageComparisonInfoTable : Table("image_comparison_info") {
    val stepIdRow: Column<Long> = (long("step_id") references StepsTable.idRow).uniqueIndex()
    val templateImagePathRow: Column<String?> = varchar("template_image_path", length = 250).nullable()
    val currentImagePathRow: Column<String?> = varchar("current_image_path", length = 250).nullable()
    val markedImagePathRow: Column<String?> = varchar("marked_image_path", length = 250).nullable()
}