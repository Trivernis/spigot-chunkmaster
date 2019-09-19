package net.trivernis.chunkmaster.lib.generation

import org.bukkit.scheduler.BukkitTask

class RunningTaskEntry(
    override val id: Int,
    val task: BukkitTask,
    override val generationTask: GenerationTask
) : TaskEntry {

    private var lastProgress: Pair<Long, Int>? = null

    /**
     * Returns the generation Speed
     */
    val generationSpeed: Double?
        get() {
            var generationSpeed: Double? = null
            if (lastProgress != null) {
                val chunkDiff = generationTask.count - lastProgress!!.second
                val timeDiff = (System.currentTimeMillis() - lastProgress!!.first).toDouble()/1000
                generationSpeed = chunkDiff.toDouble()/timeDiff
            }
            lastProgress = Pair(System.currentTimeMillis(), generationTask.count)
            return generationSpeed
        }

    init {
        lastProgress = Pair(System.currentTimeMillis(), generationTask.count)
    }


    override fun cancel() {
        task.cancel()
        generationTask.cancel()
    }
}