package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import java.lang.Exception
import java.util.*
import java.util.concurrent.*
import kotlin.collections.HashSet

class ChunkUnloader(private val plugin: Chunkmaster): Runnable {
    private val maxLoadedChunks = plugin.config.getInt("generation.max-loaded-chunks")
    private var unloadingQueue = Vector<Chunk>(maxLoadedChunks)
    val isFull: Boolean
    get() {
        return unloadingQueue.size == maxLoadedChunks
    }

    val pendingSize: Int
        get() {
            return unloadingQueue.size
        }

    /**
     * Unloads all chunks in the unloading queue with each run
     */
    override fun run() {
        val chunkToUnload = unloadingQueue.toHashSet()
        unloadingQueue.clear()

        for (chunk in chunkToUnload) {
            try {
                chunk.unload(true)
            } catch (e: Exception) {
                plugin.logger.severe(e.toString())
            }
        }
        unloadingQueue.clear()
    }

    /**
     * Adds a chunk to unload to the queue
     */
    fun add(chunk: Chunk) {
        unloadingQueue.add(chunk)
    }
}