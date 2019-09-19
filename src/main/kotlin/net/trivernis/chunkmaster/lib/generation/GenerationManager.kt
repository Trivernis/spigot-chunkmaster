package net.trivernis.chunkmaster.lib.generation

import io.papermc.lib.PaperLib
import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import org.bukkit.Server
import org.bukkit.World
import java.lang.Exception
import java.lang.NullPointerException

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

            val insertStatement = chunkmaster.sqliteConnection.prepareStatement(
                """
            INSERT INTO generation_tasks (center_x, center_z, last_x, last_z, world, stop_after)
            values (?, ?, ?, ?, ?, ?)
            """
            )
            insertStatement.setInt(1, centerChunk.x)
            insertStatement.setInt(2, centerChunk.z)
            insertStatement.setInt(3, centerChunk.x)
            insertStatement.setInt(4, centerChunk.z)
            insertStatement.setString(5, world.name)
            insertStatement.setInt(6, stopAfter)
            insertStatement.execute()

            val getIdStatement = chunkmaster.sqliteConnection.prepareStatement(
                """
            SELECT id FROM generation_tasks ORDER BY id DESC LIMIT 1
        """.trimIndent()
            )
            getIdStatement.execute()
            val result = getIdStatement.resultSet
            result.next()
            val id: Int = result.getInt("id")

            insertStatement.close()
            getIdStatement.close()

            generationTask.onEndReached {
                chunkmaster.logger.info("Task #${id} finished after ${generationTask.count} chunks.")
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
        stopAfter: Int = -1
    ) {
        if (!paused) {
            chunkmaster.logger.info("Resuming chunk generation task for world \"${world.name}\"")
            val generationTask = createGenerationTask(world, center, last, stopAfter)
            val task = server.scheduler.runTaskTimer(
                chunkmaster, generationTask, 200,  // 10 sec delay
                chunkmaster.config.getLong("generation.period")
            )
            tasks.add(RunningTaskEntry(id, task, generationTask))
            generationTask.onEndReached {
                chunkmaster.logger.info("Task #${id} finished after ${generationTask.count} chunks.")
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
            val deleteTask = chunkmaster.sqliteConnection.prepareStatement(
                """
                DELETE FROM generation_tasks WHERE id = ?;
            """.trimIndent()
            )
            deleteTask.setInt(1, taskEntry.id)
            deleteTask.execute()
            deleteTask.close()

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
        chunkmaster.logger.info("Creating task to load chunk generation Tasks later...")
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
        saveProgress()
        val removalSet = HashSet<RunningTaskEntry>()
        for (task in tasks) {
            task.generationTask.cancel()
            task.task.cancel()
            if (task.task.isCancelled) {
                removalSet.add(task)
            }
            chunkmaster.logger.info("Canceled task #${task.id}")
        }
        tasks.removeAll(removalSet)
    }

    /**
     * Starts all generation tasks.
     */
    fun startAll() {
        val savedTasksStatement = chunkmaster.sqliteConnection.prepareStatement("SELECT * FROM generation_tasks")
        savedTasksStatement.execute()
        val res = savedTasksStatement.resultSet
        while (res.next()) {
            try {
                val id = res.getInt("id")
                val world = server.getWorld(res.getString("world"))
                val center = ChunkCoordinates(res.getInt("center_x"), res.getInt("center_z"))
                val last = ChunkCoordinates(res.getInt("last_x"), res.getInt("last_z"))
                val stopAfter = res.getInt("stop_after")
                if (this.tasks.find { it.id == id } == null) {
                    resumeTask(world!!, center, last, id, stopAfter)
                }
            } catch (error: NullPointerException) {
                chunkmaster.logger.severe("Failed to load Task ${res.getInt("id")}.")
            }
        }
        savedTasksStatement.close()
        if (tasks.isNotEmpty()) {
            chunkmaster.logger.info("${tasks.size} saved tasks loaded.")
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
                chunkmaster.logger.info(
                    """Task #${task.id} running for "${genTask.world.name}".
                    |Progress ${task.generationTask.count} chunks
                    |${if (task.generationTask.stopAfter > 0) "(${"%.2f".format((task.generationTask.count.toDouble() /
                            task.generationTask.stopAfter.toDouble()) * 100)}%)" else ""}.
                    | Speed: ${"%.1f".format(task.generationSpeed)} chunks/sec,
                    |Last Chunk: ${genTask.lastChunk.x}, ${genTask.lastChunk.z}""".trimMargin("|").replace('\n', ' ')
                )
                val updateStatement = chunkmaster.sqliteConnection.prepareStatement(
                    """
                    UPDATE generation_tasks SET last_x = ?, last_z = ?
                    WHERE id = ?
                    """.trimIndent()
                )
                updateStatement.setInt(1, genTask.lastChunk.x)
                updateStatement.setInt(2, genTask.lastChunk.z)
                updateStatement.setInt(3, task.id)
                updateStatement.execute()
                updateStatement.close()
            } catch (error: Exception) {
                chunkmaster.logger.warning("Exception when saving task progress ${error.message}")
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
        stopAfter: Int
    ): GenerationTask {
        return if (PaperLib.isPaper()) {
            GenerationTaskPaper(chunkmaster, world, center, start, stopAfter)
        } else {
            GenerationTaskSpigot(chunkmaster, world, center, start, stopAfter)
        }
    }
}