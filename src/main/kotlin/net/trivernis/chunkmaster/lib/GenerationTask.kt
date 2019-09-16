package net.trivernis.chunkmaster.lib

import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class GenerationTask(private val plugin: Chunkmaster, val world: World,
                     private val centerChunk: Chunk, val startChunk: Chunk): Runnable {
    private val spiral: Spiral = Spiral(Pair(centerChunk.x, centerChunk.z), Pair(startChunk.x, startChunk.z))
    private val loadedChunks: HashSet<Chunk> = HashSet()
    var count = 0
        get() = field
    var lastChunk: Chunk = startChunk
        get() = field

    /**
     * Runs the generation task. Every Iteration the next chunk will be generated if
     * it hasn't been generated already.
     */
    override fun run() {
        if (plugin.mspt < 500L) {    // pause when tps < 2
            if (loadedChunks.size > 10) {
                for (chunk in loadedChunks) {
                    if (chunk.isLoaded) {
                        chunk.unload(true)
                    }
                }
            } else {
                val nextChunkCoords = spiral.next()
                val chunk = world.getChunkAt(nextChunkCoords.first, nextChunkCoords.second)

                if (!world.isChunkGenerated(chunk.x, chunk.z)) {
                    chunk.load(true)
                    loadedChunks.add(chunk)
                }
                lastChunk = chunk
                count++
            }
        }
    }

    /**
     * Cancels the generation task.
     * This unloads all chunks that were generated but not unloaded yet.
     */
    fun cancel() {
        for (chunk in loadedChunks) {
            if (chunk.isLoaded) {
                chunk.unload(true)
            }
        }
    }
}