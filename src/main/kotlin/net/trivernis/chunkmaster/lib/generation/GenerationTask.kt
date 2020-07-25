package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.dynmap.*
import net.trivernis.chunkmaster.lib.shapes.Shape
import org.bukkit.World
import java.util.concurrent.Semaphore

/**
 * Interface for generation tasks.
 */
abstract class GenerationTask(
    private val plugin: Chunkmaster,
    protected val unloader: ChunkUnloader,
    startChunk: ChunkCoordinates,
    val shape: Shape,
    val missingChunks: HashSet<ChunkCoordinates>,
    var state: TaskState
) :
    Runnable {

    abstract val radius: Int
    abstract val world: World
    abstract var count: Int
    abstract var endReached: Boolean
    var isRunning: Boolean = false

    var lastChunkCoords = ChunkCoordinates(startChunk.x, startChunk.z)
        protected set
    protected val msptThreshold = plugin.config.getLong("generation.mspt-pause-threshold")
    protected var cancelRun: Boolean = false

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
    private val ignoreWorldborder = plugin.config.getBoolean("generation.ignore-worldborder")

    abstract fun generate()
    abstract fun validate()
    abstract fun generateMissing()
    abstract fun cancel()

    override fun run() {
        isRunning = true
        try {
            when (state) {
                TaskState.GENERATING -> {
                    this.generate()
                    plugin.logger.info(plugin.langManager.getLocalized("TASK_VALIDATE_STATE"))
                    this.validate()
                    plugin.logger.info(plugin.langManager.getLocalized("TASK_CORRECT_STATE", this.missingChunks.size))
                    this.generateMissing()
                }
                TaskState.VALIDATING -> {
                    plugin.logger.info(plugin.langManager.getLocalized("TASK_VALIDATE_STATE"))
                    this.validate()
                    plugin.logger.info(plugin.langManager.getLocalized("TASK_CORRECT_STATE", this.missingChunks.size))
                    this.generateMissing()
                }
                TaskState.CORRECTING -> this.generateMissing()
                else -> { }
            }
            if (!cancelRun && this.borderReached()) {
                this.setEndReached()
            }
        } catch (e: InterruptedException){}
        isRunning = false
    }

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
     * Handles the invocation of the end reached callback and additional logic
     */
    private fun setEndReached() {
        endReached = true
        count = shape.count
        updateGenerationAreaMarker(true)
        endReachedCallback?.invoke(this)
    }

    /**
     * Registers end reached callback
     */
    fun onEndReached(cb: (GenerationTask) -> Unit) {
        endReachedCallback = cb
    }
}