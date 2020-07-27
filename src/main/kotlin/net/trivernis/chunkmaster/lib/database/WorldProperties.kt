package net.trivernis.chunkmaster.lib.database

import java.util.concurrent.CompletableFuture

class WorldProperties(private val sqliteManager: SqliteManager) {

    private val properties = HashMap<String, Pair<Int, Int>>()

    /**
     * Returns the world center for one world
     */
    fun getWorldCenter(worldName: String): CompletableFuture<Pair<Int, Int>?> {
        val completableFuture = CompletableFuture<Pair<Int, Int>?>()

        if (properties[worldName] != null) {
            completableFuture.complete(properties[worldName])
        } else {
            sqliteManager.executeStatement("SELECT * FROM world_properties WHERE name = ?", hashMapOf(1 to worldName)) {
                if (it != null && it.next()) {
                    completableFuture.complete(Pair(it.getInt("center_x"), it.getInt("center_z")))
                } else {
                    completableFuture.complete(null)
                }
            }
        }

        return completableFuture
    }

    /**
     * Updates the center of a world
     */
    fun setWorldCenter(worldName: String, center: Pair<Int, Int>): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()

        getWorldCenter(worldName).thenAccept {
            if (it != null) {
                updateWorldProperties(worldName, center).thenAccept { completableFuture.complete(null) }
            } else {
                insertWorldProperties(worldName, center).thenAccept { completableFuture.complete(null) }
            }
        }
        return completableFuture
    }

    /**
     * Updates an entry in the world properties
     */
    private fun updateWorldProperties(worldName: String, center: Pair<Int, Int>): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        sqliteManager.executeStatement(
            "UPDATE world_properties SET center_x = ?, center_z = ? WHERE name = ?",
            hashMapOf(
                1 to center.first,
                2 to center.second,
                3 to worldName
            )
        ) {
            properties[worldName] = center
            completableFuture.complete(null)
        }
        return completableFuture
    }

    /**
     * Inserts into the world properties
     */
    private fun insertWorldProperties(worldName: String, center: Pair<Int, Int>): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        sqliteManager.executeStatement(
            "INSERT INTO world_properties (name, center_x, center_z) VALUES (?, ?, ?)",
            hashMapOf(
                1 to worldName,
                2 to center.first,
                3 to center.second
            )
        ) {
            properties[worldName] = center
            completableFuture.complete(null)
        }
        return completableFuture
    }
}