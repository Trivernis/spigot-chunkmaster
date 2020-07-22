package net.trivernis.chunkmaster.lib.generation

enum class TaskState {
    SEEKING {
        override fun toString(): String {
            return "SEEKING"
        }
    },
    GENERATING {
        override fun toString(): String {
            return "GENERATING"
        }
    },
    VALIDATING {
        override fun toString(): String {
            return "VALIDATING"
        }
    },
    PAUSING {
        override fun toString(): String {
            return "PAUSING"
        }
    },
}