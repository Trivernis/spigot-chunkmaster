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
    val worldCenters: HashMap<String, Pair<Int, Int>> = HashMap()
    val loadedChunkCount: Int
        get() {
            return unloader.pendingSize
        }
    private val unloader = ChunkUnloader(chunkmaster)


    val allTasks: HashSet<TaskEntry>
        get() {
            if (this.tasks.isEmpty() && this.pausedTasks.isEmpty()) {
                if (this.worldCenters.isEmpty()) {
                    this.loadWorldCenters()
                }
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
            val centerChunk = if (worldCenters[world.name] == null) {
                ChunkCoordinates(world.spawnLocation.chunk.x, world.spawnLocation.chunk.z)
            } else {
                val center = worldCenters[world.name]!!
                ChunkCoordinates(center.first, center.second)
            }
            val generationTask = createGenerationTask(world, centerChunk, centerChunk, radius, shape, null)

            chunkmaster.sqliteManager.executeStatement(
                """
                INSERT INTO generation_tasks (center_x, center_z, last_x, last_z, world, radius, shape)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                HashMap(
                    mapOf(
                        1 to centerChunk.x,
                        2 to centerChunk.z,
                        3 to centerChunk.x,
                        4 to centerChunk.z,
                        5 to world.name,
                        6 to radius,
                        7 to shape
                    )
                ),
                null
            )

            var id = 0
            chunkmaster.sqliteManager.executeStatement("""
                SELECT id FROM generation_tasks ORDER BY id DESC LIMIT 1
                """.trimIndent(), HashMap()) {
                it!!.next()
                id = it.getInt("id")
            }

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
            chunkmaster.sqliteManager.executeStatement("""
                DELETE FROM generation_tasks WHERE id = ?;
                """.trimIndent(), HashMap(mapOf(1 to taskEntry.id)),
                null
            )
            deletePendingChunks(id)

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
            this.loadWorldCenters()
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
        chunkmaster.sqliteManager.executeStatement("SELECT * FROM generation_tasks", HashMap()) { res ->
            var count = 0
            while (res!!.next()) {
                count++
                try {
                    val id = res.getInt("id")
                    val world = server.getWorld(res.getString("world"))
                    val center = ChunkCoordinates(res.getInt("center_x"), res.getInt("center_z"))
                    val last = ChunkCoordinates(res.getInt("last_x"), res.getInt("last_z"))
                    val radius = res.getInt("radius")
                    val shape = res.getString("shape")
                    if (this.tasks.find { it.id == id } == null) {
                        loadPendingChunks(id) {
                            resumeTask(world!!, center, last, id, radius, shape, it)
                        }
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
     * Loads all pending chunks for a world and invokes the callback with the result
     */
    private fun loadPendingChunks(taskId: Int, cb: ((List<ChunkCoordinates>) -> Unit)?) {
        chunkmaster.sqliteManager.executeStatement("SELECT * FROM paper_pending_chunks WHERE task_id = ?",hashMapOf(1 to taskId)) {
            val pendingChunks = ArrayList<ChunkCoordinates>()
            while (it!!.next()) {
                pendingChunks.add(ChunkCoordinates(it.getInt("chunk_x"), it.getInt("chunk_z")))
            }
            cb?.invoke(pendingChunks)
        }
    }

    /**
     * Deletes all pending chunks from the database
     */
    private fun deletePendingChunks(taskId: Int) {
        chunkmaster.sqliteManager.executeStatement("DELETE FROM paper_pending_chunks WHERE task_id = ?", hashMapOf(1 to taskId), null)
    }

    /**
     * Overload that doesn't need an argument
     */
    private fun loadWorldCenters() {
        loadWorldCenters(null)
    }

    /**
     * Loads the world centers from the database
     */
    fun loadWorldCenters(cb: (() -> Unit)?) {
        chunkmaster.sqliteManager.executeStatement("SELECT * FROM world_properties", HashMap()) {
            while (it!!.next()) {
                worldCenters[it.getString("name")] = Pair(it.getInt("center_x"), it.getInt("center_z"))
            }
            cb?.invoke()
        }
    }

    /**
     * Updates the center of a world
     */
    fun updateWorldCenter(worldName: String, center: Pair<Int, Int>) {
        chunkmaster.sqliteManager.executeStatement("SELECT * FROM world_properties WHERE name = ?", HashMap(mapOf(1 to worldName))) {
            if (it!!.next()) {
                chunkmaster.sqliteManager.executeStatement("UPDATE world_properties SET center_x = ?, center_z = ? WHERE name = ?",
                    hashMapOf(
                        1 to center.first,
                        2 to center.second,
                        3 to worldName
                    )
                , null)
            } else {
                chunkmaster.sqliteManager.executeStatement("INSERT INTO world_properties (name, center_x, center_z) VALUES (?, ?, ?)",
                    hashMapOf(
                        1 to worldName,
                        2 to center.first,
                        3 to center.second
                    )
                , null)
            }
        }
        worldCenters[worldName] = center
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
                    genTask.count,
                    percentage,
                    eta,
                    chunkSpeed!!,
                    genTask.lastChunkCoords.x,
                    genTask.lastChunkCoords.z))
                saveProgressToDatabase(genTask, task.id)
                genTask.updateLastChunkMarker()
            } catch (error: Exception) {
                chunkmaster.logger.warning(chunkmaster.langManager.getLocalized("TASK_SAVE_FAILED", error.toString()))
            }
        }
    }

    /**
     * Saves the generation progress to the database
     */
    private fun saveProgressToDatabase(generationTask: GenerationTask, id: Int) {
        val lastChunk = generationTask.lastChunkCoords
        chunkmaster.sqliteManager.executeStatement(
            """
                    UPDATE generation_tasks SET last_x = ?, last_z = ?
                    WHERE id = ?
                    """.trimIndent(),
            HashMap(mapOf(1 to lastChunk.x, 2 to lastChunk.z, 3 to id))
        ) {
            if (generationTask is GenerationTaskPaper) {
                this.deletePendingChunks(id)
                var sql = "INSERT INTO paper_pending_chunks (task_id, chunk_x, chunk_z) VALUES"
                var index = 1
                val valueMap= HashMap<Int, Any>()

                for (pending in generationTask.pendingChunks) {
                    sql += "(?, ?, ?),"
                    valueMap[index++] = id
                    valueMap[index++] = pending.coordinates.x
                    valueMap[index++] = pending.coordinates.z
                }
                chunkmaster.sqliteManager.executeStatement(sql.removeSuffix(","), valueMap, null)
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
                pendingChunks ?: emptyList()
            )
        } else {
            GenerationTaskSpigot(
                chunkmaster,
                unloader,
                world,
                start,
                radius,
                shape,
                pendingChunks ?: emptyList()
            )
        }
    }
}