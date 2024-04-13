package io.inkwellmc.brewery.barrel

import io.inkwellmc.brewery.util.BoundingBox
import io.inkwellmc.brewery.util.BreweryLogger
import io.inkwellmc.brewery.util.BukkitUtil
import io.inkwellmc.brewery.util.LegacyUtil
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import kotlin.math.abs

class BarrelBody(var barrel: Barrel, var signOffset: Byte) {
  val spigot = barrel.spigot
  var boundingBox: BoundingBox?
    private set

  init {
    boundingBox = BoundingBox(0, 0, 0, 0, 0, 0)
  }

  constructor(barrel: Barrel, signOffset: Byte, boundingBoxFile: BoundingBox, async: Boolean) : this(barrel, signOffset) {
    if (boundsSeemBad(boundingBoxFile)) {
      if (async) {
        this.boundingBox = null
        return
      }
      // If loading from old data, or block locations are missing, or other error, regenerate BoundingBox
      // This will only be done in those extreme cases.
      regenerateBounds()
    } else {
      this.boundingBox = boundingBoxFile
    }
  }

  fun isSignOfBarrel(signOffset: Byte): Boolean {
    return signOffset == (0).toByte() || this.signOffset == (0).toByte() || this.signOffset == signOffset
  }

  fun getBrokenBlock(force: Boolean): Block? {
    if (force || BukkitUtil.isChunkLoaded(spigot)) {
      return if (LegacyUtil.isWoodWallSign(spigot.type)) {
        checkSBarrel()
      } else {
        checkLBarrel()
      }
    }
    return null
  }

  fun regenerateBounds(): Boolean {
    BreweryLogger.info("Regenerating Barrel BoundingBox: " + (if (boundingBox == null) "was null" else "area=" + boundingBox!!.area()))
    val broken = getBrokenBlock(true)
    if (broken != null) {
      barrel.remove(broken, null, true)
      return false
    }
    return true
  }

  fun getWoodType(): Byte {
    val wood = when (getDirection(spigot)) {
      0 -> return 0
      1 -> spigot.getRelative(1, 0, 0)
      2 -> spigot.getRelative(-1, 0, 0)
      3 -> spigot.getRelative(0, 0, 1)
      else -> spigot.getRelative(0, 0, -1)
    }
    return LegacyUtil.getWoodType(wood.type)!!.id
  }

  fun destroySign() {
    signOffset = 0
  }

  fun isLarge(): Boolean {
    return barrel.isLarge()
  }

  fun isSmall(): Boolean {
    return barrel.isSmall()
  }

  fun getSignOfSpigot(): Block {
    if (signOffset.toInt() != 0) {
      if (LegacyUtil.isWoodWallSign(spigot.type)) {
        return spigot
      }

      if (LegacyUtil.isWoodWallSign(spigot.getRelative(0, signOffset.toInt(), 0).type)) {
        return spigot.getRelative(0, signOffset.toInt(), 0)
      } else {
        signOffset = 0
      }
    }
    return spigot
  }

  fun hasBlock(block: Block?): Boolean {
    if (block != null) {
      if (spigot == block) {
        return true
      }
      if (spigot.world == block.world) {
        return boundingBox != null && boundingBox!!.contains(block.x, block.y, block.z)
      }
    }
    return false
  }

  fun checkSBarrel(): Block? {
    val direction = getDirection(spigot) // 1=x+ 2=x- 3=z+ 4=z-
    if (direction == 0) {
      return spigot
    }
    val startX: Int
    val startZ: Int

    when (direction) {
      1 -> {
        startX = 1
        startZ = -1
      }

      2 -> {
        startX = -2
        startZ = 0
      }

      3 -> {
        startX = 0
        startZ = 1
      }

      else -> {
        startX = -1
        startZ = -2
      }
    }
    val endX = startX + 1
    val endZ = startZ + 1

    var type: Material
    var x = startX
    var y = 0
    var z = startZ
    while (y <= 1) {
      while (x <= endX) {
        while (z <= endZ) {
          val block = spigot.getRelative(x, y, z)
          type = block.type

          if (y == 0 && LegacyUtil.areStairsInverted(block) && LegacyUtil.isWoodStairs(type)) {
            z++
          } else if (y == 1 && !LegacyUtil.areStairsInverted(block)) {
            z++
          } else {
            return block
          }
        }
        z = startZ
        x++
      }
      z = startZ
      x = startX
      y++
    }
    boundingBox = BoundingBox(
      spigot.x + startX,
      spigot.y,
      spigot.z + startZ,
      spigot.x + endX,
      spigot.y + 1,
      spigot.z + endZ)
    return null
  }

