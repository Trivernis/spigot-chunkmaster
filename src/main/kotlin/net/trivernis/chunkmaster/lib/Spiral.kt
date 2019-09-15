package net.trivernis.chunkmaster.lib

import kotlin.math.abs

class Spiral(private val center: Pair<Int, Int>, private val start: Pair<Int, Int>) {
    var currentPos = start
    var direction = 0
    var count = 0

    /**
     * Returns the next value in the spiral
     */
    fun next(): Pair<Int, Int> {
        if (count == 1) {   // because of the center behaviour
            count ++
            return currentPos
        }
        if (currentPos == center) { // the center has to be handled exclusively
            currentPos = Pair(center.first, center.second + 1)
            count ++
            return center
        } else {
            val distances = getDistances(center, currentPos)
            if (abs(distances.first) == abs(distances.second)) {
                    direction = (direction + 1)%5
                }
            }
        when(direction) {
            0 -> {
                currentPos = Pair(currentPos.first + 1, currentPos.second)
            }
            1 -> {
                currentPos = Pair(currentPos.first, currentPos.second - 1)
            }
            2 -> {
                currentPos = Pair(currentPos.first - 1, currentPos.second)
            }
            3 -> {
                currentPos = Pair(currentPos.first, currentPos.second + 1)
            }
            4 -> {
                currentPos = Pair(currentPos.first, currentPos.second + 1)
                direction = 0
            }
        }
        count ++
        return currentPos
    }

    /**
     * Returns the distances between 2 coordinates
     */
    private fun getDistances(pos1: Pair<Int, Int>, pos2: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(pos2.first - pos1.first, pos2.second - pos1.second)
    }
}