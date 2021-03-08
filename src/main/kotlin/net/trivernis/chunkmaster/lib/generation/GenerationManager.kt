package net.trivernis.chunkmaster.lib.generation

import net.trivernis.chunkmaster.Chunkmaster
import net.trivernis.chunkmaster.lib.generation.taskentry.PausedTaskEntry
import net.trivernis.chunkmaster.lib.generation.taskentry.RunningTaskEntry
import net.trivernis.chunkmaster.lib.generation.taskentry.TaskEntry
import net.trivernis.chunkmaster.lib.shapes.Circle
import net.trivernis.chunkmaster.lib.shapes.Shape
import net.trivernis.chunkmaster.lib.shapes.Square
import org.bukkit.Server
import org.bukkit.World
import java.util.concurrent.CompletableFuture

class GenerationManager(private val chunkmaster: Chunkmaster, private val server: Server) {

    val tasks: HashSet<RunningTaskEntry> = HashSet()
    val pausedTasks: HashSet<PausedTaskEntry> = HashSet()
    val worldProperties = chunkmaster.sqliteManager.worldProperties
    private val pendingChunksTable = chunkmaster.sqliteManager.pendingChunks
    private val generationTasks = chunkmaster.sqliteManager.generationTasks
    private val completedGenerationTasks = chunkmaster.sqliteManager.completedGenerationTasks

    private val unloadingPeriod: Long
        get() {
            return chunkmaster.config.getLong("generation.unloading-period")
        }
    private val pauseOnPlayerCount: Int
        get() {
            return chunkmaster.config.getInt("generation.pause-on-player-count")
        }
    private val autostart: Boolean
        get() {
            return chunkmaster.config.getBoolean("generation.autostart")
        }

    val loadedChunkCount: Int
        get() {
            return unloader.pendingSize
        }
    private val unloader = ChunkUnloader(chunkmaster)


