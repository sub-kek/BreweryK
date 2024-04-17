package io.inkwellmc.breweryk.util

import io.inkwellmc.breweryk.cauldron.CauldronHeatSource
import io.inkwellmc.breweryk.cauldron.CauldronType
import io.inkwellmc.breweryk.type.WoodType
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.Lightable
import org.bukkit.block.data.type.Stairs

object LegacyUtil {
  private val stairs = HashSet<Material>()
  private val planks = HashSet<Material>()
  private val fences = HashSet<Material>()
  private val signs = HashSet<Material>()

  init {
    // Получение всех доступных типов дерева
    Tag.WOODEN_STAIRS.values.forEach { stairs.add(it) }
    Tag.PLANKS.values.forEach { planks.add(it) }
    Tag.WOODEN_FENCES.values.forEach { fences.add(it) }
    Tag.WALL_SIGNS.values.forEach { signs.add(it) }
  }

  fun getWoodType(material: Material): WoodType? {
    WoodType.entries.forEach { if (material.name.startsWith(it.name)) return it }
    return null
  }

  fun isBarrelMaterial(material: Material): Boolean {
    return isWoodPlanks(material) || isWoodStairs(material) || isWoodFence(material) || isWoodWallSign(material)
  }

  fun isWoodPlanks(material: Material): Boolean {
    return planks.contains(material)
  }

  fun isWoodStairs(material: Material): Boolean {
    return stairs.contains(material)
  }

  fun isWoodFence(material: Material): Boolean {
    return fences.contains(material)
  }

  fun isWoodWallSign(material: Material): Boolean {
    return signs.contains(material)
  }

  fun areStairsInverted(block: Block): Boolean {
    val data = block.blockData
    return data is Stairs && data.half == Bisected.Half.TOP
  }

  private fun isLitCampfire(block: Block): Boolean {
    val blockData = block.blockData
    if (blockData is Lightable) return blockData.isLit
    return false
  }

  fun isCauldronHeatSource(block: Block): Boolean {
    val typeName = block.type.name

    CauldronHeatSource.entries.forEach {
      if (it.name == typeName) {
        if (it.needLitCheck) return isLitCampfire(block)
        return true
      }
    }

    return false
  }

  fun debug() {
    debugList("Planks", planks.toList())
    debugList("Stairs", stairs.toList())
    debugList("Fences", fences.toList())
    debugList("Signs", signs.toList())
  }


  private fun debugList(name: String, items: List<Material>) {
    val materialNames: MutableList<String> = ArrayList()
    items.forEach { materialNames.add(it.name) }
    BreweryLogger.debug("$name: ${java.lang.String.join(", ", materialNames)}")
  }
}
