package net.trivernis.chunkmaster.lib.generation

import org.bukkit.Chunk
import java.util.concurrent.CompletableFuture

class PendingChunkEntry(val coordinates: ChunkCoordinates, val chunk: CompletableFuture<Chunk>) {
    val isDone: Boolean
        get() = chunk.isDone
}