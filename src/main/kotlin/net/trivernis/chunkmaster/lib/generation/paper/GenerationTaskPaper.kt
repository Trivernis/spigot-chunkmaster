package net.trivernis.chunkmaster.lib.generation.paper

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates
import net.trivernis.chunkmaster.lib.generation.ChunkUnloader
import net.trivernis.chunkmaster.lib.generation.GenerationTask
import net.trivernis.chunkmaster.lib.generation.TaskState
import net.trivernis.chunkmaster.lib.shapes.Shape
import org.bukkit.World
import java.util.concurrent.*

class GenerationTaskPaper(
    private val plugin: Chunkmaster,
    unloader: ChunkUnloader,
    override val world: World,
    startChunk: ChunkCoordinates,
    override val radius: Int = -1,
    shape: Shape,
    previousPendingChunks: List<ChunkCoordinates>,
    state: TaskState
) : GenerationTask(plugin, unloader, startChunk, shape, previousPendingChunks, state) {

    private val maxPendingChunks = plugin.config.getInt("generation.max-pending-chunks")
    val pendingChunks = ArrayBlockingQueue<PendingChunkEntry>(maxPendingChunks)

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
    override fun generate() {
        try {
            for (pending in this.previousPendingChunks) {
                this.requestGeneration(pending)
            }

            seekGenerated()
            generateUntilBorder()
        } catch (_: InterruptedException){}
    }

    /**
     * Validates that all chunks have been generated or generates missing ones
     * TODO: Just validate and store information about missing chunks.
     *       Generate missing chunks later
     */
    override fun validate() {
        generateUntilBorder()
    }

    /**
     * Seeks until it encounters a chunk that hasn't been generated yet
     */
    private fun seekGenerated() {
        var chunkCoordinates: ChunkCoordinates
        do {
            chunkCoordinates = nextChunkCoordinates
        } while (world.isChunkGenerated(chunkCoordinates.x, chunkCoordinates.z));
        lastChunkCoords = chunkCoordinates
    }

    private fun generateUntilBorder() {
        var chunkCoordinates: ChunkCoordinates
        while (!cancelRun && !borderReachedCheck()) {
            if (plugin.mspt < msptThreshold) {
                chunkCoordinates = nextChunkCoordinates
                this.requestGeneration(chunkCoordinates)

                this.lastChunkCoords = chunkCoordinates
                this.count = shape.count
            }
        }
    }

    /**
     * Request the generation of a chunk
     */
    private fun requestGeneration(chunkCoordinates: ChunkCoordinates) {
        if (!world.isChunkGenerated(chunkCoordinates.x, chunkCoordinates.z)) {
            val pendingChunkEntry = PendingChunkEntry(chunkCoordinates, world.getChunkAtAsync(chunkCoordinates.x, chunkCoordinates.z, true))
            pendingChunkEntry.chunk.thenAccept {
                this.unloader.add(it)
                this.pendingChunks.remove(pendingChunkEntry)
            }
            this.pendingChunks.put(pendingChunkEntry)
        }
    }

    /**
     * Cancels the generation task.
     * This unloads all chunks that were generated but not unloaded yet.
     */
    override fun cancel() {
        this.cancelRun = true
        this.pendingChunks.forEach { it.chunk.cancel(false) }
        updateGenerationAreaMarker(true)
    }
}