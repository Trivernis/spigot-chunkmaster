package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.dynmap.*
import net.trivernis.chunkmaster.lib.shapes.Shape
import org.bukkit.Chunk
import org.bukkit.World
import java.lang.Exception

/**
 * Interface for generation tasks.
 */
abstract class GenerationTask(
    private val plugin: Chunkmaster,
    startChunk: ChunkCoordinates,
    val shape: Shape
) :
    Runnable {

    abstract val radius: Int
    abstract val world: World
    abstract var count: Int
    abstract var endReached: Boolean

    val loadedChunksCount: Int
        get() {
            return loadedChunks.size
        }

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
    private val markerLastStyle = MarkerStyle(null, LineStyle(2, 1.0, 0x0077FF), FillStyle(.5, 0x0077FF))
    private val markerLastId = "chunkmaster_lastchunk"
    private val markerLastName = "Chunkmaster Last Chunk"
    private val ignoreWorldborder = plugin.config.getBoolean("generation.ignore-worldborder")

    abstract override fun run()
    abstract fun cancel()

    val nextChunkCoordinates: ChunkCoordinates
        get() {
            val nextChunkCoords = shape.next()
            return ChunkCoordinates(nextChunkCoords.first, nextChunkCoords.second)
        }

    /**
     * Checks if the World border or the maximum chunk setting for the task is reached.
     */
    protected fun borderReached(): Boolean {
        return (!world.worldBorder.isInside(lastChunkCoords.getCenterLocation(world)) && !ignoreWorldborder)
                || shape.endReached()
    }

    /**
     * Unloads all chunks that have been loaded
     */
    protected fun unloadLoadedChunks() {
        for (chunk in loadedChunks) {
            if (chunk.isLoaded) {
                try {
                    chunk.unload(true)
                } catch (e: Exception) {
                    plugin.logger.severe(e.toString())
                }
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
            markerSet?.deletePolyLineMarker(markerAreaId)
        } else if (dynmapIntegration && radius > 0) {
            markerSet?.creUpdatePolyLineMarker(
                markerAreaId,
                markerAreaName,
                this.shape.getShapeEdgeLocations().map { ChunkCoordinates(it.first, it.second).getCenterLocation(this.world) },
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
                this.lastChunkCoords.getCenterLocation(world).chunk.getBlock(0, 0, 0).location,
                this.lastChunkCoords.getCenterLocation(world).chunk.getBlock(15, 0, 15).location,
                markerLastStyle
            )
        }
    }

    /**
     * Handles the invocation of the end reached callback and additional logic
     */
    private fun setEndReached() {
        endReached = true
        count = shape.count
        endReachedCallback?.invoke(this)
        updateGenerationAreaMarker(true)
        updateLastChunkMarker(true)
    }

    /**
     * Performs a check if the border has been reached
     */
    protected fun borderReachedCheck(): Boolean {
        val done = borderReached()
        if (done) {
            unloadLoadedChunks()
            setEndReached()
        }
        return done
    }

    /**
     * Registers end reached callback
     */
    fun onEndReached(cb: (GenerationTask) -> Unit) {
        endReachedCallback = cb
    }
}