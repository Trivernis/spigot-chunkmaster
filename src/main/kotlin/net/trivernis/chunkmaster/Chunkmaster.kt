package net.trivernis.chunkmaster

import net.trivernis.chunkmaster.lib.Spiral
import org.bukkit.plugin.java.JavaPlugin

class Chunkmaster: JavaPlugin() {
    override fun onEnable() {
        configure()
    }

    override fun onDisable() {

    }

    private fun configure() {
        config.options().copyDefaults(true)
    }
}