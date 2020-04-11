package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Spiral
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.*

/**
 * Interface for generation tasks.
 */
abstract class GenerationTask(plugin: Chunkmaster, private val centerChunk: ChunkCoordinates, startChunk: ChunkCoordinates) :
    Runnable {

    abstract val stopAfter: Int
    abstract val world: World
    abstract val count: Int
    abstract var endReached: Boolean

    protected val spiral: Spiral =
        Spiral(Pair(centerChunk.x, centerChunk.z), Pair(startChunk.x, startChunk.z))
    protected val loadedChunks: HashSet<Chunk> = HashSet()
    var lastChunkCoords = ChunkCoordinates(startChunk.x, startChunk.z)
        protected set
    protected val chunkSkips = plugin.config.getInt("generation.chunk-skips-per-step")
    protected val msptThreshold = plugin.config.getLong("generation.mspt-pause-threshold")
    protected val maxLoadedChunks = plugin.config.getInt("generation.max-loaded-chunks")
    protected val chunksPerStep = plugin.config.getInt("generation.chunks-per-step")
    protected val dynmapIntegration = plugin.config.getBoolean("dynmap")
    protected val dynmap = plugin.dynmapApi
    protected var endReachedCallback: ((GenerationTask) -> Unit)? = null
        private set

    private val markerId = "chunkmaster_genarea"
    private val markerName = "Chunkmaster Generation Area"
    private val ignoreWorldborder = plugin.config.getBoolean("generation.ignore-worldborder")

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
     * Unloads all chunks that have been loaded
     */
    protected fun unloadLoadedChunks() {
        for (chunk in loadedChunks) {
            if (chunk.isLoaded) {
                chunk.unload(true)
            }
            if (dynmapIntegration) {
                dynmap?.triggerRenderOfVolume(chunk.getBlock(0, 0, 0).location, chunk.getBlock(15, 255, 15).location)
            }
        }

        loadedChunks.clear()
    }

    /**
     * Updates the dynmap marker for the generation radius
     */
    protected fun updateDynmapMarker(clear: Boolean = false) {
        val markerSet = dynmap?.markerAPI?.getMarkerSet("markers")
        var marker = markerSet?.findAreaMarker(markerId)
        if (clear) {
            marker?.deleteMarker()
        } else if (dynmapIntegration && stopAfter > 0) {
            val (topLeft, bottomRight) = this.getAreaCorners()
            if (marker != null) {
                marker.setCornerLocations(
                    doubleArrayOf((topLeft.x * 16).toDouble(), (bottomRight.x * 16).toDouble()),
                    doubleArrayOf((topLeft.z * 16).toDouble(), (bottomRight.z * 16).toDouble())
                )
            } else {
                marker = markerSet?.createAreaMarker(
                    markerId,
                    markerName,
                    false,
                    world.name,
                    doubleArrayOf((topLeft.x * 16).toDouble(), (bottomRight.x * 16).toDouble()),
                    doubleArrayOf((topLeft.z * 16).toDouble(), (bottomRight.z * 16).toDouble()),
                    true
                )
            }
            marker?.setFillStyle(.0, 0)
            marker?.setLineStyle(2, 1.0, 0x0000FF)
        }
    }

    /**
     * Returns an approximation of cornders of the generation area
     */
    protected fun getAreaCorners(): Pair<ChunkCoordinates, ChunkCoordinates> {
        val width = sqrt(stopAfter.toFloat())
        return Pair(
            ChunkCoordinates(centerChunk.x - floor(width/2).toInt(), centerChunk.z - floor(width/2).toInt()),
            ChunkCoordinates(centerChunk.x + ceil(width/2).toInt(), centerChunk.z + ceil(width/2).toInt())
        )
    }

    /**
     * Handles the invocation of the end reached callback and additional logic
     */
    protected fun setEndReached() {
        endReached = true
        endReachedCallback?.invoke(this)
        updateDynmapMarker(true)
        if (dynmapIntegration) {
            val (topLeft, bottomRight) = this.getAreaCorners()
            dynmap?.triggerRenderOfVolume(topLeft.getCenterLocation(world), bottomRight.getCenterLocation(world))
        }
    }

    /**
     * Registers end reached callback
     */
    fun onEndReached(cb: (GenerationTask) -> Unit) {
        endReachedCallback = cb
    }
}