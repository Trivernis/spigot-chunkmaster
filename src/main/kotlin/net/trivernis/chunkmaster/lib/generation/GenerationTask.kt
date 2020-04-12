package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.Spiral
import net.trivernis.chunkmaster.lib.dynmap.*
import org.bukkit.Chunk
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

    private var endReachedCallback: ((GenerationTask) -> Unit)? = null

    private val dynmapIntegration = plugin.config.getBoolean("dynmap")
    private val dynmap = plugin.dynmapApi
    private val markerSet: ExtendedMarkerSet? = if (dynmap != null) {
        DynmapApiWrapper(dynmap).getCreateMarkerSet("chunkmaster", "Chunkmaster")
    } else {
        null
    }
    private val markerAreaStyle = MarkerStyle(null, LineStyle(2, 1.0, 0x0022FF), FillStyle(.0, 0))
    private val markerAreaId = "chunkmaster_genarea"
    private val markerAreaName = "Chunkmaster Generation Area"
    private val markerLastStyle = MarkerStyle(null, LineStyle(2, 1.0, 0x0077FF), FillStyle(.0, 0))
    private val markerLastId = "chunkmaster_lastchunk"
    private val markerLastName = "Chunkmaster Last Chunk"
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
    protected fun updateGenerationAreaMarker(clear: Boolean = false) {
        if (clear) {
            markerSet?.deleteAreaMarker(markerAreaId)
        } else if (dynmapIntegration && stopAfter > 0) {
            val (topLeft, bottomRight) = this.getAreaCorners()
            markerSet?.creUpdateAreMarker(
                markerAreaId,
                markerAreaName,
                topLeft.getCenterLocation(world),
                bottomRight.getCenterLocation(world),
                markerAreaStyle
            )
        }
    }

    /**
     * Updates the dynmap marker for the generation radius
     */
    fun updateLastChunkMarker(clear: Boolean = false) {
        if (clear) {
            markerSet?.deleteAreaMarker(markerLastId)
        } else if (dynmapIntegration) {
            markerSet?.creUpdateAreMarker(
                markerLastId,
                markerLastName,
                this.lastChunk.getBlock(0, 0, 0).location,
                this.lastChunk.getBlock(15, 0, 15).location,
                markerLastStyle
            )
        }
    }

    /**
     * Returns an approximation of cornders of the generation area
     */
    private fun getAreaCorners(): Pair<ChunkCoordinates, ChunkCoordinates> {
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
        updateGenerationAreaMarker(true)
        updateLastChunkMarker(true)
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