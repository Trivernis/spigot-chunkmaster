package net.trivernis.chunkmaster.lib.generation

import org.bukkit.scheduler.BukkitTask

/**
 * Generic task entry
 */
interface TaskEntry {
    val id: Int
    val generationTask: GenerationTask

    fun cancel()
}