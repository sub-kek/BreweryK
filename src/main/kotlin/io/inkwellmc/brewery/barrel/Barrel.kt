package io.inkwellmc.brewery.barrel

import io.inkwellmc.brewery.Brewery
import io.inkwellmc.brewery.util.BoundingBox
import io.inkwellmc.brewery.util.BreweryLogger
import io.inkwellmc.brewery.util.BukkitUtil
import io.inkwellmc.brewery.util.LegacyUtil
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.*


class Barrel(val spigot: Block, signOffset: Byte) : InventoryHolder {
  var body: BarrelBody
  private var inventory: Inventory
  private var time = 0f

  init {
    inventory = if (isLarge()) {
      Brewery.instance.server.createInventory(this, 27, Brewery.language.get("barrel.inventory-title"))
    } else {
      Brewery.instance.server.createInventory(this, 9, Brewery.language.get("barrel.inventory-title"))
    }
    body = BarrelBody(this, signOffset)
  }

  constructor(spigot: Block, signOffset: Byte, boundingBox: BoundingBox, items: Map<String, Any>, time: Float) : this(spigot, signOffset, boundingBox, items, time, false)

  constructor(spigot: Block, signOffset: Byte, bounds: BoundingBox, items: Map<String, Any>?, time: Float, async: Boolean) : this(spigot, signOffset) {
    if (isLarge()) {
      this.inventory = Brewery.instance.server.createInventory(this, 27, Brewery.language.get("barrel.inventory-title"))
    } else {
      this.inventory = Brewery.instance.server.createInventory(this, 9, Brewery.language.get("barrel.inventory-title"))
    }
    if (items != null) {
      for (slot in items.keys) {
        if (items[slot] is ItemStack) {
          inventory.setItem(Brewery.instance.parseInt(slot), items[slot] as ItemStack?)
        }
      }
    }
    this.time = time

    body = BarrelBody(this, signOffset, bounds, async)
  }

  fun isLarge(): Boolean {
    return !isSmall()
  }

  fun isSmall(): Boolean {
    return LegacyUtil.isWoodWallSign(spigot.type)
  }

  fun hasBlock(block: Block): Boolean {
    return body.hasBlock(block)
  }

  fun playOpeningSound() {
    val randPitch = (Math.random() * 0.1).toFloat()
    val location: Location = spigot.location
    if (location.world == null) return
    if (isLarge()) {
      location.world!!.playSound(location, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.4f, 0.55f + randPitch)
      location.world!!.playSound(location, Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.4f, 0.45f + randPitch)
    } else {
      location.world!!.playSound(location, Sound.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.5f, 0.8f + randPitch)
    }
  }

  fun playClosingSound() {
    val randPitch = (Math.random() * 0.1).toFloat()
    val location: Location = spigot.location
    if (location.world == null) return
    if (isLarge()) {
      location.world!!.playSound(location, Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.5f + randPitch)
      location.world!!.playSound(location, Sound.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.2f, 0.6f + randPitch)
    } else {
      location.world!!.playSound(location, Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.8f + randPitch)
    }
  }

