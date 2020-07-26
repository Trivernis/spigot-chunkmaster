package net.trivernis.chunkmaster.lib.generation

import org.bukkit.Location
import org.bukkit.World

class ChunkCoordinates(val x: Int, val z: Int) {
    fun getCenterLocation(world: World): Location {
        return Location(world, ((x*16) + 8).toDouble(), 1.0, ((z*16) + 8).toDouble())
    }

    override fun toString(): String {
        return "($x, $z)"
    }
}