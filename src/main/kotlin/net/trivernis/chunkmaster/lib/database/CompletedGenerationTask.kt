package net.trivernis.chunkmaster.lib.database

import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates

data class CompletedGenerationTask(
    val id: Int,
    val world: String,
    val radius: Int,
    val center: ChunkCoordinates,
    val shape: String
)