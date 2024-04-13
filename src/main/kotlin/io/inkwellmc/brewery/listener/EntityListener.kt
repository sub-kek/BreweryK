package io.inkwellmc.brewery.listener

import io.inkwellmc.brewery.barrel.Barrel
import io.inkwellmc.brewery.util.BukkitUtil
import io.inkwellmc.brewery.util.LegacyUtil
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent

class EntityListener : Listener {
  @EventHandler
  fun onExplode(event: EntityExplodeEvent) {
    val iterator = event.blockList().listIterator()
    if (!iterator.hasNext()) return
    var block: Block?
    while (iterator.hasNext()) {
      block = iterator.next()
      if (!LegacyUtil.isBarrelMaterial(block.type)) continue
      val barrel = Barrel.get(block)
      if (barrel != null) {
        barrel.remove(block, null, true)
        BukkitUtil.explodeBarrel(barrel.body)
      }
    }
  }
}