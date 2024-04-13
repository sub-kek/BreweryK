package io.inkwellmc.brewery

import com.tcoded.folialib.FoliaLib
import io.inkwellmc.brewery.config.BreweryConfig
import io.inkwellmc.brewery.config.YamlLanguage
import io.inkwellmc.brewery.data.BreweryData
import io.inkwellmc.brewery.data.DataSave
import io.inkwellmc.brewery.listener.BlockListener
import io.inkwellmc.brewery.listener.EntityListener
import io.inkwellmc.brewery.listener.PlayerListener
import io.inkwellmc.brewery.listener.WorldListener
import io.inkwellmc.brewery.util.BreweryLogger
import io.inkwellmc.brewery.util.LegacyUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Brewery : JavaPlugin() {
  lateinit var language: YamlLanguage private set
  val foliaLib = FoliaLib(this)

  override fun onEnable() {
    instance = this

    // Инициализация кофнигурации
    BreweryConfig.init()

    // Инициальзация языка
    language = YamlLanguage()
    language.init()

    BreweryLogger.debug(language.get("debug-messages.config-loaded"))

    BreweryData.readData()

    LegacyUtil.debug()

    // Регистрация Ивентов
    server.pluginManager.registerEvents(PlayerListener(), this)
    server.pluginManager.registerEvents(BlockListener(), this)
    server.pluginManager.registerEvents(EntityListener(), this)
    server.pluginManager.registerEvents(WorldListener(), this)
  }

  fun parseInt(string: String?): Int {
    if (string == null) {
      return 0
    }
    return try {
      string.toInt()
    } catch (ignored: NumberFormatException) {
      0
    }
  }

  override fun onDisable() {
    DataSave.save(true)
  }

  companion object {
    lateinit var instance: Brewery private set
    val language: YamlLanguage
      get() = instance.language

    fun message(sender: CommandSender, message: String) {
      sender.sendMessage("${language.get("prefix")}$message")
    }

    fun message(player: Player, message: String) {
      player.sendMessage("${language.get("prefix")}$message")
    }

    fun debugMessage(sender: CommandSender, message: String) {
      if (!BreweryConfig.verboseOutput) return
      sender.sendMessage("${language.get("debug-prefix")}$message")
    }

    fun debugMessage(player: Player, message: String) {
      if (!BreweryConfig.verboseOutput) return
      player.sendMessage("${language.get("debug-prefix")}$message")
    }
  }
}