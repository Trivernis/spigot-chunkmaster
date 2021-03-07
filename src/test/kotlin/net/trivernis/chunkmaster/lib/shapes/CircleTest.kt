package net.trivernis.chunkmaster.lib.shapes

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.doubles.shouldBeBetween
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.jupiter.api.BeforeEach

class CircleTest {
    private val circle = Circle(center = Pair(0, 0), radius = 2, start = Pair(0, 0))

    @BeforeEach
    fun init() {
        circle.reset()
    }

    @Test
    fun `it generates coordinates`() {
        circle.next().shouldBe(Pair(0, 0))
        circle.next().shouldBe(Pair(-1, -1))
        circle.next().shouldBe(Pair(1, 0))
        circle.next().shouldBe(Pair(-1, 0))
        circle.next().shouldBe(Pair(1, -1))
        circle.next().shouldBe(Pair(-1, 1))
        circle.next().shouldBe(Pair(0, 1))
        circle.next().shouldBe(Pair(0, -1))
        circle.next().shouldBe(Pair(1, 1))
    }

    @Test
    fun `it reports when reaching the end`() {
        for (i in 1..25) {
            circle.next()
        }
        circle.endReached().shouldBeTrue()
    }

    @Test
    fun `it reports the radius`() {
        for (i in 1..9) {
            circle.next()
        }
        circle.currentRadius().shouldBe(1)
    }

    @Test
    fun `it returns the right edges`() {
        circle.getShapeEdgeLocations().shouldContainAll(
            listOf(
                Pair(2, -1),
                Pair(2, 0),
                Pair(2, 1),
                Pair(1, 2),
                Pair(0, 2),
                Pair(-1, 2),
                Pair(-2, 1),
                Pair(-2, 0),
                Pair(-2, -1),
                Pair(-1, -2),
                Pair(0, -2),
                Pair(1, -2),
            )
        )
    }

    @Test
    fun `it returns the progress`() {
        circle.progress(2).shouldBe(0)
        for (i in 1..7) {
            circle.next()
        }
        circle.progress(2).shouldBeBetween(.5, .8, .0)
    }
}