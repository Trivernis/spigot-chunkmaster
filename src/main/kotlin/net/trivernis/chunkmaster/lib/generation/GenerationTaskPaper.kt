package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.shapes.Shape
import org.bukkit.Chunk
import org.bukkit.World
import java.util.concurrent.CompletableFuture

class GenerationTaskPaper(
    private val plugin: Chunkmaster,
    override val world: World,
    startChunk: ChunkCoordinates,
    override val radius: Int = -1,
    shape: Shape
) : GenerationTask(plugin, startChunk, shape) {

    private val maxPendingChunks = plugin.config.getInt("generation.max-pending-chunks")

    private val pendingChunks = HashSet<CompletableFuture<Chunk>>()

    override var count = 0
    override var endReached: Boolean = false

    init {
        updateGenerationAreaMarker()
    }

    /**
     * Runs the generation task. Every Iteration the next chunks will be generated if
     * they haven't been generated already
     * After a configured number of chunks chunks have been generated, they will all be unloaded and saved.
     */
    override fun run() {
        if (plugin.mspt < msptThreshold) {
            if (loadedChunks.size > maxLoadedChunks) {
                unloadLoadedChunks()
            } else if (pendingChunks.size < maxPendingChunks) {
                if (borderReachedCheck()) return

                var chunk = nextChunkCoordinates
                for (i in 0 until chunkSkips) {
                    if (world.isChunkGenerated(chunk.x, chunk.z)) {
                        chunk = nextChunkCoordinates
                    } else {
                        break
                    }
                }

                if (!world.isChunkGenerated(chunk.x, chunk.z)) {
                    for (i in 0 until chunksPerStep) {
                        if (borderReached()) break
                        if (!world.isChunkGenerated(chunk.x, chunk.z)) {
                            pendingChunks.add(world.getChunkAtAsync(chunk.x, chunk.z, true))
                        }
                        chunk = nextChunkCoordinates
                    }
                    if (!world.isChunkGenerated(chunk.x, chunk.z)) {
                        pendingChunks.add(world.getChunkAtAsync(chunk.x, chunk.z, true))
                    }
                }
                lastChunkCoords = chunk
                count = shape.count // set the count to the more accurate spiral count
            }
        }
        checkChunksLoaded()
    }

    /**
     * Cancels the generation task.
     * This unloads all chunks that were generated but not unloaded yet.
     */
    override fun cancel() {
        updateGenerationAreaMarker(true)
        updateLastChunkMarker(true)
        unloadAllChunks()
    }

    /**
     * Cancels all pending chunks and unloads all loaded chunks.
     */
    private fun unloadAllChunks() {
        for (pendingChunk in pendingChunks) {
            if (pendingChunk.isDone) {
                loadedChunks.add(pendingChunk.get())
            } else {
                pendingChunk.cancel(true)
            }
        }
        pendingChunks.clear()
        if (loadedChunks.isNotEmpty()) {
            lastChunkCoords = ChunkCoordinates(loadedChunks.last().x, loadedChunks.last().z)
        }
        for (chunk in loadedChunks) {
            if (chunk.isLoaded) {
                chunk.unload(true)
            }
        }
    }

    /**
     * Checks if some chunks have been loaded and adds them to the loaded chunk set.
     */
    private fun checkChunksLoaded() {
        val completedEntrys = HashSet<CompletableFuture<Chunk>>()
        for (pendingChunk in pendingChunks) {
            if (pendingChunk.isDone) {
                completedEntrys.add(pendingChunk)
                loadedChunks.add(pendingChunk.get())
            } else if (pendingChunk.isCompletedExceptionally || pendingChunk.isCancelled) {
                completedEntrys.add(pendingChunk)
            }
        }
        pendingChunks.removeAll(completedEntrys)
    }
}