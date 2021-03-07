package net.trivernis.chunkmaster.lib

import net.trivernis.chunkmaster.Chunkmaster
import java.io.*
import java.util.*

class LanguageManager(private val plugin: Chunkmaster) {
    private val langProps = Properties()
    private val languageFolder = "${plugin.dataFolder.absolutePath}/i18n"
    private var langFileLoaded = false

    /**
     * Loads the default properties file and then the language specific ones.
     * If no lang-specific file is found in the plugins directory under i18n an attempt is made to
     * load the file from inside the jar in i18n.
     */
    fun loadProperties() {
        val language = plugin.config.getString("language")
        val langFile = "$languageFolder/$language.i18n.properties"
        val file = File(langFile)
        val loader = Thread.currentThread().contextClassLoader
        val defaultStream = this.javaClass.getResourceAsStream("/i18n/DEFAULT.i18n.properties")

        if (defaultStream != null) {
            langProps.load(getReaderForProperties(defaultStream))
            defaultStream.close()
        } else {
            plugin.logger.severe("Couldn't load default language properties.")
        }

        if (file.exists()) {
            try {
                val inputStream = loader.getResourceAsStream(langFile)
                if (inputStream != null) {
                    langProps.load(getReaderForProperties(inputStream))
                    langFileLoaded = true
                    inputStream.close()
                }
            } catch (e: Exception) {
                plugin.logger.warning("Language file $langFile could not be loaded!")
                plugin.logger.fine(e.toString())
            }
        } else {
            val inputStream = this.javaClass.getResourceAsStream("/i18n/$language.i18n.properties")
            if (inputStream != null) {
                langProps.load(getReaderForProperties(inputStream))
                langFileLoaded = true
                inputStream.close()
            } else {
                plugin.logger.warning("Language File $langFile could not be found!")
            }
        }
    }

    /**
     * Returns a localized message with replacements
     */
    fun getLocalized(key: String, vararg replacements: Any): String {
        try {
            val localizedString = langProps.getProperty(key)
            return String.format(localizedString, *replacements)
        } catch (e: NullPointerException) {
            plugin.logger.severe("Failed to get localized entry for $key")
            throw e
        }
    }

    /**
     * Reads a properties file as utf-8 and returns a string reader for the contents
     */
    private fun getReaderForProperties(stream: InputStream): Reader {
        return BufferedReader(InputStreamReader(stream, "UTF-8"))
    }
}