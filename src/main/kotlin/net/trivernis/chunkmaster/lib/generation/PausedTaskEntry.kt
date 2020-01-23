package net.trivernis.chunkmaster.lib.generation

class PausedTaskEntry(
    override val id: Int,
    override val generationTask: GenerationTask
) : TaskEntry {
    override fun cancel() {
        generationTask.cancel()
    }
}