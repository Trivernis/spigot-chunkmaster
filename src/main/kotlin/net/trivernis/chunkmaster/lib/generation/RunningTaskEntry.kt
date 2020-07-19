package net.trivernis.chunkmaster.lib.generation

import org.bukkit.scheduler.BukkitTask

class RunningTaskEntry(
    override val id: Int,
    private val task: BukkitTask,
    override val generationTask: GenerationTask
) : TaskEntry {

    private var lastProgress: Pair<Long, Double>? = null
    private var lastChunkCount: Pair<Long, Int>? = null

    /**
     * Returns the generation Speed
     */
    val generationSpeed: Pair<Double?, Double?>
        get() {
            var generationSpeed: Double? = null
            var chunkGenerationSpeed: Double? = null
            if (lastProgress != null) {
                val progressDiff = generationTask.shape.progress() - lastProgress!!.second
                val timeDiff = (System.currentTimeMillis() - lastProgress!!.first).toDouble()/1000
                generationSpeed = progressDiff/timeDiff
            }
            if (lastChunkCount != null) {
                val chunkDiff = generationTask.count - lastChunkCount!!.second
                val timeDiff = (System.currentTimeMillis() - lastChunkCount!!.first).toDouble()/1000
                chunkGenerationSpeed = chunkDiff/timeDiff
            }
            lastProgress = Pair(System.currentTimeMillis(), generationTask.shape.progress())
            lastChunkCount = Pair(System.currentTimeMillis(), generationTask.count)
            return Pair(generationSpeed, chunkGenerationSpeed)
        }

    init {
        lastProgress = Pair(System.currentTimeMillis(), generationTask.shape.progress())
        lastChunkCount = Pair(System.currentTimeMillis(), generationTask.count)
    }


    override fun cancel() {
        generationTask.cancel()
        task.cancel()
    }
}