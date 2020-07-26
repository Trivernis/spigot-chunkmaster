package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import java.lang.Exception
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashSet

class ChunkUnloader(private val plugin: Chunkmaster): Runnable {
    private val maxLoadedChunks = plugin.config.getInt("generation.max-loaded-chunks")
    private val lock = ReentrantReadWriteLock()
    private var unloadingQueue = Vector<Chunk>(maxLoadedChunks)
    val isFull: Boolean
        get() {
            return pendingSize == maxLoadedChunks
        }

    val pendingSize: Int
        get() {
            lock.readLock().lock()
            val size = unloadingQueue.size
            lock.readLock().unlock()
            return size
        }

    /**
     * Unloads all chunks in the unloading queue with each run
     */
    override fun run() {
        lock.writeLock().lock()
        try {
            val chunkToUnload = unloadingQueue.toHashSet()

            for (chunk in chunkToUnload) {
                try {
                    chunk.unload(true)
                } catch (e: Exception) {
                    plugin.logger.severe(e.toString())
                }
            }
            unloadingQueue.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * Adds a chunk to unload to the queue
     */
    fun add(chunk: Chunk) {
        lock.writeLock().lockInterruptibly()
        try {
            unloadingQueue.add(chunk)
        } finally {
            lock.writeLock().unlock()
        }
    }
}