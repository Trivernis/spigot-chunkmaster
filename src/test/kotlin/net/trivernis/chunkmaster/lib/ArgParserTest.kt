package net.trivernis.chunkmaster.lib

import io.kotest.matchers.shouldBe
import org.junit.Test

class ArgParserTest {
    var argParser = ArgParser()

    @Test
    fun `it parses arguments`() {
        argParser.parseArguments("first second third forth").shouldBe(listOf("first", "second", "third", "forth"))
    }

    @Test
    fun `it handles escaped sequences`() {
        argParser.parseArguments("first second\\ pt2 third").shouldBe(listOf("first", "second pt2", "third"))
        argParser.parseArguments("first \"second\\\" part 2\" third")
            .shouldBe(listOf("first", "second\" part 2", "third"))
        argParser.parseArguments("first \\\\second third").shouldBe(listOf("first", "\\second", "third"))
    }

    @Test
    fun `it parses quoted arguments as one argument`() {
        argParser.parseArguments("first \"second with space\" third")
            .shouldBe(listOf("first", "second with space", "third"))
        argParser.parseArguments("\"first\" \"second\" \"third\"").shouldBe(listOf("first", "second", "third"))
    }

    @Test
    fun `it parses single arguments`() {
        argParser.parseArguments("one").shouldBe(listOf("one"))
        argParser.parseArguments("\"one\"").shouldBe(listOf("one"))
    }

    @Test
    fun `it parses no arguments`() {
        argParser.parseArguments("").shouldBe(emptyList())
    }

    @Test
    fun `it parses just whitespace as no arguments`() {
        argParser.parseArguments("     ").shouldBe(emptyList())
        argParser.parseArguments("\t\t").shouldBe(emptyList())
    }

    @Test
    fun `it parses arguments with weird whitespace`() {
        argParser.parseArguments("   first      second  \t third \n forth    ")
            .shouldBe(listOf("first", "second", "third", "forth"))
    }

    @Test
    fun `it deals predictable with malformed input`() {
        argParser.parseArguments("first \"second third fourth").shouldBe(listOf("first", "second third fourth"))
        argParser.parseArguments("\"first second \"third\" fourth")
            .shouldBe(listOf("first second ", "third", " fourth"))
        argParser.parseArguments("first second third fourth\"").shouldBe(listOf("first", "second", "third", "fourth"))
        argParser.parseArguments("\"").shouldBe(emptyList())
    }
}