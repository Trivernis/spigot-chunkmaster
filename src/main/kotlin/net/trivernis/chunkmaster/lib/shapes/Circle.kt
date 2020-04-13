package net.trivernis.chunkmaster.lib.shapes

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.system.exitProcess

class Circle(center: Pair<Int, Int>, start: Pair<Int, Int>): Shape(center, start) {
    var r = 0
        private set
    private var coords = Stack<Pair<Int, Int>>()

    override fun next(): Pair<Int, Int> {
        if (count == 0 && currentPos != center) {
            val tmpCircle = Circle(center, center)
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
            coords.addAll(tmpCoords)
        }
        count++
        val coord = coords.pop()
        currentPos = Pair(coord.first + center.first, coord.second + center.second)
        return currentPos
    }

    /**
     * Returns the int coordinates for a circle
     * @param r - the radius
     */
    private fun getCircleCoordinates(r: Int): HashSet<Pair<Int, Int>> {
        val coords = ArrayList<Pair<Int, Int>>()
        val segCoords = getSegment(r)
        coords.addAll(segCoords)
        for (step in 0..7) {
            for (pos in segCoords) {
                coords.add(when (step) {
                    0 -> pos
                    1 -> Pair(pos.second, pos.first)
                    2 -> Pair(pos.first, -pos.second)
                    3 -> Pair(-pos.second, pos.first)
                    4 -> Pair(-pos.first, -pos.second)
                    5 -> Pair(-pos.second, -pos.first)
                    6 -> Pair(-pos.first, pos.second)
                    7 -> Pair(pos.second, -pos.first)
                    else -> pos
                })
            }
        }

        val set = HashSet<Pair<Int, Int>>()
        set.addAll(coords)
        return set
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