package io.inkwellmc.brewery.util

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.util.Vector
import kotlin.math.max
import kotlin.math.min

class BoundingBox(private val x1: Int, private val y1: Int, private val z1: Int, private val x2: Int, private val y2: Int, private val z2: Int) {

  fun contains(x: Int, y: Int, z: Int): Boolean {
    return (x >= x1 && x <= x2) && (y >= y1 && y <= y2) && (z >= z1 && z <= z2)
  }

  fun contains(loc: Location): Boolean {
    return contains(loc.blockX, loc.blockY, loc.blockZ)
  }

  fun contains(block: Block): Boolean {
    return contains(block.x, block.y, block.z)
  }

  fun area(): Long {
    return ((x2 - x1 + 1).toLong()) * ((y2 - y1 + 1).toLong()) * ((z2 - z1 + 1).toLong())
  }

  fun serialize(): String {
    return "$x1,$y1,$z1,$x2,$y2,$z2"
  }

  fun getWidthX(): Int {
    return this.x2 - this.x1
  }

  fun getWidthZ(): Int {
    return this.z2 - this.z1
  }

  fun getHeight(): Int {
    return this.y2 - this.y1
  }

  fun getCenterX(): Double {
    return this.x1 + this.getWidthX() * 0.5
  }

  fun getCenterY(): Double {
    return this.y1 + this.getHeight() * 0.5
  }

  fun getCenterZ(): Double {
    return this.z2 + this.getWidthZ() * 0.5
  }

  fun getCenter(): Vector {
    return Vector(this.getCenterX(), this.getCenterY(), this.getCenterZ())
  }

  companion object {
    fun fromPoints(locations: IntArray): BoundingBox {
      require(locations.size % 3 == 0) { "Locations has to be pairs of three" }

      val length = locations.size - 2

      var minx = Int.MAX_VALUE
      var miny = Int.MAX_VALUE
      var minz = Int.MAX_VALUE
      var maxx = Int.MIN_VALUE
      var maxy = Int.MIN_VALUE
      var maxz = Int.MIN_VALUE
      var i = 0
      while (i < length) {
        minx = min(locations[i].toDouble(), minx.toDouble()).toInt()
        miny = min(locations[i + 1].toDouble(), miny.toDouble()).toInt()
        minz = min(locations[i + 2].toDouble(), minz.toDouble()).toInt()
        maxx = max(locations[i].toDouble(), maxx.toDouble()).toInt()
        maxy = max(locations[i + 1].toDouble(), maxy.toDouble()).toInt()
        maxz = max(locations[i + 2].toDouble(), maxz.toDouble()).toInt()
        i += 3
      }
      return BoundingBox(minx, miny, minz, maxx, maxy, maxz)
    }
  }
}