package io.inkwellmc.breweryk.util

import io.inkwellmc.breweryk.BreweryK
import io.inkwellmc.breweryk.config.BreweryConfig

/**
 * Замена [логгера][java.util.logging.Logger] java
 */
object BreweryLogger {
  /**
   * Отправляет дебаг сообщение в консоль
   *
   * Вывод происходит если [verboseOutput][io.inkwellmc.breweryk.config.BreweryConfig.verboseOutput] равен true
   * @param debugMessage сообщение для вывода на консоль
   */
  fun debug(debugMessage: String) {
    if (!BreweryConfig.verboseOutput) return
    BreweryK.instance.server.consoleSender.sendMessage(
      "${BreweryK.language.get("debug-prefix")}$debugMessage"
    )
  }

  /**
   * Отправляет информационное сообщение в консоль
   *
   * @param infoMessage сообщение для вывода на консоль
   */
  fun info(infoMessage: String) {
    BreweryK.instance.server.consoleSender.sendMessage(
      "${BreweryK.language.get("prefix")}$infoMessage"
    )
  }
}