  companion object {
    var barrels: MutableList<Barrel> = Collections.synchronizedList(ArrayList())

    fun create(sign: Block, player: Player): Boolean {
      val spigot: Block = BarrelBody.getSpigotOfSign(sign)

      var signOffset: Byte = 0
      if (spigot != sign) {
        signOffset = (sign.y - spigot.y).toByte()
      }

      var barrel: Barrel? = getBySpigot(spigot)
      if (barrel == null) {
        barrel = Barrel(spigot, signOffset)
        if (barrel.body.getBrokenBlock(true) == null) {
          if (LegacyUtil.isWoodWallSign(spigot.type)) {
            if (!player.hasPermission("brewery.createbarrel.small")) {
              Brewery.message(player, "barrel-messages.create-small-no-permission")
              return false
            }
          } else {
            if (!player.hasPermission("brewery.createbarrel.large")) {
              Brewery.message(player, "barrel-messages.create-large-no-permission")
              return false
            }
          }
          barrels.add(0, barrel)
          return true
        }
      } else {
        if (barrel.body.signOffset == (0).toByte() && signOffset == (0).toByte()) {
          barrel.body.signOffset = signOffset
          return true
        }
      }
      return false
    }

    fun hasDataInWorld(world: World): Boolean {
      return barrels.stream().anyMatch { barrel -> barrel.spigot.world == world }
    }

    fun onUnload(world: World) {
      barrels.removeIf { barrel: Barrel -> barrel.spigot.world == world }
    }

    fun getBySpigot(sign: Block): Barrel? {
      val spigot: Block = BarrelBody.getSpigotOfSign(sign)

      var signOffset: Byte = 0
      if (spigot != sign) {
        signOffset = (sign.y - spigot.y).toByte()
      }

      for ((i, barrel) in barrels.withIndex()) {
        if (barrel.body.isSignOfBarrel(signOffset)) {
          if (barrel.spigot == spigot) {
            if (barrel.body.signOffset == (0).toByte() && signOffset != (0).toByte())
              barrel.body.signOffset = signOffset
            moveMRU(i)
            return barrel
          }
        }
      }
      return null
    }

    fun getByWood(wood: Block): Barrel? {
      if (LegacyUtil.isWoodPlanks(wood.type) || LegacyUtil.isWoodStairs(wood.type)) {
        for ((i, barrel) in barrels.withIndex()) {
          if (barrel.spigot.world == wood.world && barrel.body.boundingBox!!.contains(wood)) {
            moveMRU(i)
            return barrel
          }
        }
      }
      return null
    }

    fun get(block: Block?): Barrel? {
      if (block == null) return null

      val type = block.type
      return if (LegacyUtil.isWoodFence(type) || LegacyUtil.isWoodWallSign(type)) {
        getBySpigot(block)
      } else {
        getByWood(block)
      }
    }

    private fun moveMRU(index: Int) {
      if (index > 0) {
        barrels[index - 1] = barrels.set(index, barrels[index - 1])
      }
    }

    fun save(config: ConfigurationSection) {
      BukkitUtil.createWorldSections(config)

      if (barrels.isNotEmpty()) {
        var id = 0
        for (barrel in barrels) {
          var prefix: String = barrel.spigot.world.uid.toString() + "." + id

          // block: x/y/z
          config["$prefix.spigot"] = barrel.spigot.getX().toString() + "/" + barrel.spigot.getY() + "/" + barrel.spigot.getZ()

          // save the body data into the section as well
          barrel.body.save(config, prefix)

          var slot = 0
          var item: ItemStack?
          var invConfig: ConfigurationSection? = null
          while (slot < barrel.inventory.getSize()) {
            item = barrel.inventory.getItem(slot)
            if (item != null) {
              if (invConfig == null) {
                if (barrel.time != 0f) {
                  config["$prefix.time"] = barrel.time
                }
                invConfig = config.createSection("$prefix.inv")
              }
              // ItemStacks are configurationSerializeable, makes them
              // really easy to save
              invConfig[slot.toString() + ""] = item
            }

            slot++
          }

          id++
        }
      }
    }
  }

  override fun getInventory(): Inventory {
    return inventory
  }

  fun openInventory(player: Player) {
    if (time > 0) {
      if (inventory.viewers.isEmpty() && inventory.contains(Material.POTION)) {
        var loadTime = System.nanoTime()
        loadTime = System.nanoTime() - loadTime
        val ftime = (loadTime / 1000000.0).toFloat()
        BreweryLogger.debug("opening Barrel with potions (" + ftime + "ms)")
      }
    }
    time = 0f

    player.openInventory(inventory)
  }

  fun remove(broken: Block?, breaker: Player?, dropItems: Boolean) {
    val viewers: List<HumanEntity> = ArrayList(inventory.viewers)
    // Copy List to fix ConcModExc
    for (viewer in viewers) {
      viewer.closeInventory()
    }
    val items = inventory.contents
    inventory.clear()
    val wood: Byte = body.getWoodType()
    for (item in items) {
      if (item != null) {
        broken?.world?.dropItem(broken.location, item) ?: spigot.world.dropItem(spigot.location, item)
      }
    }

    barrels.remove(this)
  }
}