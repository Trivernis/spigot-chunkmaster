package net.trivernis.chunkmaster.lib.generation

enum class TaskState {
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
    CORRECTING {
        override fun toString(): String {
            return "CORRECTING"
        }
    },
    PAUSING {
        override fun toString(): String {
            return "PAUSING"
        }
    },
}