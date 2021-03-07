package net.trivernis.chunkmaster.lib
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.every
import io.mockk.mockk
import net.trivernis.chunkmaster.Chunkmaster
import org.bukkit.configuration.file.FileConfiguration
import org.junit.Test

class LanguageManagerTest {
    private var langManager: LanguageManager

    init {
        val plugin = mockk<Chunkmaster>()
        val config = mockk<FileConfiguration>()

        every {plugin.dataFolder} returns createTempDir()
        every {plugin.config} returns config
        every {config.getString("language")} returns "en"

        langManager = LanguageManager(plugin)
        langManager.loadProperties()
    }

    @Test
    fun `it returns localized for a key`() {
        langManager.getLocalized("NOT_PAUSED").shouldNotBeEmpty()
    }
}