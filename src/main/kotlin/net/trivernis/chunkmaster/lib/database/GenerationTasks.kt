package net.trivernis.chunkmaster.lib.database

import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates
import net.trivernis.chunkmaster.lib.generation.TaskState
import java.util.concurrent.CompletableFuture

class GenerationTasks(private val sqliteManager: SqliteManager) {
    /**
     * Returns all stored generation tasks
     */
    fun getGenerationTasks(): CompletableFuture<List<GenerationTaskData>> {
        val completableFuture = CompletableFuture<List<GenerationTaskData>>()

        sqliteManager.executeStatement("SELECT * FROM generation_tasks", HashMap()) { res ->
            val tasks = ArrayList<GenerationTaskData>()

            while (res!!.next()) {
                val id = res.getInt("id")
                val world = res.getString("world")
                val center = ChunkCoordinates(res.getInt("center_x"), res.getInt("center_z"))
                val last = ChunkCoordinates(res.getInt("last_x"), res.getInt("last_z"))
                val radius = res.getInt("radius")
                val shape = res.getString("shape")
                val state = stringToState(res.getString("state"))
                val taskData = GenerationTaskData(id, world, radius, shape, state, center, last)
                if (tasks.find { it.id == id } == null) {
                    tasks.add(taskData)
                }
            }
            completableFuture.complete(tasks)
        }
        return completableFuture
    }

    /**
     * Adds a generation task to the database
     */
    fun addGenerationTask(world: String, center: ChunkCoordinates, radius: Int, shape: String): CompletableFuture<Int> {
        val completableFuture = CompletableFuture<Int>()
        sqliteManager.executeStatement("""
            INSERT INTO generation_tasks (center_x, center_z, last_x, last_z, world, radius, shape)
            values (?, ?, ?, ?, ?, ?, ?)""",
            hashMapOf(
                1 to center.x,
                2 to center.z,
                3 to center.x,
                4 to center.z,
                5 to world,
                6 to radius,
                7 to shape
            )
        ) {
            sqliteManager.executeStatement(
                """
                SELECT id FROM generation_tasks ORDER BY id DESC LIMIT 1
                """.trimIndent(), HashMap()
            ) {
                it!!.next()
                completableFuture.complete(it.getInt("id"))
            }
        }
        return completableFuture
    }

    /**
     * Deletes a generationTask from the database
     */
    fun deleteGenerationTask(id: Int): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        sqliteManager.executeStatement("DELETE FROM generation_tasks WHERE id = ?;", hashMapOf(1 to id)) {
            completableFuture.complete(null)
        }
        return completableFuture
    }

    fun updateGenerationTask(id: Int, last: ChunkCoordinates, state: TaskState): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        sqliteManager.executeStatement(
            """
            UPDATE generation_tasks SET last_x = ?, last_z = ?, state = ?
            WHERE id = ?
            """.trimIndent(),
            hashMapOf(1 to last.x, 2 to last.z, 3 to id, 4 to state.toString())
        ) {
            completableFuture.complete(null)
        }
        return completableFuture
    }

    /**
     * Converts a string into a task state
     */
    private fun stringToState(stringState: String): TaskState {
        TaskState.valueOf(stringState)
        return when (stringState) {
            "GENERATING" -> TaskState.GENERATING
            "VALIDATING" -> TaskState.VALIDATING
            "PAUSING" -> TaskState.PAUSING
            "CORRECTING" -> TaskState.CORRECTING
            else -> TaskState.GENERATING
        }
    }
}