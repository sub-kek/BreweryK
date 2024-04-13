package io.inkwellmc.brewery.listener

import io.inkwellmc.brewery.Brewery
import io.inkwellmc.brewery.barrel.Barrel
import io.inkwellmc.brewery.config.BreweryConfig
import io.inkwellmc.brewery.type.BarrelDestroyReason
import io.inkwellmc.brewery.util.BukkitUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.SignChangeEvent

class BlockListener : Listener {
  @EventHandler
  fun onBlockBreak(event: BlockBreakEvent) {
    if (!BukkitUtil.blockDestroy(event.block, event.player, BarrelDestroyReason.PLAYER)) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onBlockBurn(event: BlockBurnEvent) {
    if (!BukkitUtil.blockDestroy(event.block, null, BarrelDestroyReason.BURNED)) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onSignChange(event: SignChangeEvent) {
    if (hasBarrelLine(event.lines)) {
      val player = event.player
      if (Barrel.create(event.block, player)) {
        Brewery.message(player, Brewery.language.get("barrel-messages.barrel-created"))
      }
    }
  }

  private fun hasBarrelLine(lines: Array<String?>): Boolean {
    for (line in lines) {
      for (configLine in BreweryConfig.barrelCreateLines) {
        if (configLine.equals(line, ignoreCase = true)) return true
      }
    }
    return false
  }
}