  fun checkLBarrel(): Block? {
    val direction: Int = getDirection(spigot) // 1=x+ 2=x- 3=z+ 4=z-
    BreweryLogger.debug("$direction")
    if (direction == 0) {
      return spigot
    }
    val startX: Int
    val startZ: Int
    val endX: Int
    val endZ: Int

    when (direction) {
      1 -> {
        startX = 1
        startZ = -1
      }

      2 -> {
        startX = -4
        startZ = -1
      }

      3 -> {
        startX = -1
        startZ = 1
      }

      else -> {
        startX = -1
        startZ = -4
      }
    }

    if (direction == 1 || direction == 2) {
      endX = startX + 3
      endZ = startZ + 2
    } else {
      endX = startX + 2
      endZ = startZ + 3
    }

    var type: Material
    var x = startX
    var y = 0
    var z = startZ
    while (y <= 2) {
      while (x <= endX) {
        while (z <= endZ) {
          val block = spigot.getRelative(x, y, z)
          type = block.type

          var asLeftRight: Int
          var asForward: Int

          if (direction == 1 || direction == 2) {
            asLeftRight = z
            asForward = abs(x)
          } else {
            asLeftRight = x
            asForward = abs(z)
          }

          // Проверка что блоки на концах крестовины бочек являются досками
          if (((y == 0 && asLeftRight == 0) || (y == 1 && (asLeftRight == 1 || asLeftRight == -1)) || (y == 2 && asLeftRight == 0)) && LegacyUtil.isWoodPlanks(type)) {
            z++
          } // Проверка что блоки в центре бочки являются досками
          else if ((y == 1 && (z == 0 || asForward == 1 || asForward == 4)) && LegacyUtil.isWoodPlanks(type)) {
            z++
          } // Пропуск проверки если блоки внутри бочки
          else if (y == 1 && (asForward == 2 || asForward == 3) && asLeftRight == 0) {
            z++
          } // Проверка что блоки в нижних углах бочки являются перевернутыми ступенями
          else if ((y == 0 && (asLeftRight == 1 || asLeftRight == -1)) && LegacyUtil.areStairsInverted(block)) {
            z++
          } // Проверка что блоки в верхних углах бочки являются перевернутыми ступенями
          else if ((y == 2 && (asLeftRight == 1 || asLeftRight == -1)) && !LegacyUtil.areStairsInverted(block) && LegacyUtil.isWoodStairs(type)) {
            z++
          } else {
            return block
          }
        }
        z = startZ
        x++
      }
      z = startZ
      x = startX
      y++
    }
    boundingBox = BoundingBox(
      spigot.x + startX,
      spigot.y,
      spigot.z + startZ,
      spigot.x + endX,
      spigot.y + 2,
      spigot.z + endZ)

    return null
  }

  fun save(config: ConfigurationSection, prefix: String) {
    if (signOffset.toInt() != 0) {
      config["$prefix.sign"] = signOffset
    }
    config["$prefix.bounds"] = boundingBox!!.serialize()
  }

  companion object {
    fun getSpigotOfSign(sign: Block): Block {
      for (y in -2..1) {
        val relative: Block = sign.getRelative(0, y, 0)
        if (LegacyUtil.isWoodFence(relative.type)) return relative
      }
      return sign
    }

    fun getDirection(spigot: Block): Int {
      var direction = 0 // 1=x+ 2=x- 3=z+ 4=z-
      var type = spigot.getRelative(0, 0, 1).type
      if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
        direction = 3
      }
      type = spigot.getRelative(0, 0, -1).type
      if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
        if (direction == 0) {
          direction = 4
        } else {
          return 0
        }
      }
      type = spigot.getRelative(1, 0, 0).type
      if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
        if (direction == 0) {
          direction = 1
        } else {
          return 0
        }
      }
      type = spigot.getRelative(-1, 0, 0).type
      if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)) {
        if (direction == 0) {
          direction = 2
        } else {
          return 0
        }
      }
      return direction
    }

    fun boundsSeemBad(bounds: BoundingBox): Boolean {
      val area = bounds.area()
      return area > 64 || area < 4
    }
  }
}