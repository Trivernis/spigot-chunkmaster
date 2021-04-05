package net.trivernis.chunkmaster.lib.generation.taskentry

import net.trivernis.chunkmaster.lib.generation.GenerationTask

class RunningTaskEntry(
    override val id: Int,
    override val generationTask: GenerationTask
) : TaskEntry {

    private var lastProgress: Pair<Long, Double>? = null
    private var lastChunkCount: Pair<Long, Int>? = null
    private var thread = Thread(generationTask)

    /**
     * Returns the generation Speed
     */
    val generationSpeed: Pair<Double?, Double?>
        get() {
            var generationSpeed: Double? = null
            var chunkGenerationSpeed: Double? = null
            val progress =
                generationTask.shape.progress(if (generationTask.radius < 0) (generationTask.world.worldBorder.size / 32).toInt() else null)
            if (lastProgress != null) {
                val progressDiff = progress - lastProgress!!.second
                val timeDiff = (System.currentTimeMillis() - lastProgress!!.first).toDouble() / 1000
                generationSpeed = progressDiff / timeDiff
            }
            if (lastChunkCount != null) {
                val chunkDiff = generationTask.count - lastChunkCount!!.second
                val timeDiff = (System.currentTimeMillis() - lastChunkCount!!.first).toDouble() / 1000
                chunkGenerationSpeed = chunkDiff / timeDiff
            }
            lastProgress = Pair(System.currentTimeMillis(), progress)
            lastChunkCount = Pair(System.currentTimeMillis(), generationTask.count)
            return Pair(generationSpeed, chunkGenerationSpeed)
        }

    init {
        lastProgress = Pair(System.currentTimeMillis(), generationTask.shape.progress(null))
        lastChunkCount = Pair(System.currentTimeMillis(), generationTask.count)
    }

    fun start() {
        thread.start()
    }

    fun cancel(timeout: Long): Boolean {
        if (generationTask.isRunning) {
            generationTask.cancel()
            thread.interrupt()
        }
        return try {
            joinThread(timeout)
        } catch (e: InterruptedException) {
            true
        }
    }

    private fun joinThread(timeout: Long): Boolean {
        var threadStopped = false

        for (i in 0..100) {
            if (!thread.isAlive || !generationTask.isRunning) {
                threadStopped = true
                break
            }
            Thread.sleep(timeout / 100)
        }
        return threadStopped
    }
}