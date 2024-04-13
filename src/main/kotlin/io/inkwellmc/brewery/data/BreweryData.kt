package io.inkwellmc.brewery.data

import com.tcoded.folialib.wrapper.task.WrappedTask
import io.inkwellmc.brewery.Brewery
import io.inkwellmc.brewery.barrel.Barrel
import io.inkwellmc.brewery.config.BreweryConfig
import io.inkwellmc.brewery.util.BoundingBox
import io.inkwellmc.brewery.util.BreweryLogger
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
    Brewery.instance.foliaLib.impl.runTimer(Consumer {
      DataSave.autoSave()
    }, 1200, 1200)
  }

  // Загрузка данных
  fun readData() {
    val file = File(Brewery.instance.dataFolder, "data.yml")
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

      val worlds: List<World> = Brewery.instance.server.worlds
      if (BreweryConfig.loadDataAsync) {
        Brewery.instance.foliaLib.impl.runLaterAsync(Consumer { lwDataTask(worlds) }, 1)
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
      val file = File(Brewery.instance.dataFolder, "world-data.yml")
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
            val block = world.getBlockAt(Brewery.instance.parseInt(splitted[0]), Brewery.instance.parseInt(splitted[1]), Brewery.instance.parseInt(splitted[2]))
            val time = section.getDouble("$barrel.time", 0.0).toFloat()
            val sign = section.getInt("$barrel.sign", 0).toByte()

            var box: BoundingBox? = null
            if (section.contains("$barrel.bounds")) {
              val bds = section.getString("$barrel.bounds", "")!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
              if (bds.size == 6) {
                box = BoundingBox(Brewery.instance.parseInt(bds[0]), Brewery.instance.parseInt(bds[1]), Brewery.instance.parseInt(bds[2]), Brewery.instance.parseInt(bds[3]), Brewery.instance.parseInt(bds[4]), Brewery.instance.parseInt(bds[5]))
              }
            } else if (section.contains("$barrel.st")) {
              // Convert from Stair and Wood Locations to BoundingBox
              val st = section.getString("$barrel.st", "")!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
              val wo = section.getString("$barrel.wo", "")!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
              var woLength = wo.size
              if (woLength <= 1) {
                woLength = 0
              }
              val points = arrayOfNulls<String>(st.size + woLength)
              System.arraycopy(st, 0, points, 0, st.size)
              if (woLength > 1) {
                System.arraycopy(wo, 0, points, st.size, woLength)
              }
              val locs = Arrays.stream<String?>(points).mapToInt { s: String? -> Brewery.instance.parseInt(s) }.toArray()
              try {
                box = BoundingBox.fromPoints(locs)
              } catch (e: Exception) {
                e.printStackTrace()
              }
            }

            val bbox: BoundingBox? = box
            Brewery.instance.foliaLib.impl.runAtLocationLater(block.location, Consumer {
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


    val worldDataFile = File(Brewery.instance.dataFolder, "world-data.yml")
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
      worldDataFile.renameTo(File(Brewery.instance.dataFolder, "world-data-backup.yml"))
      DataSave.lastBackup = 0
    } else {
      DataSave.lastBackup++
    }

    followUpConsumer.accept(null)
  }
}

class DataSave : Consumer<Any?> {
  private val loadedWorlds: List<World> = Brewery.instance.server.worlds
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

      if (Brewery.instance.isEnabled) Brewery.instance.foliaLib.impl.runLaterAsync(DataWrite(data, worldData), 1)
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
      if (collectInstant) read.accept(null) else Brewery.instance.foliaLib.impl.runLaterAsync(read, 1)
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
    val dataFile = File(Brewery.instance.dataFolder, "data.yml")
    val worldDataFile = File(Brewery.instance.dataFolder, "world-data.yml")

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