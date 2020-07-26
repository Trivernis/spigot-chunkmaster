package net.trivernis.chunkmaster.lib.generation.taskentry

import net.trivernis.chunkmaster.lib.generation.GenerationTask

/**
 * Generic task entry
 */
interface TaskEntry {
    val id: Int
    val generationTask: GenerationTask
}