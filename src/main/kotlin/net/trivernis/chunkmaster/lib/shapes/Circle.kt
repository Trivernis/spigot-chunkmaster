package net.trivernis.chunkmaster.lib.shapes

import net.trivernis.chunkmaster.lib.dynmap.ExtendedMarkerSet
import net.trivernis.chunkmaster.lib.dynmap.MarkerStyle
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.system.exitProcess

class Circle(center: Pair<Int, Int>, start: Pair<Int, Int>, radius: Int): Shape(center, start, radius) {
    private var r = 0
    private var coords = Stack<Pair<Int, Int>>()
    private var previousCoords = HashSet<Pair<Int, Int>>()

    override fun endReached(): Boolean {
        if ((radius + 1) in 1..r) return true
        return radius > 0 && coords.isEmpty() && r >= radius
    }

    override fun progress(): Double {
        return (PI * r.toFloat().pow(2))/(PI* radius.toFloat().pow(2))
    }

    override fun currentRadius(): Int {
        return r
    }

    /**
     * Returns the edge locations of the shape to be used
     * with dynmap markers
     */
    override fun getShapeEdgeLocations(): List<Pair<Int, Int>> {
        val locations = this.getCircleCoordinates(this.radius)
        locations.add(locations.first())
        return locations.map{ Pair(it.first + center.first, it.second + center.second) }
    }

    /**
     * Returns the next coordinate of the circle until the end is reached
     */
    override fun next(): Pair<Int, Int> {
        if (endReached()) {
            return currentPos
        }
        if (count == 0 && currentPos != center) {
            val tmpCircle = Circle(center, center, radius)
            while (tmpCircle.next() != currentPos);
            this.count = tmpCircle.count
            this.r = tmpCircle.r
        }
        if (count == 0) {
            count++
            return center
        }
        if (coords.isEmpty()) {
            r++
            val tmpCoords = HashSet<Pair<Int, Int>>()
            tmpCoords.addAll(getCircleCoordinates((r*2)-1).map { Pair(it.first / 2, it.second / 2) })
            tmpCoords.addAll(getCircleCoordinates(r))
            tmpCoords.removeAll(previousCoords)
            previousCoords.clear()
            coords.addAll(tmpCoords)
            previousCoords.addAll(tmpCoords)
        }
        count++
        val coord = coords.pop()
        currentPos = Pair(coord.first + center.first, coord.second + center.second)
        return currentPos
    }

    /**
     * Returns the int coordinates for a circle
     * Some coordinates might already be present in the list
     * @param r - the radius
     */
    private fun getCircleCoordinates(r: Int): ArrayList<Pair<Int, Int>> {
        val coords = ArrayList<Pair<Int, Int>>()
        val segCoords = getSegment(r)
        coords.addAll(segCoords.reversed())
        for (step in 1..7) {
            val tmpSeg = ArrayList<Pair<Int, Int>>()
            for (pos in segCoords) {
                val coord = when (step) {
                    1 -> Pair(pos.first, -pos.second)
                    2 ->Pair(pos.second, -pos.first)
                    3 -> Pair(-pos.second, -pos.first)
                    4 -> Pair(-pos.first, -pos.second)
                    5 -> Pair(-pos.first, pos.second)
                    6 -> Pair(-pos.second, pos.first)
                    7 -> Pair(pos.second, pos.first)
                    else -> pos
                }
                if (coord !in coords) {
                    tmpSeg.add(coord)
                }
            }
            if (step % 2 == 0) {
                coords.addAll(tmpSeg.reversed())
            } else {
                coords.addAll(tmpSeg)
            }
        }

        return coords
    }

    /**
     * Returns the int coordinates for a circles segment
     * @param r - the radius
     */
    private fun getSegment(r: Int): ArrayList<Pair<Int, Int>> {
        var d = -r
        var x = r
        var y = 0
        val coords = ArrayList<Pair<Int, Int>>()
        while (y <= x) {
            coords.add(Pair(x, y))
            d += 2 * y + 1
            y += 1
            if (d > 0) {
                x -= 1
                d -= 2 * x
            }
        }
        return coords
    }
}