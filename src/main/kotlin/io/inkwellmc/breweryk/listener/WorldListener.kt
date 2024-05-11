package io.inkwellmc.breweryk.listener

import io.inkwellmc.breweryk.BreweryK
import io.inkwellmc.breweryk.barrel.Barrel
import io.inkwellmc.breweryk.config.BreweryConfig
import io.inkwellmc.breweryk.data.BreweryData
import io.inkwellmc.breweryk.data.DataSave
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.util.function.Consumer

class WorldListener : Listener {
  @EventHandler
  fun onWorldLoad(event: WorldLoadEvent) {
    val world = event.world
    if (BreweryConfig.loadDataAsync) {
      BreweryK.instance.foliaLib.impl.runLaterAsync(Consumer { lwDataTask(world) }, 1)
    } else {
      lwDataTask(world)
    }
  }

  private fun lwDataTask(world: World) {
    if (!BreweryData.acquireDataLoadMutex()) return

    try {
      BreweryData.loadWorldData(world.uid.toString(), world)
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      BreweryData.releaseDataLoadMutex()
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onWorldUnload(event: WorldUnloadEvent) {
    val world = event.world
    if (Barrel.hasDataInWorld(world)) {
      DataSave.unloadingWorlds.add(world)
      DataSave.save(false)
    }
  }
}
