package net.trivernis.chunkmaster.lib.database

import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates
import java.sql.ResultSet
import java.util.concurrent.CompletableFuture

class CompletedGenerationTasks(private val sqliteManager: SqliteManager) {
    /**
     * Returns the list of all completed tasks
     */
    fun getCompletedTasks(): CompletableFuture<List<CompletedGenerationTask>> {
        val completableFuture = CompletableFuture<List<CompletedGenerationTask>>()

        sqliteManager.executeStatement("SELECT * FROM completed_generation_tasks", HashMap()) { res ->
            val tasks = ArrayList<CompletedGenerationTask>()

            while (res!!.next()) {
                tasks.add(mapSqlResponseToWrapperObject(res))
            }
            completableFuture.complete(tasks)
        }
        return completableFuture
    }

    /**
     * Returns a list of completed tasks for a world
     */
    fun getCompletedTasksForWorld(world: String): CompletableFuture<List<CompletedGenerationTask>> {
        val completableFuture = CompletableFuture<List<CompletedGenerationTask>>()

        sqliteManager.executeStatement(
            "SELECT * FROM completed_generation_tasks WHERE world = ?",
            hashMapOf(1 to world)
        ) { res ->
            val tasks = ArrayList<CompletedGenerationTask>()

            while (res!!.next()) {
                tasks.add(mapSqlResponseToWrapperObject(res))
            }
            completableFuture.complete(tasks)
        }
        return completableFuture
    }

    private fun mapSqlResponseToWrapperObject(res: ResultSet): CompletedGenerationTask {
        val id = res.getInt("id")
        val world = res.getString("world")
        val center = ChunkCoordinates(res.getInt("center_x"), res.getInt("center_z"))
        val radius = res.getInt("completed_radius")
        val shape = res.getString("shape")
        return CompletedGenerationTask(id, world, radius, center, shape)
    }

    /**
     * Adds a completed task
     */
    fun addCompletedTask(
        id: Int,
        world: String,
        radius: Int,
        center: ChunkCoordinates,
        shape: String
    ): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        sqliteManager.executeStatement(
            "INSERT INTO completed_generation_tasks (id, world, completed_radius, center_x, center_z, shape) VALUES (?, ?, ?, ?, ?, ?)",
            hashMapOf(
                1 to id,
                2 to world,
                3 to radius,
                4 to center.x,
                5 to center.z,
                6 to shape,
            )
        ) {
            completableFuture.complete(null)
        }
        return completableFuture
    }
}