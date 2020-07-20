package net.trivernis.chunkmaster.lib.generation.paper

import net.trivernis.chunkmaster.lib.generation.ChunkCoordinates
import org.bukkit.Chunk
import java.util.concurrent.CompletableFuture

class PendingChunkEntry(val coordinates: ChunkCoordinates, val chunk: CompletableFuture<Chunk>) {
    val isDone: Boolean
        get() = chunk.isDone
}