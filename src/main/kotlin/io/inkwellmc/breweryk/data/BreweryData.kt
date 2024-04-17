package io.inkwellmc.breweryk.data

import com.tcoded.folialib.wrapper.task.WrappedTask
import io.inkwellmc.breweryk.BreweryK
import io.inkwellmc.breweryk.barrel.Barrel
import io.inkwellmc.breweryk.cauldron.BreweryCauldron
import io.inkwellmc.breweryk.config.BreweryConfig
import io.inkwellmc.breweryk.util.BoundingBox
import io.inkwellmc.breweryk.util.BreweryLogger
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

object BreweryData {
  var dataMutex: AtomicInteger = AtomicInteger(0)
  var worldData: FileConfiguration? = null

  init {
    // Запуск авто сохранения каждые 1200 тиков( 60 секунд )
    BreweryK.instance.foliaLib.impl.runTimer(Consumer {
      DataSave.autoSave()
    }, 1200, 1200)
  }

  // Загрузка данных
  fun readData() {
    val file = File(BreweryK.instance.dataFolder, "data.yml")
    if (file.exists()) {
      val t1 = System.currentTimeMillis()

      val data: FileConfiguration = YamlConfiguration.loadConfiguration(file)

      var section = data.getConfigurationSection("players")
      if (section != null) {
        for (uuid in section.getKeys(false)) {
          try {
            UUID.fromString(uuid)
          } catch (e: IllegalArgumentException) {
            continue
          }

          val quality: Int = section.getInt("$uuid.quality")
          val drunk: Int = section.getInt("$uuid.drunk")
          val offDrunk: Int = section.getInt("$uuid.offDrunk", 0)
        }
      }

      val worlds: List<World> = BreweryK.instance.server.worlds
      if (BreweryConfig.loadDataAsync) {
        BreweryK.instance.foliaLib.impl.runLaterAsync(Consumer { lwDataTask(worlds) }, 1)
      } else {
        lwDataTask(worlds)
      }
    } else {
      BreweryLogger.info("No data.yml found, will create new one!")
    }
  }

  fun lwDataTask(worlds: List<World>) {
    if (!acquireDataLoadMutex()) return

    try {
      for (world in worlds) {
        loadWorldData(world.uid.toString(), world)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      releaseDataLoadMutex()
      if (BreweryConfig.loadDataAsync && dataMutex.get() == 0) {
        BreweryLogger.info("Background data loading complete.")
      }
    }
  }

  // load Block locations of given world
  // can be run async
  fun loadWorldData(uuid: String, world: World) {
    if (worldData == null) {
      val file = File(BreweryK.instance.dataFolder, "world-data.yml")
      if (file.exists()) {
        val t1 = System.currentTimeMillis()
        worldData = YamlConfiguration.loadConfiguration(file)
        val t2 = System.currentTimeMillis()
        if (t2 - t1 > 15000) {
          BreweryLogger.info("Bukkit took " + (t2 - t1) / 1000.0 + "s to load Inventories from the World-Data File (in the Background),")
          BreweryLogger.info("consider switching to Paper, or have less items in Barrels if it takes a long time for Barrels to become available")
        }
      } else {
        return
      }
    }

    if (worldData!!.contains("cauldrons.$uuid")) {
      val section = worldData!!.getConfigurationSection("cauldrons.$uuid")!!
      for (cauldron in section.getKeys(false)) {
        val cauldronBlockLocation = section.getString("block")
        if (cauldronBlockLocation != null) {
          val splitLocation = cauldronBlockLocation.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
          if (splitLocation.size == 3) {
            val block = world.getBlockAt(BreweryK.instance.parseInt(splitLocation[0]), BreweryK.instance.parseInt(splitLocation[1]), BreweryK.instance.parseInt(splitLocation[2]))

            BreweryK.instance.foliaLib.impl.runAtLocationLater(block.location, Consumer {
              BreweryCauldron.getByBlock(block)
            }, 1)
          }
        }
      }
    }

    // loading Barrel
    if (worldData!!.contains("barrels.$uuid")) {
      val section = worldData!!.getConfigurationSection("barrels.$uuid")!!
      for (barrel in section.getKeys(false)) {
        // block spigot is splitted into x/y/z
        val spigot = section.getString("$barrel.spigot")
        if (spigot != null) {
          val splitted = spigot.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
          if (splitted.size == 3) {
            // load itemStacks from invSection

            val invSection = section.getConfigurationSection("$barrel.inv")
            val block = world.getBlockAt(BreweryK.instance.parseInt(splitted[0]), BreweryK.instance.parseInt(splitted[1]), BreweryK.instance.parseInt(splitted[2]))
            val time = section.getDouble("$barrel.time", 0.0).toFloat()
            val sign = section.getInt("$barrel.sign", 0).toByte()

            var box: BoundingBox? = null
            if (section.contains("$barrel.bounds")) {
              val bds = section.getString("$barrel.bounds", "")!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
              if (bds.size == 6) {
                box = BoundingBox(BreweryK.instance.parseInt(bds[0]), BreweryK.instance.parseInt(bds[1]), BreweryK.instance.parseInt(bds[2]), BreweryK.instance.parseInt(bds[3]), BreweryK.instance.parseInt(bds[4]), BreweryK.instance.parseInt(bds[5]))
              }
            }

            val bbox: BoundingBox? = box
            BreweryK.instance.foliaLib.impl.runAtLocationLater(block.location, Consumer {
              val b = if (invSection != null) {
                Barrel(block, sign, bbox!!, invSection.getValues(true), time, true)
              } else {
                Barrel(block, sign, bbox!!, null, time, true)
              }
              if (b.body.boundingBox != null) {
                Barrel.barrels.add(b)
              } else {
                // The Barrel Bounds need recreating, as they were missing or corrupt
                if (b.body.regenerateBounds()) {
                  Barrel.barrels.add(b)
                }
              }
            }, 1)
          } else {
            BreweryLogger.info("Incomplete Block-Data in data.yml: " + section.currentPath + "." + barrel)
          }
        } else {
          BreweryLogger.info("Missing Block-Data in data.yml: " + section.currentPath + "." + barrel)
        }
      }
    }
  }

  fun acquireDataLoadMutex(): Boolean {
    var wait = 0
    while (dataMutex.updateAndGet { i: Int -> if (i >= 0) i + 1 else i } <= 0) {
      wait++
      if (!BreweryConfig.loadDataAsync || wait > 60) {
        BreweryLogger.info("Could not load World Data, Mutex: " + dataMutex.get())
        return false
      }
      try {
        Thread.sleep(1000)
      } catch (e: InterruptedException) {
        return false
      }
    }
    return true
  }

  fun releaseDataLoadMutex() {
    dataMutex.decrementAndGet()
  }
}

class ReadOldData(private val followUpConsumer: Consumer<Any?>) : Consumer<WrappedTask?> {
  var data: FileConfiguration? = null
    private set

