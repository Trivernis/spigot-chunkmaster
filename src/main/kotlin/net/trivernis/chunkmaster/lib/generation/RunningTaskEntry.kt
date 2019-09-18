package net.trivernis.chunkmaster.lib.generation

import org.bukkit.scheduler.BukkitTask

class RunningTaskEntry(
    override val id: Int,
    val task: BukkitTask,
    override val generationTask: GenerationTask
) : TaskEntry {
    override fun cancel() {
        task.cancel()
        generationTask.cancel()
    }
}