package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import java.lang.Exception
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashSet

class ChunkUnloader(private val plugin: Chunkmaster): Runnable {
    private var unloadingQueue = ArrayBlockingQueue<Chunk>(plugin.config.getInt("generation.max-loaded-chunks"))
    val pendingSize: Int
        get() {
            return unloadingQueue.size
        }

    /**
     * Unloads all chunks in the unloading queue with each run
     */
    override fun run() {
        val chunkToUnload = HashSet<Chunk>()
        unloadingQueue.drainTo(chunkToUnload)
        for (chunk in chunkToUnload) {
            try {
                chunk.unload(true)
            } catch (e: Exception) {
                plugin.logger.severe(e.toString())
            }
        }
    }

    /**
     * Adds a chunk to unload to the queue
     */
    fun add(chunk: Chunk) {
        unloadingQueue.add(chunk)
    }
}