  override fun accept(task: WrappedTask?) {
    var wait = 0
    while (!BreweryData.dataMutex.compareAndSet(0, -1)) {
      if (wait > 300) {
        BreweryLogger.info("Loading Process active for too long while trying to save! Mutex: ${BreweryData.dataMutex.get()}")
        return
      }
      wait++
      try {
        Thread.sleep(100)
      } catch (e: InterruptedException) {
        return
      }
    }


    val worldDataFile = File(BreweryK.instance.dataFolder, "world-data.yml")
    if (BreweryData.worldData == null) {
      if (!worldDataFile.exists()) {
        data = YamlConfiguration()
        followUpConsumer.accept(null)
        return
      }

      data = YamlConfiguration.loadConfiguration(worldDataFile)
    } else {
      data = BreweryData.worldData
    }

    if (DataSave.lastBackup > 10) {
      worldDataFile.renameTo(File(BreweryK.instance.dataFolder, "world-data-backup.yml"))
      DataSave.lastBackup = 0
    } else {
      DataSave.lastBackup++
    }

    followUpConsumer.accept(null)
  }
}

class DataSave : Consumer<Any?> {
  private val loadedWorlds: List<World> = BreweryK.instance.server.worlds
  var collected: Boolean = false

  override fun accept(ignored: Any?) {
    try {
      val saveTime = System.nanoTime()
      BreweryData.worldData = null

      val data: FileConfiguration = YamlConfiguration()
      val worldData: FileConfiguration = YamlConfiguration()

      saveWorldNames(worldData)

      if (Barrel.barrels.isNotEmpty()) {
        Barrel.save(worldData.createSection("barrels"))
      }

      if (BreweryCauldron.cauldrons.isNotEmpty()) {
        BreweryCauldron.save(worldData.createSection("cauldrons"))
      }

      collected = true

      if (unloadingWorlds.isNotEmpty()) {
        try {
          for (world in unloadingWorlds) {
            Barrel.onUnload(world)
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
        unloadingWorlds.clear()
      }

      BreweryLogger.debug("saving: ${(System.nanoTime() - saveTime) / 1000000.0}ms")

      if (BreweryK.instance.isEnabled) BreweryK.instance.foliaLib.impl.runLaterAsync(DataWrite(data, worldData), 1)
    } catch (e: Exception) {
      e.printStackTrace()
      BreweryData.dataMutex.set(0)
    }
  }

  fun saveWorldNames(root: FileConfiguration) {
    for (world in loadedWorlds) {
      root["worlds.${world.uid}"] = world.name
    }
  }

  companion object {
    var lastBackup: Int = 0
    var lastSave: Int = 1
    private const val AUTOSAVE: Int = 1
    var unloadingWorlds: MutableList<World> = CopyOnWriteArrayList()

    fun save(collectInstant: Boolean) {
      val read = ReadOldData(DataSave())
      if (collectInstant) read.accept(null) else BreweryK.instance.foliaLib.impl.runLaterAsync(read, 1)
    }

    fun autoSave() {
      if (lastSave >= AUTOSAVE) {
        save(false) // save all data
      } else {
        lastSave++
      }
    }
  }
}


class DataWrite(private val data: FileConfiguration, private val worldData: FileConfiguration) : Consumer<WrappedTask> {
  override fun accept(task: WrappedTask) {
    val dataFile = File(BreweryK.instance.dataFolder, "data.yml")
    val worldDataFile = File(BreweryK.instance.dataFolder, "world-data.yml")

    try {
      data.save(dataFile)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    try {
      worldData.save(worldDataFile)
    } catch (e: Exception) {
      e.printStackTrace()
    }

    DataSave.lastSave = 1
    BreweryData.dataMutex.set(0)
  }
}