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

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.util.UUID


object ProjectsTable : Table("projects") {
    val idRow: Column<UUID> = uuid("id")
    val nameRow: Column<String> = varchar("name", length = 100).uniqueIndex()
    val descriptionRow: Column<String?> = varchar("description", length = 500).nullable()
    val templatesBaseDirRow: Column<String?> = text("templates_base_dir").nullable()
    val currentScreenshotsBaseDirRow: Column<String?> = text("current_screenshots_base_dir").nullable()
    val versionRow: Column<Long> = long("version").default(1)
    val createdAtRow: Column<Instant> = timestamp("created_at")
    val updatedAtRow: Column<Instant> = timestamp("updated_at")

    override val primaryKey = PrimaryKey(idRow)
}

object RunsTable : Table("runs") {
    val idRow: Column<UUID> = uuid("id")
    val projectIdRow: Column<UUID> = uuid("project_id") references ProjectsTable.idRow
    val nameRow: Column<String?> = varchar("name", length = 255).nullable()
    val startedAtRow: Column<Instant> = timestamp("started_at")
    val finishedAtRow: Column<Instant?> = timestamp("finished_at").nullable()
    val threadsRow: Column<Int> = integer("threads")
    val testsRow: Column<Int> = integer("tests")
    val versionRow: Column<Long> = long("version").default(1)
    val createdAtRow: Column<Instant> = timestamp("created_at")
    val updatedAtRow: Column<Instant> = timestamp("updated_at")

    override val primaryKey = PrimaryKey(idRow)
}

object TestsTable : Table("tests") {
    val idRow: Column<UUID> = uuid("id")
    val projectIdRow: Column<UUID> = uuid("project_id") references ProjectsTable.idRow
    val runIdRow: Column<UUID> = uuid("run_id") references RunsTable.idRow
    val identifierRow: Column<String> = varchar("identifier", length = 255)
    val nameRow: Column<String> = varchar("name", length = 500)
    val startTimeRow: Column<Instant> = timestamp("start_time")
    val endTimeRow: Column<Instant> = timestamp("end_time")

    override val primaryKey = PrimaryKey(idRow)
}

object StepsTable : Table("steps") {
    val idRow: Column<UUID> = uuid("id")
    val testIdRow: Column<UUID> = uuid("test_id") references TestsTable.idRow
    val parentStepIdRow: Column<UUID?> = optReference("parent_step_id", idRow)
    val identifierRow: Column<String> = varchar("identifier", length = 255)
    val nameRow: Column<String> = varchar("name", length = 500)
    val statusRow: Column<String> = varchar("status", length = 16)
    val screenshotMismatchRow: Column<Boolean> = bool("screenshot_mismatch").default(false)
    val messageRow: Column<String?> = text("message").nullable()
    val traceRow: Column<String?> = text("trace").nullable()
    val startTimeRow: Column<Instant> = timestamp("start_time")
    val endTimeRow: Column<Instant> = timestamp("end_time")

    override val primaryKey = PrimaryKey(idRow)
}

object StepParametersTable : Table("step_parameters") {
    val stepIdRow: Column<UUID> = (uuid("step_id") references StepsTable.idRow).index()
    val nameRow: Column<String> = varchar("name", length = 255)
    val valueRow: Column<String?> = text("value").nullable()
}

object ErrorImagesTable : Table("error_images") {
    val stepIdRow: Column<UUID> = (uuid("step_id") references StepsTable.idRow).uniqueIndex()
    val imageRow: Column<ByteArray> = binary("image")
    val createdAtRow: Column<Instant> = timestamp("created_at").clientDefault { Clock.System.now() }
}

object ImageComparisonInfoTable : Table("image_comparison_info") {
    val stepIdRow: Column<UUID> = (uuid("step_id") references StepsTable.idRow).uniqueIndex()
    val currentImagePathRow: Column<String> = text("current_image_path")
    val templateImagePathRow: Column<String?> = text("template_image_path").nullable()
    val markedImagePathRow: Column<String?> = text("marked_image_path").nullable()
}
