package net.trivernis.chunkmaster.lib.shapes

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.jupiter.api.BeforeEach

class SpiralTest {

    private val spiral = Spiral(center = Pair(0, 0), radius = 2, start = Pair(0, 0))

    @BeforeEach
    fun init() {
        spiral.reset()
    }

    @Test
    fun `it generates coordinates`() {
        spiral.next().shouldBe(Pair(0, 0))
        spiral.next().shouldBe(Pair(0, 1))
        spiral.next().shouldBe(Pair(1, 1))
        spiral.next().shouldBe(Pair(1, 0))
        spiral.next().shouldBe(Pair(1, -1))
        spiral.next().shouldBe(Pair(0, -1))
        spiral.next().shouldBe(Pair(-1, -1))
        spiral.next().shouldBe(Pair(-1, 0))
        spiral.next().shouldBe(Pair(-1, 1))
        spiral.next().shouldBe(Pair(-1, 2))
        spiral.next().shouldBe(Pair(0, 2))
    }

    @Test
    fun `it reports when reaching the end`() {
        for (i in 1..25) {
            spiral.next()
        }
        spiral.endReached().shouldBeTrue()
    }

    @Test
    fun `it reports the radius`() {
        for (i in 1..9) {
            spiral.next()
        }
        spiral.currentRadius().shouldBe(1)
    }

    @Test
    fun `it returns the right edges`() {
        spiral.getShapeEdgeLocations().shouldContainAll(listOf(Pair(2, 2), Pair(-2, 2), Pair(2, -2), Pair(-2, -2)))
    }

    @Test
    fun `it returns the progress`() {
        spiral.progress(2).shouldBe(0)
        for (i in 1..8) {
            spiral.next()
        }
        spiral.progress(2).shouldBe(0.5)
    }
}