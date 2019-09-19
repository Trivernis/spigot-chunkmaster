package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import org.bukkit.World

class GenerationTaskSpigot(
    private val plugin: Chunkmaster, override val world: World,
    centerChunk: ChunkCoordinates, private val startChunk: ChunkCoordinates,
    override val stopAfter: Int = -1
) : GenerationTask(plugin, centerChunk, startChunk) {


    override var count = 0
        private set
    override var endReached: Boolean = false
        private set

    /**
     * Runs the generation task. Every Iteration the next chunk will be generated if
     * it hasn't been generated already.
     * After 10 chunks have been generated, they will all be unloaded and saved.
     */
    override fun run() {
        if (plugin.mspt < msptThreshold) {    // pause when tps < 2
            if (loadedChunks.size > maxLoadedChunks) {
                for (chunk in loadedChunks) {
                    if (chunk.isLoaded) {
                        chunk.unload(true)
                    }
                }
                loadedChunks.clear()
            } else {
                if (borderReached()) {
                    endReached = true
                    endReachedCallback?.invoke()
                    return
                }

                var chunk = lastChunkCoords

                if (!world.isChunkGenerated(chunk.x, chunk.z)) {
                    for (i in 0 until minOf(chunksPerStep, (stopAfter - count) - 1)) {
                        chunk = nextChunkCoordinates
                        val chunkInstance = world.getChunkAt(chunk.x, chunk.z)
                        chunkInstance.load(true)
                        loadedChunks.add(chunkInstance)
                    }
                }
                lastChunkCoords = chunk
                count = spiral.count // set the count to the more accurate spiral count
            }
        }
    }

    /**
     * Cancels the generation task.
     * This unloads all chunks that were generated but not unloaded yet.
     */
    override fun cancel() {
        for (chunk in loadedChunks) {
            if (chunk.isLoaded) {
                chunk.unload(true)
            }
        }
    }
}