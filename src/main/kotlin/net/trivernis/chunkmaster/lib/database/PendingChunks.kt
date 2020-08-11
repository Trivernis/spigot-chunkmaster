package net.trivernis.chunkmaster.lib.database

import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

class PendingChunks(private val sqliteManager: SqliteManager) {

    private val insertionCount = 300
    /**
     * Returns a list of pending chunks for a taskId
     */
    fun getPendingChunks(taskId: Int): CompletableFuture<List<ChunkCoordinates>> {
        val completableFuture = CompletableFuture<List<ChunkCoordinates>>()
        sqliteManager.executeStatement("SELECT * FROM pending_chunks WHERE task_id = ?", hashMapOf(1 to taskId)) {
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

    fun addPendingChunks(taskId: Int, pendingChunks: List<ChunkCoordinates>): CompletableFuture<Void> {
        val futures = ArrayList<CompletableFuture<Void>>()
        val statementCount = ceil(pendingChunks.size.toDouble() / insertionCount).toInt()

        for (i in 0 until statementCount) {
            futures.add(insertPendingChunks(taskId, pendingChunks.subList(i * insertionCount, ((i * insertionCount) + insertionCount).coerceAtMost(pendingChunks.size))))
        }

        if (futures.size > 0) {
            return CompletableFuture.allOf(*futures.toTypedArray())
        } else {
            return CompletableFuture.supplyAsync { null }
        }
    }

    /**
     * Adds pending chunks for a taskid
     */
    private fun insertPendingChunks(taskId: Int, pendingChunks: List<ChunkCoordinates>): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        if (pendingChunks.isEmpty()) {
            completableFuture.complete(null)
        } else {
            var sql = "INSERT INTO pending_chunks (task_id, chunk_x, chunk_z) VALUES"
            var index = 1
            val valueMap = HashMap<Int, Any>()

            for (coordinates in pendingChunks) {
                sql += "(?, ?, ?),"
                valueMap[index++] = taskId
                valueMap[index++] = coordinates.x
                valueMap[index++] = coordinates.z
            }
            sqliteManager.executeStatement(sql.removeSuffix(","), valueMap) {
                completableFuture.complete(null)
            }
        }
        return completableFuture
    }
}