package io.inkwellmc.breweryk.util

import org.bukkit.Location
import org.bukkit.block.Block
import kotlin.math.max
import kotlin.math.min

class BoundingBox(
  private val minX: Int,
  private val minY: Int,
  private val minZ: Int,
  private val maxX: Int,
  private val maxY: Int,
  private val maxZ: Int
) {

  fun contains(x: Int, y: Int, z: Int): Boolean {
    return (x in minX..maxX) && (y in minY..maxY) && (z in minZ..maxZ)
  }

  fun contains(loc: Location): Boolean {
    return contains(loc.blockX, loc.blockY, loc.blockZ)
  }

  fun contains(block: Block): Boolean {
    return contains(block.x, block.y, block.z)
  }

  fun area(): Long {
    return ((maxX - minX + 1).toLong()) * ((maxY - minY + 1).toLong()) * ((maxZ - minZ + 1).toLong())
  }

  fun serialize(): String {
    return "$minX,$minY,$minZ,$maxX,$maxY,$maxZ"
  }

  companion object {
    fun fromPoints(locations: IntArray): BoundingBox {
      require(locations.size % 3 == 0) { "Locations has to be pairs of three" }

      val length = locations.size - 2

      var minX = Int.MAX_VALUE
      var minY = Int.MAX_VALUE
      var minZ = Int.MAX_VALUE
      var maxX = Int.MIN_VALUE
      var maxY = Int.MIN_VALUE
      var maxZ = Int.MIN_VALUE
      var i = 0
      while (i < length) {
        minX = min(locations[i].toDouble(), minX.toDouble()).toInt()
        minY = min(locations[i + 1].toDouble(), minY.toDouble()).toInt()
        minZ = min(locations[i + 2].toDouble(), minZ.toDouble()).toInt()
        maxX = max(locations[i].toDouble(), maxX.toDouble()).toInt()
        maxY = max(locations[i + 1].toDouble(), maxY.toDouble()).toInt()
        maxZ = max(locations[i + 2].toDouble(), maxZ.toDouble()).toInt()
        i += 3
      }
      return BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
    }
  }
}