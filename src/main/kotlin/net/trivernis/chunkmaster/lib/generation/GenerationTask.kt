package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Spiral
import org.bukkit.Chunk
import org.bukkit.World

/**
 * Interface for generation tasks.
 */
abstract class GenerationTask(plugin: Chunkmaster, centerChunk: ChunkCoordinates, startChunk: ChunkCoordinates) :
    Runnable {

    abstract val stopAfter: Int
    abstract val world: World
    abstract val count: Int
    abstract val endReached: Boolean

    protected val spiral: Spiral =
        Spiral(Pair(centerChunk.x, centerChunk.z), Pair(startChunk.x, startChunk.z))
    protected val loadedChunks: HashSet<Chunk> = HashSet()
    protected var lastChunkCoords = ChunkCoordinates(startChunk.x, startChunk.z)
    protected val chunkSkips = plugin.config.getInt("generation.chunk-skips-per-step")
    protected val msptThreshold = plugin.config.getLong("generation.mspt-pause-threshold")
    protected val maxLoadedChunks = plugin.config.getInt("generation.max-loaded-chunks")
    protected val chunksPerStep = plugin.config.getInt("generation.chunks-per-step")
    protected val ignoreWorldborder = plugin.config.getBoolean("generation.ignore-worldborder")

    protected var endReachedCallback: ((GenerationTask) -> Unit)? = null
        private set

    abstract override fun run()
    abstract fun cancel()

    val nextChunkCoordinates: ChunkCoordinates
        get() {
            val nextChunkCoords = spiral.next()
            return ChunkCoordinates(nextChunkCoords.first, nextChunkCoords.second)
        }

    val lastChunk: Chunk
        get() {
            return world.getChunkAt(lastChunkCoords.x, lastChunkCoords.z)
        }

    val nextChunk: Chunk
        get() {
            val next = nextChunkCoordinates
            return world.getChunkAt(next.x, next.z)
        }

    /**
     * Checks if the World border or the maximum chunk setting for the task is reached.
     */
    protected fun borderReached(): Boolean {
        return (!world.worldBorder.isInside(lastChunkCoords.getCenterLocation(world)) && !ignoreWorldborder)
                || (stopAfter in 1..count)
    }

    /**
     * Registers end reached callback
     */
    fun onEndReached(cb: (GenerationTask) -> Unit) {
        endReachedCallback = cb
    }
}