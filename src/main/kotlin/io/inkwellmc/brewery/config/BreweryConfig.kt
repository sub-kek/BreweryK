@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.inkwellmc.brewery.config

import io.inkwellmc.brewery.Brewery
import org.simpleyaml.configuration.comments.CommentType
import org.simpleyaml.configuration.file.YamlFile
import java.io.IOException
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.util.logging.Level

object BreweryConfig {
  private val plugin = Brewery.instance
  private val config = YamlFile()

  fun init() {
    val configFile = Path.of(plugin.dataFolder.path, "config.yml").toFile()

    if (configFile.exists()) {
      try {
        config.load(configFile)
      } catch (e: IOException) {
        plugin.logger.log(Level.SEVERE, "Error while load config: ", e)
      }
    }

    getString("info.version", "1.0", "Don't change this value")
    setComment("info",
      "Brewery Configuration",
      "Join our Discord for support: https://discord.gg/eRvwvmEXWz")

    for (method in BreweryConfig::class.java.declaredMethods) {
      if (Modifier.isPrivate(method.modifiers) && method.parameterCount == 0 && method.returnType == Void.TYPE && !method.name.startsWith("lambda")) {
        method.isAccessible = true
        try {
          method.invoke(this)
        } catch (t: Throwable) {
          plugin.logger.log(Level.WARNING, "Failed to load configuration option from " + method.name, t)
        }
      }
    }

    try {
      config.save(configFile)
    } catch (e: IOException) {
      plugin.logger.log(Level.SEVERE, "Error while save config: ", e)
    }
  }

  private fun setComment(key: String, vararg comment: String) {
    if (config.contains(key) && comment.isNotEmpty()) {
      config.setComment(key, java.lang.String.join("\n", *comment), CommentType.BLOCK)
    }
  }

  private fun ensureDefault(key: String, defaultValue: Any, vararg comment: String) {
    if (!config.contains(key)) config[key] = defaultValue

    setComment(key, *comment)
  }

  private fun getBoolean(key: String, defaultValue: Boolean, vararg comment: String): Boolean {
    ensureDefault(key, defaultValue, *comment)
    return config.getBoolean(key, defaultValue)
  }

  private fun getInt(key: String, defaultValue: Int, vararg comment: String): Int {
    ensureDefault(key, defaultValue, *comment)
    return config.getInt(key, defaultValue)
  }

  private fun getDouble(key: String, defaultValue: Double, vararg comment: String): Double {
    ensureDefault(key, defaultValue, *comment)
    return config.getDouble(key, defaultValue)
  }

  private fun getString(key: String, defaultValue: String, vararg comment: String): String {
    ensureDefault(key, defaultValue, *comment)
    return config.getString(key, defaultValue)
  }

  private fun getStringList(key: String, defaultValue: List<String>, vararg comment: String): MutableList<String> {
    ensureDefault(key, defaultValue, *comment)
    return config.getStringList(key)
  }

  var verboseOutput = false
  var alwaysSaveLanguage = false
  private fun debugSettings() {
    verboseOutput = getBoolean("debug.verbose", verboseOutput)
    alwaysSaveLanguage = getBoolean("debug.always-save-language", alwaysSaveLanguage)
  }

  var locale = Language.ENGLISH.label
  private fun globalSettings() {
    locale = getString("global.locale", locale, "Language of plugin", "Supported: ${Language.allSeparatedComma}")
  }

  var loadDataAsync = true
  private fun dataSettings() {
    loadDataAsync = getBoolean("data.load-async", loadDataAsync)
  }

  var barrelCreateLines: MutableList<String> = ArrayList()
  var barrelOpenEverywhere = true
  private fun barrelSettings() {
    barrelCreateLines.add("barrel")

    barrelCreateLines = getStringList("barrel.create-lines", barrelCreateLines)
    barrelOpenEverywhere = getBoolean("barrel.open-everywhere", barrelOpenEverywhere)
  }

  var smallBarrelExplode = true
  var largeBarrelExplode = true
  var smallBarrelExplodePower = 3f
  var largeBarrelExplodePower = 6f
  private fun barrelExplosionSettings() {
    smallBarrelExplode = getBoolean("barrel.explode.small", smallBarrelExplode)
    largeBarrelExplode = getBoolean("barrel.explode.large", largeBarrelExplode)
    smallBarrelExplodePower = getString("barrel.explode.small-power", smallBarrelExplodePower.toString()).toFloat()
    largeBarrelExplodePower = getString("barrel.explode.large-power", largeBarrelExplodePower.toString()).toFloat()
  }
}