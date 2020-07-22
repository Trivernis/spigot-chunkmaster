package net.trivernis.chunkmaster.lib.generation

import io.papermc.lib.PaperLib
import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.generation.paper.GenerationTaskPaper
import net.trivernis.chunkmaster.lib.generation.spigot.GenerationTaskSpigot
import net.trivernis.chunkmaster.lib.generation.taskentry.PausedTaskEntry
import net.trivernis.chunkmaster.lib.generation.taskentry.RunningTaskEntry
import net.trivernis.chunkmaster.lib.generation.taskentry.TaskEntry
import net.trivernis.chunkmaster.lib.shapes.Circle
import net.trivernis.chunkmaster.lib.shapes.Spiral
import org.bukkit.Server
import org.bukkit.World

class GenerationManager(private val chunkmaster: Chunkmaster, private val server: Server) {

    val tasks: HashSet<RunningTaskEntry> = HashSet()
    val pausedTasks: HashSet<PausedTaskEntry> = HashSet()
    val worldProperties = chunkmaster.sqliteManager.worldProperties
    private val pendingChunksTable = chunkmaster.sqliteManager.pendingChunks
    private val generationTasks = chunkmaster.sqliteManager.generationTasks

    val loadedChunkCount: Int
        get() {
            return unloader.pendingSize
        }
    private val unloader = ChunkUnloader(chunkmaster)


    val allTasks: HashSet<TaskEntry>
        get() {
            if (this.tasks.isEmpty() && this.pausedTasks.isEmpty()) {
                this.startAll()
                if (server.onlinePlayers.size >= chunkmaster.config.getInt("generation.pause-on-player-count")) {
                    this.pauseAll()
                }
            }
            val all = HashSet<TaskEntry>()
            all.addAll(pausedTasks)
            all.addAll(tasks)
            return all
        }
    var paused = false
        private set

    /**
     * Adds a generation task
     */
    fun addTask(world: World, radius: Int = -1, shape: String = "square"): Int {
        val foundTask = allTasks.find { it.generationTask.world == world }

        if (foundTask == null) {
            val center = worldProperties.getWorldCenter(world.name).join()

            val centerChunk = if (center == null) {
                ChunkCoordinates(world.spawnLocation.chunk.x, world.spawnLocation.chunk.z)
            } else {
                ChunkCoordinates(center.first, center.second)
            }
            val generationTask = createGenerationTask(world, centerChunk, centerChunk, radius, shape, null)
            val id = generationTasks.addGenerationTask(world.name, centerChunk, radius, shape).join()

            generationTask.onEndReached {
                chunkmaster.logger.info(chunkmaster.langManager.getLocalized("TASK_FINISHED", id, it.count))
                removeTask(id)
            }

            if (!paused) {
                val taskEntry = RunningTaskEntry(
                    id,
                    generationTask
                )
                taskEntry.start()
                tasks.add(taskEntry)
            } else {
                pausedTasks.add(
                    PausedTaskEntry(
                        id,
                        generationTask
                    )
                )
            }

            return id
        } else {
            return foundTask.id
        }
    }

