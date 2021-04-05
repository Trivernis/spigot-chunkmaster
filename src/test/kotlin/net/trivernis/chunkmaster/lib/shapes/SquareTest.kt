package net.trivernis.chunkmaster.lib.shapes

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.jupiter.api.BeforeEach

class SquareTest {

    private val square = Square(center = Pair(0, 0), radius = 2, start = Pair(0, 0))

    @BeforeEach
    fun init() {
        square.reset()
    }

    @Test
    fun `it generates coordinates`() {
        square.next().shouldBe(Pair(0, 0))
        square.next().shouldBe(Pair(0, 1))
        square.next().shouldBe(Pair(1, 1))
        square.next().shouldBe(Pair(1, 0))
        square.next().shouldBe(Pair(1, -1))
        square.next().shouldBe(Pair(0, -1))
        square.next().shouldBe(Pair(-1, -1))
        square.next().shouldBe(Pair(-1, 0))
        square.next().shouldBe(Pair(-1, 1))
        square.next().shouldBe(Pair(-1, 2))
        square.next().shouldBe(Pair(0, 2))
    }

    @Test
    fun `it reports when reaching the end`() {
        for (i in 1..25) {
            square.next()
        }
        square.endReached().shouldBeTrue()
    }

    @Test
    fun `it reports the radius`() {
        for (i in 1..9) {
            square.next()
        }
        square.currentRadius().shouldBe(1)
    }

    @Test
    fun `it returns the right edges`() {
        square.getShapeEdgeLocations().shouldContainAll(listOf(Pair(2, 2), Pair(-2, 2), Pair(2, -2), Pair(-2, -2)))
    }

    @Test
    fun `it returns the progress`() {
        square.progress(2).shouldBe(0)
        for (i in 1..8) {
            square.next()
        }
        square.progress(2).shouldBe(0.5)
    }
}