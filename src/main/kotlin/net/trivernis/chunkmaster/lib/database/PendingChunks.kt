package net.trivernis.chunkmaster.lib.database

import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates
import java.util.concurrent.CompletableFuture

class PendingChunks(private val sqliteManager: SqliteManager) {
    /**
     * Returns a list of pending chunks for a taskId
     */
    fun getPendingChunks(taskId: Int): CompletableFuture<List<ChunkCoordinates>> {
        val completableFuture = CompletableFuture<List<ChunkCoordinates>>()
        sqliteManager.executeStatement("SELECT * FROM pending_chunks WHERE task_id = ?",hashMapOf(1 to taskId)) {
            val pendingChunks = ArrayList<ChunkCoordinates>()
            while (it!!.next()) {
                pendingChunks.add(ChunkCoordinates(it.getInt("chunk_x"), it.getInt("chunk_z")))
            }
            completableFuture.complete(pendingChunks)
        }
        return completableFuture
    }

    /**
     * Clears all pending chunks of a task
     */
    fun clearPendingChunks(taskId: Int): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        sqliteManager.executeStatement("DELETE FROM pending_chunks WHERE task_id = ?", hashMapOf(1 to taskId)) {
            completableFuture.complete(null)
        }
        return completableFuture
    }

    /**
     * Adds pending chunks for a taskid
     */
    fun addPendingChunks(taskId: Int, pendingChunks: List<ChunkCoordinates>): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        var sql = "INSERT INTO pending_chunks (task_id, chunk_x, chunk_z) VALUES"
        var index = 1
        val valueMap= HashMap<Int, Any>()

        for (coordinates in pendingChunks) {
            sql += "(?, ?, ?),"
            valueMap[index++] = taskId
            valueMap[index++] = coordinates.x
            valueMap[index++] = coordinates.z
        }
        sqliteManager.executeStatement(sql.removeSuffix(","), valueMap) {
            completableFuture.complete(null)
        }
        return completableFuture
    }
}