package net.trivernis.chunkmaster.lib

import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import org.bukkit.World
import java.util.concurrent.CompletableFuture
import io.papermc.lib.PaperLib

class GenerationTaskPaper(
    private val plugin: Chunkmaster, override val world: World,
    centerChunk: Chunk, private val startChunk: Chunk,
    override val stopAfter: Int = -1
) : GenerationTask(plugin, centerChunk, startChunk) {

    private val pendingChunks = HashSet<CompletableFuture<Chunk>>()

    override var count = 0
        private set
    override var lastChunk: Chunk = startChunk
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
            if (loadedChunks.size > 10) {
                for (chunk in loadedChunks) {
                    if (chunk.isLoaded) {
                        chunk.unload(true)
                    }
                }
            } else if (pendingChunks.size < 10) {   // if more than 10 chunks are pending, wait.
                if (borderReached()) {
                    endReached = true
                    return
                }

                var chunk = nextChunk
                for (i in 1 until chunkSkips) {
                    if (PaperLib.isChunkGenerated(world, chunk.x, chunk.z)) {
                        chunk = nextChunk
                    } else {
                        break
                    }
                }

                if (!PaperLib.isChunkGenerated(world, chunk.x, chunk.z)) {
                    pendingChunks.add(PaperLib.getChunkAtAsync(world, chunk.x, chunk.z, true))
                }
                lastChunk = chunk
                count = spiral.count // set the count to the more accurate spiral count
            }
        }
        checkChunksLoaded()
    }

    /**
     * Cancels the generation task.
     * This unloads all chunks that were generated but not unloaded yet.
     */
    override fun cancel() {
        for (pendingChunk in pendingChunks) {
            if (pendingChunk.isDone) {
                loadedChunks.add(pendingChunk.get())
            } else {
                pendingChunk.cancel(true)
            }
        }
        pendingChunks.clear()
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