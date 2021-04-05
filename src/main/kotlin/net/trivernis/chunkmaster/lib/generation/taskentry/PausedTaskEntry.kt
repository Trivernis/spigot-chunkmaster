package net.trivernis.chunkmaster.lib.generation.taskentry

import net.trivernis.chunkmaster.lib.generation.GenerationTask

class PausedTaskEntry(
    override val id: Int,
    override val generationTask: GenerationTask
) : TaskEntry