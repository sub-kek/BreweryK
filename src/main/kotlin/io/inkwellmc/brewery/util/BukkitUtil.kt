package io.inkwellmc.brewery.util

import io.inkwellmc.brewery.Brewery
import io.inkwellmc.brewery.barrel.Barrel
import io.inkwellmc.brewery.barrel.BarrelBody
import io.inkwellmc.brewery.config.BreweryConfig
import io.inkwellmc.brewery.type.BarrelDestroyReason
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.data.type.WallSign
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player

object BukkitUtil {
  fun isChunkLoaded(block: Block): Boolean {
    return block.world.isChunkLoaded(block.x shr 4, block.z shr 4)
  }

  fun blockDestroy(block: Block?, player: Player?, reason: BarrelDestroyReason): Boolean {
    if (block == null || block.type.isAir) {
      return true
    }
    val type = block.type
    if (LegacyUtil.isWoodFence(type)) {
      // Удалить бочку и выбросить предметы
      Barrel.getBySpigot(block)?.remove(null, player, true)
      return true
    } else if (LegacyUtil.isWoodWallSign(type)) {
      // Удалить маленькие бочки
      val barrel2: Barrel? = Barrel.getBySpigot(block)
      if (barrel2 != null) {
        if (!barrel2.isLarge()) {
            barrel2.remove(null, player, true)
            return true
        } else {
          barrel2.body.destroySign()
        }
      }
      return true
    } else if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
      Barrel.getByWood(block)?.let {
        it.remove(block, player, true)
        if (reason == BarrelDestroyReason.BURNED) explodeBarrel(it.body)
      }
    }
    return true
  }

  fun explodeBarrel(barrelBody: BarrelBody) {
    val location: Location = barrelBody.spigot.location
    location.add(0.0, 1.0, 0.0)
    if ((barrelBody.isLarge() && BreweryConfig.largeBarrelExplode) || (barrelBody.isSmall() && BreweryConfig.smallBarrelExplode)) {
      location.add((barrelBody.getSignOfSpigot().blockData as WallSign).facing.direction.multiply(if (barrelBody.isLarge()) -2 else -1))
      Brewery.instance.foliaLib.impl.runAtLocation(location) {
        val armorStand = location.world!!.spawn(location, ArmorStand::class.java)
        armorStand.isInvisible = true
        armorStand.isInvulnerable = true
        armorStand.customName = Brewery.language.get("barrel.title")
        location.world!!.createExplosion(location, if (barrelBody.isLarge()) BreweryConfig.largeBarrelExplodePower else BreweryConfig.smallBarrelExplodePower, true, true, armorStand)
        armorStand.remove()
      }
    }
  }

  fun createWorldSections(section: ConfigurationSection) {
    for (world in Brewery.instance.server.worlds) {
      val worldName: String = world.uid.toString()
      section.createSection(worldName)
    }
  }
}