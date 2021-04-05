package net.trivernis.chunkmaster.lib.database

import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates
import net.trivernis.chunkmaster.lib.generation.TaskState

data class GenerationTaskData(
    val id: Int,
    val world: String,
    val radius: Int,
    val shape: String,
    val state: TaskState,
    val center: ChunkCoordinates,
    val last: ChunkCoordinates
)