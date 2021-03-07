package net.trivernis.chunkmaster.lib

/**
 * Better argument parser for command arguments
 */
class ArgParser {
    private var input = ""
    private var position = 0
    private var currentChar = ' '
    private var escaped = false

    /**
     * Parses arguments from a string and respects quotes
     */
    fun parseArguments(arguments: String): List<String> {
        if (arguments.isEmpty()) {
            return emptyList()
        }

        input = arguments
        position = 0
        currentChar = input[position]
        escaped = false
        val args = ArrayList<String>()
        var arg = ""

        while (!endReached()) {
            nextCharacter()

            if (currentChar == '\\' && !escaped) {
                escaped = true
                continue
            }

            if (currentChar.isWhitespace() && !escaped) {
                if (arg.isNotBlank()) {
                    args.add(arg)
                }
                arg = ""
            } else if (currentChar == '"' && !escaped) {
                if (arg.isNotBlank()) {
                    args.add(arg)
                }
                arg = parseString()
                if (arg.isNotBlank()) {
                    args.add(arg)
                }
                arg = ""
            } else {
                arg += currentChar
            }
            escaped = false
        }
        if (arg.isNotBlank()) {
            args.add(arg)
        }
        return args
    }

    /**
     * Parses an enquoted string
     */
    private fun parseString(): String {
        var output = ""

        while (!endReached()) {
            nextCharacter()

            if (currentChar == '\\') {
                escaped = !escaped
                continue
            }

            if (currentChar == '"' && !escaped) {
                break
            }
            output += currentChar
            escaped = false
        }
        return output
    }

    private fun nextCharacter() {
        if (!endReached()) {
            currentChar = input[position++]
        }
    }

    private fun endReached(): Boolean {
        return position >= input.length
    }
}