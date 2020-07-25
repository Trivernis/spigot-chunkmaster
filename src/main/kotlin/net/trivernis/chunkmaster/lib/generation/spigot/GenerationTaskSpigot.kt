package net.trivernis.chunkmaster.lib.generation.spigot

import io.papermc.lib.PaperLib
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates
import net.trivernis.chunkmaster.lib.generation.ChunkUnloader
import net.trivernis.chunkmaster.lib.generation.GenerationTask
import net.trivernis.chunkmaster.lib.generation.TaskState
import net.trivernis.chunkmaster.lib.shapes.Shape
import org.bukkit.World

class GenerationTaskSpigot(
    private val plugin: Chunkmaster,
    unloader: ChunkUnloader,
    override val world: World,
    startChunk: ChunkCoordinates,
    override val radius: Int = -1,
    shape: Shape,
    missingChunks: HashSet<ChunkCoordinates>,
    state: TaskState
) : GenerationTask(plugin, unloader, startChunk, shape, missingChunks, state) {


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
        isRunning = true
        try {
            this.generateMissing()
            this.state = TaskState.GENERATING
            while (!cancelRun && !borderReached()) {
                if (plugin.mspt < msptThreshold) {
                    val chunkCoordinates = nextChunkCoordinates
                    val chunkInstance = world.getChunkAt(chunkCoordinates.x, chunkCoordinates.z)
                    chunkInstance.load(true)
                    unloader.add(chunkInstance)

                    lastChunkCoords = chunkCoordinates
                    count = shape.count
                }
            }
        } catch (_: InterruptedException) {
        }
        isRunning = false
    }

    /**
     * Generates all chunks that were missed
     */
    override fun generateMissing() {
        this.state = TaskState.CORRECTING
        for (pending in this.missingChunks) {
            val chunkInstance = world.getChunkAt(pending.x, pending.z)
            chunkInstance.load(true)
            unloader.add(chunkInstance)
            this.lastChunkCoords = pending
            this.count = this.missingChunks.indexOf(pending)
        }
    }

    /**
     * Validates if all chunks have been generated correctly
     */
    override fun validate() {
        this.state = TaskState.VALIDATING
        this.shape.reset()
        val missedChunks = HashSet<ChunkCoordinates>()

        while (!this.cancelRun && !this.borderReached()) {
            val chunkCoordinates = nextChunkCoordinates
            if (!PaperLib.isChunkGenerated(world, chunkCoordinates.x, chunkCoordinates.z)) {
                missedChunks.add(chunkCoordinates)
            }
        }
        this.missingChunks.addAll(missedChunks)
    }

    /**
     * Cancels the generation task.
     * This unloads all chunks that were generated but not unloaded yet.
     */
    override fun cancel() {
        cancelRun = true
        updateGenerationAreaMarker(true)
    }
}