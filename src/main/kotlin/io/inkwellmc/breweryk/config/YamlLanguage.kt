@file:Suppress("MemberVisibilityCanBePrivate")

package io.inkwellmc.breweryk.config

import io.inkwellmc.breweryk.BreweryK
import org.bukkit.ChatColor
import org.simpleyaml.configuration.file.YamlFile
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.logging.Level

class YamlLanguage {
  private val plugin: BreweryK = BreweryK.instance
  private val language = YamlFile()

  fun init() {
    try {
      val languageFolder = Path.of(plugin.dataFolder.path, "language").toFile()
      languageFolder.mkdir()
      val languageFile = File(languageFolder, "${BreweryConfig.locale}.yml")
      var isNewFile = false

      if (!languageFile.exists()) {
        val inputStream: InputStream = plugin.javaClass.classLoader.getResourceAsStream(
          "language${File.separator}${if (languageExists(BreweryConfig.locale)) BreweryConfig.locale else Language.ENGLISH.label}.yml"
        )!!
        Files.copy(inputStream, languageFile.toPath())
        isNewFile = true
      }

      language.load(languageFile)

      if (isNewFile) {
        language["version"] = plugin.description.version
        language.save(languageFile)
      }

      if (language.getString("version") != plugin.description.version && !isNewFile || BreweryConfig.alwaysSaveLanguage) {
        val oldLanguage = language["language"]
        languageFile.delete()
        val inputStream: InputStream = plugin.javaClass.classLoader.getResourceAsStream(
          "language${File.separator}${BreweryConfig.locale}.yml"
        )!!
        Files.copy(inputStream, languageFile.toPath())
        language.load(languageFile)
        language["version"] = plugin.description.version
        language["language-old"] = oldLanguage
        language.save(languageFile)
      }
    } catch (e: Throwable) {
      plugin.logger.log(Level.SEVERE, "Error while loading language: ", e)
    }
  }

  fun get(key: String, vararg replace: String): String {
    return translateColor(Formatter.format(language.getString("language.$key", "<unknown: $key>"), *replace))
  }

  fun languageExists(label: String): Boolean {
    val inputStream: InputStream? =
      plugin.javaClass.classLoader.getResourceAsStream("language${File.separator}$label.yml")
    return !Objects.isNull(inputStream)
  }

  fun translateColor(input: String): String {
    return ChatColor.translateAlternateColorCodes('&', input)
  }

  private object Formatter {
    fun format(input: String, vararg replace: String): String {
      var output = input
      for (i in replace.indices) {
        output = output.replace("{$i}", replace[i])
      }
      return output
    }
  }
}
