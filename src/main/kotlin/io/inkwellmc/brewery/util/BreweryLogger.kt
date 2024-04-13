package io.inkwellmc.brewery.util

import io.inkwellmc.brewery.Brewery
import io.inkwellmc.brewery.config.BreweryConfig

/**
 * Замена [логгера][java.util.logging.Logger] java
 */
object BreweryLogger {
  /**
   * Отправляет дебаг сообщение в консоль
   *
   * Вывод происходит если [verboseOutput][io.inkwellmc.brewery.config.BreweryConfig.verboseOutput] равен true
   * @param debugMessage сообщение для вывода на консоль
   */
  fun debug(debugMessage: String) {
    if (!BreweryConfig.verboseOutput) return
    Brewery.instance.server.consoleSender.sendMessage(
      "${Brewery.language.get("debug-prefix")}$debugMessage"
    )
  }

  /**
   * Отправляет информационное сообщение в консоль
   * @param infoMessage сообщение для вывода на консоль
   */
  fun info(infoMessage: String) {
    Brewery.instance.server.consoleSender.sendMessage(
      "${Brewery.language.get("prefix")}$infoMessage"
    )
  }
}