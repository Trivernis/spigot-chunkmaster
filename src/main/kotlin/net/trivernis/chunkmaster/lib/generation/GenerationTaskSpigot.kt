package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.shapes.Shape
import org.bukkit.World
import java.lang.Exception

class GenerationTaskSpigot(
    private val plugin: Chunkmaster,
    unloader: ChunkUnloader,
    override val world: World,
    startChunk: ChunkCoordinates,
    override val radius: Int = -1,
    shape: Shape
) : GenerationTask(plugin, unloader, startChunk, shape) {


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
        while (!cancel && !borderReachedCheck()) {
            if (plugin.mspt < msptThreshold) {
                if (borderReachedCheck()) return

                var chunk = nextChunkCoordinates
                for (i in 0 until chunksPerStep) {
                    if (borderReached()) break
                    val chunkInstance = world.getChunkAt(chunk.x, chunk.z)
                    chunkInstance.load(true)
                    unloader.add(chunkInstance)
                    chunk = nextChunkCoordinates
                }
                val chunkInstance = world.getChunkAt(chunk.x, chunk.z)
                chunkInstance.load(true)
                unloader.add(chunkInstance)

                lastChunkCoords = chunk
                count = shape.count
            }
        }
    }

    /**
     * Cancels the generation task.
     * This unloads all chunks that were generated but not unloaded yet.
     */
    override fun cancel() {
        cancel = true
        updateGenerationAreaMarker(true)
        updateLastChunkMarker(true)
    }
}