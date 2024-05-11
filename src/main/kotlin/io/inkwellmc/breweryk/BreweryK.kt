package io.inkwellmc.breweryk

import com.tcoded.folialib.FoliaLib
import io.inkwellmc.breweryk.config.BreweryConfig
import io.inkwellmc.breweryk.config.YamlLanguage
import io.inkwellmc.breweryk.data.BreweryData
import io.inkwellmc.breweryk.data.DataSave
import io.inkwellmc.breweryk.listener.BlockListener
import io.inkwellmc.breweryk.listener.EntityListener
import io.inkwellmc.breweryk.listener.PlayerListener
import io.inkwellmc.breweryk.listener.WorldListener
import io.inkwellmc.breweryk.util.BreweryLogger
import io.inkwellmc.breweryk.util.LegacyUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.Random

class BreweryK : JavaPlugin() {
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
    lateinit var instance: BreweryK private set
    val language: YamlLanguage
      get() = instance.language

    fun message(sender: CommandSender, vararg message: String) {
      message.forEach {
        sender.sendMessage("${language.get("prefix")}${it}")
      }
    }

    fun message(player: Player, vararg message: String) {
      message.forEach {
        player.sendMessage("${language.get("prefix")}${it}")
      }
    }

    fun debugMessage(sender: CommandSender, vararg message: String) {
      if (!BreweryConfig.verboseOutput) return
      message.forEach {
        sender.sendMessage("${language.get("debug-prefix")}${it}")
      }
    }

    fun debugMessage(player: Player, vararg message: String) {
      if (!BreweryConfig.verboseOutput) return
      message.forEach {
        player.sendMessage("${language.get("debug-prefix")}${it}")
      }
    }
  }
}

fun main() {
  println(Random().nextInt(100000) shl 1)
}