import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import org.jetbrains.kotlin.parsing.parseBoolean

plugins {
  kotlin("jvm") version "2.0.0-Beta5"
  id("maven-publish")
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

group = properties["pluginGroup"]!!
version = properties["pluginVersion"]!!

java {
  sourceCompatibility = JavaVersion.VERSION_16
  targetCompatibility = JavaVersion.VERSION_16
  disableAutoTargetJvm()
}

kotlin {
  jvmToolchain(16)
}

bukkit {
  name = rootProject.name
  version = rootProject.version as String
  main = "io.inkwellmc.breweryk.BreweryK"

  authors = listOf("sub-kek")

  website = "https://github.com/sub-kek/BreweryK/"

  load = BukkitPluginDescription.PluginLoadOrder.STARTUP

  apiVersion = "1.16"

  foliaSupported = true

  commands {
    register("breweryk") {
      aliases = listOf(
        "brewery",
        "brew"
      )
      permission = "breweryk.command"
    }
  }

  permissions {
    // Права игрока
    register("breweryk.player") {
      childrenMap = mapOf(
        "breweryk.createbarrel" to true,
        "breweryk.command" to true
      )
      default = BukkitPluginDescription.Permission.Default.TRUE
    }

    // Права на создание бочек
    register("brewery.createbarrel") {
      childrenMap = mapOf(
        "breweryk.createbarrel.small" to true,
        "breweryk.createbarrel.large" to true
      )
    }
    register("breweryk.createbarrel.large")
    register("breweryk.createbarrel.small")

    // Права на команды
    register("breweryk.command")
  }
}

dependencies {
  if (parseBoolean(properties["useSpigotApi"] as String)) compileOnly("org.spigotmc:spigot-api:${properties["spigotVersion"]}")
  else compileOnly("io.inkwellmc.inkwell:inkwell-api:${properties["inkwellVersion"]}")

  shadow("org.jetbrains.kotlin:kotlin-stdlib:2.0.0-Beta5")

  /*
  * YAML
  * */
  shadow("org.yaml:snakeyaml:2.2")
  shadow("me.carleslc.Simple-YAML:Simple-Yaml:1.8.4") {
    exclude(group = "org.yaml", module = "snakeyaml")
  }

  /*
  * OTHER
  * */
  shadow("com.tcoded:FoliaLib:${properties["foliaLibVersion"]}")
}

tasks.jar {
  enabled = false
}

tasks.build {
  dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
  archiveFileName = "${rootProject.name}-$version.jar"

  configurations = listOf(project.configurations.shadow.get())
  mergeServiceFiles()

  relocate("com.tcoded.folialib", "io.inkwellmc.breweryk.libs.com.tcoded.folialib")
  relocate("kotlin", "io.inkwellmc.breweryk.libs.kotlin")
  relocate("org.yaml.snakeyaml", "io.inkwellmc.breweryk.libs.org.yaml.snakeyaml")
  relocate("org.simpleyaml", "io.inkwellmc.breweryk.libs.org.simpleyaml")
  relocate("org.jetbrains.annotations", "io.inkwellmc.breweryk.libs.org.jetbrains.annotations")
  relocate("org.intellij.lang.annotations", "io.inkwellmc.breweryk.libs.org.intellij.lang.annotations")
  relocate("example", "io.inkwellmc.breweryk.libs.example")
}