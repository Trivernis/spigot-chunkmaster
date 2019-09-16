package net.trivernis.chunkmaster.lib

import org.bukkit.scheduler.BukkitTask

data class TaskEntry(val id: Int, val task: BukkitTask, val generationTask: GenerationTask) {
}