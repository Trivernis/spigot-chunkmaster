package net.trivernis.chunkmaster.lib.shapes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class Spiral(center: Pair<Int, Int>, start: Pair<Int, Int>, radius: Int): Shape(center, start, radius) {
    private var direction = 0

    override fun endReached(): Boolean {
        val distances = getDistances(center, currentPos)
        return radius > 0 && ((direction == 3
                && abs(distances.first) == abs(distances.second)
                && abs(distances.first) == radius)
                || (distances.first > radius || distances.second > radius))
    }

    override fun total(): Double {
        return (radius * 2).toDouble().pow(2)
    }

    override fun progress(): Double {
        return (count / (radius * 2).toDouble().pow(2)).coerceAtMost(1.0)
    }

    override fun currentRadius(): Int {
        val distances = getDistances(center, currentPos)
        return distances.first.coerceAtLeast(distances.second)
    }

    /**
     * Returns the next value in the spiral
     */
    override fun next(): Pair<Int, Int> {
        if (endReached()) {
            return currentPos
        }
        if (count == 0 && currentPos != center) {
            // simulate the spiral to get the correct direction and count
            val simSpiral = Spiral(center, center, radius)
            while (simSpiral.next() != currentPos);
            direction = simSpiral.direction
            count = simSpiral.count
        }
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
     * Returns the edges to be used with dynmap markers
     */
    override fun getShapeEdgeLocations(): List<Pair<Int, Int>> {
        val a = Pair(this.radius + center.first, this.radius + center.second)
        val b = Pair(this.radius + center.first, -this.radius + center.second)
        val c = Pair(-this.radius + center.first, -this.radius + center.second)
        val d = Pair(-this.radius + center.first, this.radius + center.second)
        return listOf(a, b, c, d, a)
    }

    /**
     * Returns the distances between 2 coordinates
     */
    private fun getDistances(pos1: Pair<Int, Int>, pos2: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(pos2.first - pos1.first, pos2.second - pos1.second)
    }

    /**
     * Resets the shape to its starting parameters
     */
    override fun reset() {
        this.currentPos = center
        this.count = 0
        this.direction = 0
    }
}