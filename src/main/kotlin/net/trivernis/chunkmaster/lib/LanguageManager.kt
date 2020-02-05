package net.trivernis.chunkmaster.lib
import net.trivernis.chunkmaster.Chunkmaster
import java.io.File
import java.lang.Exception
import java.util.Properties
import java.util.logging.Level

class LanguageManager(private val plugin: Chunkmaster) {
    private val langProps = Properties()
    private val languageFolder = "${plugin.dataFolder.absolutePath}/i18n"
    private var langFileLoaded = false

    fun loadProperties() {
        val language = plugin.config.getString("language")
        val langFile = "$languageFolder/$language.i18n.properties"
        val file = File(langFile)
        val loader = Thread.currentThread().contextClassLoader
        val defaultStream = this.javaClass.getResourceAsStream("/DEFAULT.i18n.properties")
        if (defaultStream != null) {
            langProps.load(defaultStream)
            defaultStream.close()
        } else {
            plugin.logger.severe("Couldn't load default language properties.")
        }
        if (file.exists()) {
            try {
                val inputStream = loader.getResourceAsStream(langFile)
                if (inputStream != null) {
                    langProps.load(inputStream)
                    langFileLoaded = true
                    inputStream.close()
                }
            } catch (e: Exception)  {
                plugin.logger.warning("Language file $langFile could not be loaded!")
                plugin.logger.fine(e.toString())
            }
        } else {
            plugin.logger.warning("Language File $langFile could not be found!")
        }
    }

    fun getLocalized(key: String) {
        langProps.getProperty(key)
    }
}