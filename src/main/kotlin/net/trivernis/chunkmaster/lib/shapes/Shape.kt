package net.trivernis.chunkmaster.lib.shapes

abstract class Shape(protected val center: Pair<Int, Int>, start: Pair<Int, Int>) {
    protected var currentPos = start
    var count = 0

    /**
     * Returns the next value
     */
    abstract fun next(): Pair<Int, Int>
}