    /**
     * Resumes a generation task
     */
    private fun resumeTask(
        world: World,
        center: ChunkCoordinates,
        last: ChunkCoordinates,
        id: Int,
        radius: Int = -1,
        shape: String = "square",
        pendingChunks: List<ChunkCoordinates>?
    ) {
        if (!paused) {
            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("RESUME_FOR_WORLD", world.name))
            val generationTask = createGenerationTask(world, center, last, radius, shape, pendingChunks)
            val taskEntry = RunningTaskEntry(
                id,
                generationTask
            )
            taskEntry.start()
            tasks.add(taskEntry)
            generationTask.onEndReached {
                chunkmaster.logger.info(chunkmaster.langManager.getLocalized("TASK_FINISHED", id, generationTask.count))
                removeTask(id)
            }
        }
    }

    /**
     * Stops a running generation task.
     */
    fun removeTask(id: Int): Boolean {
        val taskEntry: TaskEntry? = if (this.paused) {
            this.pausedTasks.find { it.id == id }
        } else {
            this.tasks.find { it.id == id }
        }
        if (taskEntry != null) {
            if (taskEntry.generationTask.isRunning && taskEntry is RunningTaskEntry) {
                taskEntry.cancel(chunkmaster.config.getLong("mspt-pause-threshold"))
            }
            generationTasks.deleteGenerationTask(id)
            pendingChunksTable.clearPendingChunks(id)

            if (taskEntry is RunningTaskEntry) {
                tasks.remove(taskEntry)
            } else if (taskEntry is PausedTaskEntry) {
                pausedTasks.remove(taskEntry)
            }
            return true
        }
        return false
    }

    /**
     * Init
     * Loads tasks from the database and resumes them
     */
    fun init() {
        chunkmaster.logger.info(chunkmaster.langManager.getLocalized("CREATE_DELAYED_LOAD"))
        server.scheduler.runTaskTimer(chunkmaster, Runnable {
            saveProgress()      // save progress every 30 seconds
        }, 600, 600)
        server.scheduler.runTaskLater(chunkmaster, Runnable {
            this.startAll()
            if (!server.onlinePlayers.isEmpty()) {
                this.pauseAll()
            }
        }, 20)
        server.scheduler.runTaskTimer(chunkmaster, unloader, 100, 100)
    }

    /**
     * Stops all generation tasks
     */
    fun stopAll() {
        val removalSet = HashSet<RunningTaskEntry>()
        for (task in tasks) {
            val id = task.id
            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("SAVING_TASK_PROGRESS", task.id))
            saveProgressToDatabase(task.generationTask, id)
            task.cancel(chunkmaster.config.getLong("mspt-pause-threshold"))
            removalSet.add(task)

            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("TASK_CANCELLED", task.id))
        }
        tasks.removeAll(removalSet)
        if (unloader.pendingSize > 0) {
            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("SAVING_CHUNKS", unloader.pendingSize))
            unloader.run()
        }
    }

    /**
     * Starts all generation tasks.
     */
    fun startAll() {
        generationTasks.getGenerationTasks().thenAccept { tasks ->
            for (task in tasks) {
                val world = server.getWorld(task.world)
                if (world != null) {
                    pendingChunksTable.getPendingChunks(task.id).thenAccept {
                        resumeTask(world, task.center, task.last, task.id, task.radius, task.shape, it)
                    }
                } else {
                    chunkmaster.logger.severe(chunkmaster.langManager.getLocalized("TASK_LOAD_FAILED", task.id))
                }
            }
        }

        if (tasks.isNotEmpty()) {
            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("TASK_LOAD_SUCCESS", tasks.size))
        }
    }

    /**
     * Pauses all tasks
     */
    fun pauseAll() {
        paused = true
        for (task in tasks) {
            pausedTasks.add(
                PausedTaskEntry(
                    task.id,
                    task.generationTask
                )
            )
        }
        stopAll()
    }

    /**
     * Resumes all tasks
     */
    fun resumeAll() {
        paused = false
        pausedTasks.clear()
        startAll()
    }

    /**
     * Saves the task progress
     */
    private fun saveProgress() {
        for (task in tasks) {
            try {
                val genTask = task.generationTask
                val (speed, chunkSpeed) = task.generationSpeed
                val percentage = if (genTask.radius > 0) "(${"%.2f".format(genTask.shape.progress() * 100)}%)" else ""
                val eta = if (genTask.radius > 0 && speed!! > 0) {
                    val etaSeconds = (genTask.shape.progress())/speed
                    val hours: Int = (etaSeconds/3600).toInt()
                    val minutes: Int = ((etaSeconds % 3600) / 60).toInt()
                    val seconds: Int = (etaSeconds % 60).toInt()
                    ", ETA: %d:%02d:%02d".format(hours, minutes, seconds)
                } else {
                    ""
                }
                chunkmaster.logger.info(chunkmaster.langManager.getLocalized(
                    "TASK_PERIODIC_REPORT",
                    task.id,
                    genTask.world.name,
                    genTask.state.toString(),
                    genTask.count,
                    percentage,
                    eta,
                    chunkSpeed!!,
                    genTask.lastChunkCoords.x,
                    genTask.lastChunkCoords.z))
                saveProgressToDatabase(genTask, task.id)
            } catch (error: Exception) {
                chunkmaster.logger.warning(chunkmaster.langManager.getLocalized("TASK_SAVE_FAILED", error.toString()))
            }
        }
    }

    /**
     * Saves the generation progress to the database
     */
    private fun saveProgressToDatabase(generationTask: GenerationTask, id: Int) {
        generationTasks.updateGenerationTask(id, generationTask.lastChunkCoords, generationTask.state).thenAccept{
            if (generationTask is GenerationTaskPaper) {
                if (generationTask.pendingChunks.size > 0) {
                    pendingChunksTable.clearPendingChunks(id).thenAccept {
                        pendingChunksTable.addPendingChunks(id, generationTask.pendingChunks.map { it.coordinates })
                    }
                }
            }
        }
    }

    /**
     * Creates a new generation task. This method is used to create a task depending
     * on the server type (Paper/Spigot).
     */
    private fun createGenerationTask(
        world: World,
        center: ChunkCoordinates,
        start: ChunkCoordinates,
        radius: Int,
        shapeName: String,
        pendingChunks: List<ChunkCoordinates>?
    ): GenerationTask {
        val shape = when (shapeName) {
            "circle" -> Circle(Pair(center.x, center.z), Pair(start.x, start.z), radius)
            "square" -> Spiral(Pair(center.x, center.z), Pair(start.x, start.z), radius)
            else -> Spiral(Pair(center.x, center.z), Pair(start.x, start.z), radius)
        }

        return if (PaperLib.isPaper()) {
            GenerationTaskPaper(
                chunkmaster,
                unloader,
                world,
                start,
                radius,
                shape,
                pendingChunks ?: emptyList(),
                TaskState.GENERATING
            )
        } else {
            GenerationTaskSpigot(
                chunkmaster,
                unloader,
                world,
                start,
                radius,
                shape,
                pendingChunks ?: emptyList(),
                TaskState.GENERATING
            )
        }
    }
}