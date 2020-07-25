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
    missingChunks: HashSet<ChunkCoordinates>,
    state: TaskState
) : GenerationTask(plugin, unloader, startChunk, shape, missingChunks, state) {

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
        generateMissing()
        seekGenerated()
        generateUntilBorder()
    }

    /**
     * Validates that all chunks have been generated or generates missing ones
     */
    override fun validate() {
        this.state = TaskState.VALIDATING
        this.shape.reset()
        val missedChunks = HashSet<ChunkCoordinates>()

        while (!cancelRun && !borderReached()) {
            val chunkCoordinates = nextChunkCoordinates
            triggerDynmapRender(chunkCoordinates)
            if (!world.isChunkGenerated(chunkCoordinates.x, chunkCoordinates.z)) {
                missedChunks.add(chunkCoordinates)
            }
        }
        this.missingChunks.addAll(missedChunks)
    }

    /**
     * Generates chunks that are missing
     */
    override fun generateMissing() {
        this.state = TaskState.CORRECTING
        val missing = this.missingChunks.toHashSet()
        this.count = 0
        for (chunk in missing) {
            this.requestGeneration(chunk)
            this.count++
            this.missingChunks.remove(chunk)
            if (this.cancelRun) {
                break
            }
        }
        if (!cancelRun) {
            this.joinPending()
        }
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

    /**
     * Generates the world until it encounters the worlds border
     */
    private fun generateUntilBorder() {
        this.state = TaskState.GENERATING
        var chunkCoordinates: ChunkCoordinates

        while (!cancelRun && !borderReached()) {
            if (plugin.mspt < msptThreshold) {
                chunkCoordinates = nextChunkCoordinates
                this.requestGeneration(chunkCoordinates)

                this.lastChunkCoords = chunkCoordinates
                this.count = shape.count
            }
        }
        if (!cancelRun) {
            joinPending()
        }
    }

    private fun joinPending() {
        while (!this.pendingChunks.isEmpty()) {
            Thread.sleep(msptThreshold)
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