package net.trivernis.chunkmaster.lib

import javafx.concurrent.Task
import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.Chunk
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.scheduler.BukkitTask
import java.lang.Exception
import java.lang.NullPointerException

class GenerationManager(private val chunkmaster: Chunkmaster, private val server: Server) {

    val tasks: HashSet<TaskEntry> = HashSet()
        get() = field

    /**
     * Adds a generation task
     */
    fun addTask(world: World): Int {
        val centerChunk = world.getChunkAt(world.spawnLocation)
        val generationTask = GenerationTask(chunkmaster, world, centerChunk, centerChunk)
        val task = server.scheduler.runTaskTimer(chunkmaster, generationTask, 10, 2)
        val insertStatement = chunkmaster.sqliteConnection.prepareStatement("""
            INSERT INTO generation_tasks (center_x, center_z, last_x, last_z, world)
            values (?, ?, ?, ?, ?)
            """)
        insertStatement.setInt(1, centerChunk.x)
        insertStatement.setInt(2, centerChunk.z)
        insertStatement.setInt(3, centerChunk.x)
        insertStatement.setInt(4, centerChunk.z)
        insertStatement.setString(5, world.name)
        insertStatement.execute()
        val getIdStatement = chunkmaster.sqliteConnection.prepareStatement("""
            SELECT id FROM generation_tasks ORDER BY id DESC LIMIT 1
        """.trimIndent())
        getIdStatement.execute()
        val result = getIdStatement.resultSet
        result.next()
        val id: Int = result.getInt("id")
        tasks.add(TaskEntry(id, task, generationTask))
        insertStatement.close()
        getIdStatement.close()
        return id
    }

    /**
     * Resumes a generation task
     */
    private fun resumeTask(world: World, center: Chunk, last: Chunk, id: Int) {
        chunkmaster.logger.info("Resuming chunk generation task for world \"${world.name}\"")
        val generationTask = GenerationTask(chunkmaster, world, center, last)
        val task = server.scheduler.runTaskTimer(chunkmaster, generationTask, 10, 2)
        tasks.add(TaskEntry(id, task, generationTask))
    }

    /**
     * Stops a running generation task.
     */
    fun removeTask(id: Int) {
        val taskEntry = this.tasks.find {it.id == id}
        if (taskEntry != null) {
            taskEntry.generationTask.cancel()
            taskEntry.task.cancel()
            if (taskEntry.task.isCancelled) {
                tasks.remove(taskEntry)
            }
            val setAutostart = chunkmaster.sqliteConnection.prepareStatement("""
                UPDATE TABLE generation_tasks SET autostart = 0 WHERE id = ?
            """.trimIndent())
            setAutostart.setInt(1, id)
            setAutostart.execute()
            setAutostart.close()
        }
    }

    /**
     * Init
     * Loads tasks from the database and resumes them
     */
    fun init() {
        chunkmaster.logger.info("Creating task to load chunk generation Tasks later...")
        server.scheduler.runTaskLater(chunkmaster, Runnable {
            if (server.onlinePlayers.isEmpty()) {
                startAll()     // run startAll after 10 seconds if empty
            }
        }, 200)
    }

    /**
     * Stops all generation tasks
     */
    fun stopAll() {
        saveProgress()
        for (task in tasks) {
            task.generationTask.cancel()
            task.task.cancel()
            if (task.task.isCancelled) {
                tasks.remove(task)
            }
            chunkmaster.logger.info("Canceled task #${task.id}")
        }
    }

    /**
     * Starts all generation tasks.
     */
    fun startAll() {
        chunkmaster.logger.info("Loading saved chunk generation tasks...")
        val savedTasksStatement = chunkmaster.sqliteConnection.prepareStatement("SELECT * FROM generation_tasks")
        savedTasksStatement.execute()
        val res = savedTasksStatement.resultSet
        while (res.next()) {
            try {
                if (res.getBoolean("autostart")) {
                    val id = res.getInt("id")
                    val world = server.getWorld(res.getString("world"))
                    val center = world!!.getChunkAt(res.getInt("center_x"), res.getInt("center_z"))
                    val last = world.getChunkAt(res.getInt("last_x"), res.getInt("last_z"))
                    if (this.tasks.find {it.id == id} == null) {
                        resumeTask(world, center, last, id)
                    }
                }
            } catch (error: NullPointerException) {
                server.consoleSender.sendMessage("Failed to load Task ${res.getInt("id")}.")
            }
        }
        savedTasksStatement.close()
        server.scheduler.runTaskTimer(chunkmaster, Runnable {
            saveProgress()      // save progress every 30 seconds
        }, 600, 600)
        chunkmaster.logger.info("${tasks.size} saved tasks loaded.")
    }

    /**
     * Saves the task progress
     */
    private fun saveProgress() {
        for (task in tasks) {
            try {
                val genTask = task.generationTask
                server.consoleSender.sendMessage("Task #${task.id} running for \"${genTask.world.name}\". " +
                        "Progress ${task.generationTask.count} chunks. Last Chunk: ${genTask.lastChunk.x}, ${genTask.lastChunk.z}")
                val updateStatement = chunkmaster.sqliteConnection.prepareStatement("""
                    UPDATE generation_tasks SET last_x = ?, last_z = ?
                    WHERE id = ?
                    """.trimIndent())
                updateStatement.setInt(1, genTask.lastChunk.x)
                updateStatement.setInt(2, genTask.lastChunk.z)
                updateStatement.setInt(3, task.id)
                updateStatement.execute()
                updateStatement.close()
            } catch (error: Exception) {
                server.consoleSender.sendMessage("Exception when saving task progress ${error.message}")
            }
        }
    }
}