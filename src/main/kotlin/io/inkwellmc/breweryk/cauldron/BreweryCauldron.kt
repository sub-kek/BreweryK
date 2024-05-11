package io.inkwellmc.breweryk.cauldron

import io.inkwellmc.breweryk.BreweryK
import io.inkwellmc.breweryk.barrel.Barrel.Companion.barrels
import io.inkwellmc.breweryk.util.BukkitUtil
import io.inkwellmc.breweryk.util.LegacyUtil
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import java.util.*

class BreweryCauldron(val block: Block) {
  companion object {
    var cauldrons: MutableList<BreweryCauldron> = Collections.synchronizedList(ArrayList())

    fun getByBlock(block: Block): BreweryCauldron? {
      if (CauldronType.fromMaterial(block.type) != CauldronType.UNKNOWN && isCauldronHasHeatSource(block)) {
        cauldrons.forEach { if (it.block == block) return it }
        val cauldron = BreweryCauldron(block)
        cauldrons.add(cauldron)
        return cauldron
      }
      return null
    }

    fun isCauldronHasHeatSource(block: Block): Boolean {
      val underBlock = block.getRelative(BlockFace.DOWN)
      return LegacyUtil.isCauldronHeatSource(underBlock)
    }

    fun save(config: ConfigurationSection) {
      BukkitUtil.createWorldSections(config)

      if (barrels.isNotEmpty()) {
        for ((id, cauldron) in cauldrons.withIndex()) {
          val prefix = "${cauldron.block.world.uid}.$id"
          // block: x/y/z
          config["$prefix.block"] = "${cauldron.block.x}/${cauldron.block.y}/${cauldron.block.z}"
        }
      }
    }
  }

  fun sendStatusMessage(player: Player) {
    BreweryK.debugMessage(player, "Это котел Brewery", "Тип: ${CauldronType.fromMaterial(block.type)}")
  }
}