    val allTasks: HashSet<TaskEntry>
        get() {
            if (this.tasks.isEmpty() && this.pausedTasks.isEmpty()) {
                this.startAll()
                if (server.onlinePlayers.size >= pauseOnPlayerCount) {
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
    fun addTask(world: World, radius: Int = -1, shape: String = "square", startRadius: Int = 0): Int {
        val foundTask = allTasks.find { it.generationTask.world == world }

        if (foundTask == null) {
            val center = worldProperties.getWorldCenter(world.name).join()


            val centerChunk = if (center == null) {
                ChunkCoordinates(world.spawnLocation.chunk.x, world.spawnLocation.chunk.z)
            } else {
                ChunkCoordinates(center.first, center.second)
            }
            val shapeInstance = stringToShape(shape, centerChunk, centerChunk, radius)
            var startCoordinates = Pair(centerChunk.x, centerChunk.z)

            if (startRadius > 0) {
                println(startRadius)

                while (shapeInstance.currentRadius() != startRadius) {
                    startCoordinates = shapeInstance.next()
                    println(shapeInstance.currentRadius())
                }
            }

            val generationTask = createGenerationTask(world, centerChunk, ChunkCoordinates(startCoordinates.first, startCoordinates.second), radius, shape, null)
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
        try {
            if (taskEntry != null) {
                if (taskEntry.generationTask.isRunning && taskEntry is RunningTaskEntry) {
                    taskEntry.cancel(chunkmaster.config.getLong("mspt-pause-threshold"))
                }
                generationTasks.deleteGenerationTask(id)
                completedGenerationTasks.addCompletedTask(
                    id,
                    taskEntry.generationTask.world.name,
                    taskEntry.generationTask.shape.currentRadius(),
                    taskEntry.generationTask.startChunk,
                    taskEntry.generationTask.shape.javaClass.simpleName
                )
                pendingChunksTable.clearPendingChunks(id)

                if (taskEntry is RunningTaskEntry) {
                    tasks.remove(taskEntry)
                } else if (taskEntry is PausedTaskEntry) {
                    pausedTasks.remove(taskEntry)
                }
                return true
            }
        } catch (e: Exception) {
            chunkmaster.logger.severe(e.toString())
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
            if (server.onlinePlayers.count() >= pauseOnPlayerCount || !autostart) {
                if (!autostart) {
                    chunkmaster.logger.info(chunkmaster.langManager.getLocalized("NO_AUTOSTART"))
                }
                this.pauseAll()
            }
        }, 20)
        server.scheduler.runTaskTimer(chunkmaster, unloader, unloadingPeriod, unloadingPeriod)
    }

    /**
     * Stops all generation tasks
     */
    fun stopAll() {
        val removalSet = HashSet<RunningTaskEntry>()
        for (task in tasks) {
            val id = task.id
            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("SAVING_TASK_PROGRESS", task.id))
            saveProgressToDatabase(task.generationTask, id).join()
            if (!task.cancel(chunkmaster.config.getLong("mspt-pause-threshold"))) {
                chunkmaster.logger.warning(chunkmaster.langManager.getLocalized("CANCEL_FAIL", task.id))
            }
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
                if (task.generationTask.state == TaskState.CORRECTING) {
                    reportCorrectionProgress(task)
                } else {
                    reportGenerationProgress(task)
                }
                saveProgressToDatabase(task.generationTask, task.id)
            } catch (error: Exception) {
                chunkmaster.logger.warning(chunkmaster.langManager.getLocalized("TASK_SAVE_FAILED", error.toString()))
                error.printStackTrace()
            }
        }
    }

    /**
     * Reports the progress for correcting tasks
     */
    private fun reportCorrectionProgress(task: RunningTaskEntry) {
        val genTask = task.generationTask
        val progress = if (genTask.missingChunks.size > 0) {
            "(${(genTask.count / genTask.missingChunks.size) * 100}%)"
        } else {
            ""
        }
        chunkmaster.logger.info(
            chunkmaster.langManager.getLocalized(
                "TASK_PERIODIC_REPORT_CORRECTING",
                task.id,
                genTask.world.name,
                genTask.count,
                progress
            )
        )
    }

    /**
     * Reports the progress of the chunk generation
     */
    private fun reportGenerationProgress(task: RunningTaskEntry) {
        val genTask = task.generationTask
        val (speed, chunkSpeed) = task.generationSpeed
        val progress =
            genTask.shape.progress(if (genTask.radius < 0) (genTask.world.worldBorder.size / 32).toInt() else null)
        val percentage =
            "(${"%.2f".format(progress * 100)}%)"

        val eta = if (speed!! > 0) {
            val remaining = 1 - progress
            val etaSeconds = remaining / speed
            val hours: Int = (etaSeconds / 3600).toInt()
            val minutes: Int = ((etaSeconds % 3600) / 60).toInt()
            val seconds: Int = (etaSeconds % 60).toInt()
            ", ETA: %dh %dmin %ds".format(hours, minutes, seconds)
        } else {
            ""
        }
        chunkmaster.logger.info(
            chunkmaster.langManager.getLocalized(
                "TASK_PERIODIC_REPORT",
                task.id,
                genTask.world.name,
                genTask.state.toString(),
                genTask.count,
                percentage,
                eta,
                chunkSpeed!!,
                genTask.lastChunkCoords.x,
                genTask.lastChunkCoords.z
            )
        )
    }

    /**
     * Saves the generation progress to the database
     */
    private fun saveProgressToDatabase(generationTask: GenerationTask, id: Int): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        generationTasks.updateGenerationTask(id, generationTask.lastChunkCoords, generationTask.state).thenAccept {
            pendingChunksTable.clearPendingChunks(id).thenAccept {
                if (generationTask is DefaultGenerationTask) {
                    if (generationTask.pendingChunks.size > 0) {
                        pendingChunksTable.addPendingChunks(id, generationTask.pendingChunks.map { it.coordinates })
                    }
                }
                pendingChunksTable.addPendingChunks(id, generationTask.missingChunks.toList()).thenAccept {
                    completableFuture.complete(null)
                }
            }
        }
        return completableFuture
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
        val shape = stringToShape(shapeName, center, start, radius)

        return DefaultGenerationTask(
            chunkmaster,
            unloader,
            world,
            start,
            radius,
            shape, pendingChunks?.toHashSet() ?: HashSet(),
            TaskState.GENERATING
        )
    }

    private fun stringToShape(
        shapeName: String,
        center: ChunkCoordinates,
        start: ChunkCoordinates,
        radius: Int
    ): Shape {
        val shape = when (shapeName) {
            "circle" -> Circle(Pair(center.x, center.z), Pair(start.x, start.z), radius)
            "square" -> Square(Pair(center.x, center.z), Pair(start.x, start.z), radius)
            else -> Square(Pair(center.x, center.z), Pair(start.x, start.z), radius)
        }
        return shape
    }
}