package net.trivernis.chunkmaster.lib.generation

import io.papermc.lib.PaperLib
import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import org.bukkit.Server
import org.bukkit.World

class GenerationManager(private val chunkmaster: Chunkmaster, private val server: Server) {

    val tasks: HashSet<RunningTaskEntry> = HashSet()
    val pausedTasks: HashSet<PausedTaskEntry> = HashSet()
    val allTasks: HashSet<TaskEntry>
        get() {
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
    fun addTask(world: World, stopAfter: Int = -1): Int {
        val foundTask = allTasks.find { it.generationTask.world == world }
        if (foundTask == null) {
            val centerChunk = ChunkCoordinates(world.spawnLocation.chunk.x, world.spawnLocation.chunk.z)
            val generationTask = createGenerationTask(world, centerChunk, centerChunk, stopAfter)

            chunkmaster.sqliteManager.executeStatement(
                """
                INSERT INTO generation_tasks (center_x, center_z, last_x, last_z, world, stop_after)
                values (?, ?, ?, ?, ?, ?)
                """,
                HashMap(
                    mapOf(
                        1 to centerChunk.x,
                        2 to centerChunk.z,
                        3 to centerChunk.x,
                        4 to centerChunk.z,
                        5 to world.name,
                        6 to stopAfter
                    )
                ),
                null
            )

            var id = 0
            chunkmaster.sqliteManager.executeStatement("""
                SELECT id FROM generation_tasks ORDER BY id DESC LIMIT 1
                """.trimIndent(), HashMap()) {
                it.next()
                id = it.getInt("id")
            }

            generationTask.onEndReached {
                chunkmaster.logger.info(chunkmaster.langManager.getLocalized("TASK_FINISHED", id, it.count))
                removeTask(id)
            }

            if (!paused) {
                val task = server.scheduler.runTaskTimer(
                    chunkmaster, generationTask, 200,  // 10 sec delay
                    chunkmaster.config.getLong("generation.period")
                )
                tasks.add(RunningTaskEntry(id, task, generationTask))
            } else {
                pausedTasks.add(PausedTaskEntry(id, generationTask))
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
        stopAfter: Int = -1,
        delay: Long = 200L
    ) {
        if (!paused) {
            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("RESUME_FOR_WORLD", world.name))
            val generationTask = createGenerationTask(world, center, last, stopAfter)
            val task = server.scheduler.runTaskTimer(
                chunkmaster, generationTask, delay,  // 10 sec delay
                chunkmaster.config.getLong("generation.period")
            )
            tasks.add(RunningTaskEntry(id, task, generationTask))
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
            taskEntry.cancel()
            chunkmaster.sqliteManager.executeStatement("""
                DELETE FROM generation_tasks WHERE id = ?;
                """.trimIndent(), HashMap(mapOf(1 to taskEntry.id)),
                null
            )

            if (taskEntry is RunningTaskEntry) {
                if (taskEntry.task.isCancelled) {
                    tasks.remove(taskEntry)
                }
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
            if (server.onlinePlayers.isEmpty()) {
                startAll()     // run startAll after 10 seconds if empty
            }
        }, 600)
    }

    /**
     * Stops all generation tasks
     */
    fun stopAll() {
        val removalSet = HashSet<RunningTaskEntry>()
        for (task in tasks) {
            val lastChunk = task.generationTask.lastChunkCoords
            val id = task.id
            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("SAVING_TASK_PROGRESS", task.id))
            saveProgressToDatabase(lastChunk, id)
            task.task.cancel()
            task.generationTask.cancel()
            if (task.task.isCancelled) {
                removalSet.add(task)
            }
            chunkmaster.logger.info(chunkmaster.langManager.getLocalized("TASK_CANCELED", task.id))
        }
        tasks.removeAll(removalSet)
    }

    /**
     * Starts all generation tasks.
     */
    fun startAll() {
        chunkmaster.sqliteManager.executeStatement("SELECT * FROM generation_tasks", HashMap()) { res ->
            var count = 0
            while (res.next()) {
                count++
                try {
                    val id = res.getInt("id")
                    val world = server.getWorld(res.getString("world"))
                    val center = ChunkCoordinates(res.getInt("center_x"), res.getInt("center_z"))
                    val last = ChunkCoordinates(res.getInt("last_x"), res.getInt("last_z"))
                    val stopAfter = res.getInt("stop_after")
                    if (this.tasks.find { it.id == id } == null) {
                        resumeTask(world!!, center, last, id, stopAfter, 200L + count)
                    }
                } catch (error: NullPointerException) {
                    chunkmaster.logger.severe(chunkmaster.langManager.getLocalized("TASK_LOAD_FAILED", res.getInt("id")))
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
            pausedTasks.add(PausedTaskEntry(task.id, task.generationTask))
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
                val speed = task.generationSpeed!!
                val percentage = if (genTask.stopAfter > 0) "(${"%.2f".format(
                    (genTask.count.toDouble() / genTask.stopAfter.toDouble()) * 100
                )}%)" else ""
                val eta = if (genTask.stopAfter > 0 && speed > 0) {
                    val etaSeconds = (genTask.stopAfter - genTask.count).toDouble()/speed
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
                    genTask.count,
                    percentage,
                    eta,
                    speed,
                    genTask.lastChunk.x,
                    genTask.lastChunk.z))
                saveProgressToDatabase(genTask.lastChunkCoords, task.id)
            } catch (error: Exception) {
                chunkmaster.logger.warning(chunkmaster.langManager.getLocalized("TASK_SAVE_FAILED", error.toString()))
            }
        }
    }

    /**
     * Saves the generation progress to the database
     */
    private fun saveProgressToDatabase(lastChunk: ChunkCoordinates, id: Int) {
        chunkmaster.sqliteManager.executeStatement(
            """
                    UPDATE generation_tasks SET last_x = ?, last_z = ?
                    WHERE id = ?
                    """.trimIndent(),
            HashMap(mapOf(1 to lastChunk.x, 2 to lastChunk.z, 3 to id)),
            null
        )
    }

    /**
     * Creates a new generation task. This method is used to create a task depending
     * on the server type (Paper/Spigot).
     */
    private fun createGenerationTask(
        world: World,
        center: ChunkCoordinates,
        start: ChunkCoordinates,
        stopAfter: Int
    ): GenerationTask {
        return if (PaperLib.isPaper()) {
            GenerationTaskPaper(chunkmaster, world, center, start, stopAfter)
        } else {
            GenerationTaskSpigot(chunkmaster, world, center, start, stopAfter)
        }
    }
}