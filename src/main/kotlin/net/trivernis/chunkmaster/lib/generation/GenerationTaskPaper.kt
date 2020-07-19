package net.trivernis.chunkmaster.lib.generation

import io.papermc.lib.PaperLib
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.shapes.Shape
import org.bukkit.Chunk
import org.bukkit.World
import java.lang.Exception
import java.util.concurrent.*
import kotlin.math.max

class GenerationTaskPaper(
    private val plugin: Chunkmaster,
    unloader: ChunkUnloader,
    override val world: World,
    startChunk: ChunkCoordinates,
    override val radius: Int = -1,
    shape: Shape
) : GenerationTask(plugin, unloader, startChunk, shape) {

    private val maxPendingChunks = plugin.config.getInt("generation.max-pending-chunks")
    private val maxChunks = Semaphore(this.maxPendingChunks)

    override var count = 0
    override var endReached: Boolean = false

    init {
        updateGenerationAreaMarker()
        count = shape.count
    }

    /**
     * Runs the generation task. Every Iteration the next chunks will be generated if
     * they haven't been generated already
     * After a configured number of chunks chunks have been generated, they will all be unloaded and saved.
     */
    override fun run() {
        var chunk: ChunkCoordinates
        do {
            chunk = nextChunkCoordinates
        } while (world.isChunkGenerated(chunk.x, chunk.z));

        while (!cancel && !borderReachedCheck()) {
            if (plugin.mspt < msptThreshold) {
                chunk = nextChunkCoordinates

                if (!world.isChunkGenerated(chunk.x, chunk.z)) {
                    maxChunks.acquireUninterruptibly()
                    world.getChunkAtAsync(chunk.x, chunk.z, true).thenAccept {
                        this.unloader.add(it)
                        maxChunks.release()
                    }
                }
                this.lastChunkCoords = chunk
                this.count = shape.count
            }
        }
        println("Waiting for tasks to finish...")
        maxChunks.acquireUninterruptibly(this.maxPendingChunks)

        println("Task stopped")
    }

    /**
     * Cancels the generation task.
     * This unloads all chunks that were generated but not unloaded yet.
     */
    override fun cancel() {
        this.cancel = true
        maxChunks.acquireUninterruptibly(this.maxPendingChunks)
        updateGenerationAreaMarker(true)
        updateLastChunkMarker(true)
    }
}