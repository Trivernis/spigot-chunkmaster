package net.trivernis.chunkmaster.lib.generation

import io.papermc.lib.PaperLib
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.shapes.Shape
import org.bukkit.World
import java.util.concurrent.*

class DefaultGenerationTask(
    private val plugin: Chunkmaster,
    unloader: ChunkUnloader,
    world: World,
    startChunk: ChunkCoordinates,
    override val radius: Int = -1,
    shape: Shape,
    missingChunks: HashSet<ChunkCoordinates>,
    state: TaskState
) : GenerationTask(plugin, world, unloader, startChunk, shape, missingChunks, state) {

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
        this.shape.reset()
        val missedChunks = HashSet<ChunkCoordinates>()

        while (!cancelRun && !borderReached()) {
            val chunkCoordinates = nextChunkCoordinates
            triggerDynmapRender(chunkCoordinates)
            if (!PaperLib.isChunkGenerated(world, chunkCoordinates.x, chunkCoordinates.z)) {
                missedChunks.add(chunkCoordinates)
            }
        }
        this.missingChunks.addAll(missedChunks)
    }

    /**
     * Generates chunks that are missing
     */
    override fun generateMissing() {
        val missing = this.missingChunks.toHashSet()
        this.count = 0

        while (missing.size > 0 && !cancelRun) {
            if (plugin.mspt < msptThreshold && !unloader.isFull) {
                val chunk = missing.first()
                missing.remove(chunk)
                this.requestGeneration(chunk)
                this.count++
            } else {
                Thread.sleep(50L)
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
        do {
            lastChunkCoords = nextChunkCoordinates
            count = shape.count
        } while (PaperLib.isChunkGenerated(world, lastChunkCoords.x, lastChunkCoords.z))
    }

    /**
     * Generates the world until it encounters the worlds border
     */
    private fun generateUntilBorder() {
        var chunkCoordinates: ChunkCoordinates

        while (!cancelRun && !borderReached()) {
            if (plugin.mspt < msptThreshold && !unloader.isFull) {
                chunkCoordinates = nextChunkCoordinates
                requestGeneration(chunkCoordinates)

                lastChunkCoords = chunkCoordinates
                count = shape.count
            } else {
                Thread.sleep(50L)
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
        if (!PaperLib.isChunkGenerated(world, chunkCoordinates.x, chunkCoordinates.z) || PaperLib.isSpigot()) {
            val pendingChunkEntry = PendingChunkEntry(
                chunkCoordinates,
                PaperLib.getChunkAtAsync(world, chunkCoordinates.x, chunkCoordinates.z, true)
            )
            this.pendingChunks.put(pendingChunkEntry)
            pendingChunkEntry.chunk.thenAccept {
                this.unloader.add(it)
                this.pendingChunks.remove(pendingChunkEntry)
            }
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