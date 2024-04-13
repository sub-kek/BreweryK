package io.inkwellmc.brewery.listener

import io.inkwellmc.brewery.barrel.Barrel
import io.inkwellmc.brewery.config.BreweryConfig
import io.inkwellmc.brewery.util.LegacyUtil
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class PlayerListener : Listener {
  @EventHandler
  fun onBarrelOpen(event: PlayerInteractEvent) {
    val clickedBlock: Block = event.clickedBlock ?: return
    if (event.action != Action.RIGHT_CLICK_BLOCK) return

    val player = event.player
    val clickedMaterial = clickedBlock.type

    if (event.hand != EquipmentSlot.HAND) return

    var barrel: Barrel? = null
    if (LegacyUtil.isWoodPlanks(clickedMaterial) && BreweryConfig.barrelOpenEverywhere) {
      barrel = Barrel.getByWood(clickedBlock)
    } else if (LegacyUtil.isWoodStairs(clickedMaterial)) {
      barrel = Barrel.getByWood(clickedBlock)
      if (barrel != null && !BreweryConfig.barrelOpenEverywhere && barrel.isLarge()) barrel = null
    } else if (LegacyUtil.isWoodFence(clickedMaterial) || LegacyUtil.isWoodWallSign(clickedMaterial)) barrel = Barrel.getBySpigot(clickedBlock)

    if (barrel != null) {
      event.isCancelled = true

      barrel.openInventory(player)

      barrel.playOpeningSound()
    }
  }

  @EventHandler
  fun onBarrelClose(event: InventoryCloseEvent) {
    if (event.inventory.holder !is Barrel) return

    val barrel = event.inventory.holder as Barrel
    barrel.playClosingSound()
  }
}
