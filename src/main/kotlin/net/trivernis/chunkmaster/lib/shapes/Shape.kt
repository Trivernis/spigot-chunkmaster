package net.trivernis.chunkmaster.lib.shapes

import net.trivernis.chunkmaster.lib.dynmap.ExtendedMarkerSet
import net.trivernis.chunkmaster.lib.dynmap.MarkerStyle
import javax.xml.stream.Location

abstract class Shape(protected val center: Pair<Int, Int>, start: Pair<Int, Int>, radius: Int) {
    protected var currentPos = start
    protected var radius = radius
        private set
    var count = 0

    /**
     * Returns the next value
     */
    abstract fun next(): Pair<Int, Int>

    /**
     * If the shape can provide a next value
     */
    abstract fun endReached(): Boolean

    /**
     * Returns the progress of the shape
     */
    abstract fun progress(): Double

    /**
     * Returns the current radius
     */
    abstract fun currentRadius(): Int

    /**
     * returns a poly marker for the shape
     */
    abstract fun getShapeEdgeLocations(): List<Pair<Int, Int>>
}