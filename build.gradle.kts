import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  kotlin("jvm") version "2.0.0-Beta5"
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
  main = "io.inkwellmc.brewery.Brewery"

  authors = listOf("sub-kek")

  website = "https://github.com/sub-kek/BreweryK/"

  load = BukkitPluginDescription.PluginLoadOrder.STARTUP

  apiVersion = "1.16"

  foliaSupported = true

  permissions {
    register("brewery.player") {
      childrenMap = mapOf(
        "brewery.createbarrel" to true
      )
      default = BukkitPluginDescription.Permission.Default.TRUE
    }
    register("brewery.createbarrel") {
      childrenMap = mapOf(
        "brewery.createbarrel.small" to true,
        "brewery.createbarrel.large" to true
      )
    }
    register("brewery.createbarrel.large")
    register("brewery.createbarrel.small")
  }
}

dependencies {
  compileOnly("org.spigotmc:spigot-api:${properties["spigotVersion"]}")
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

  relocate("com.tcoded.folialib", "io.inkwellmc.brewery.libs.com.tcoded.folialib")
  relocate("kotlin", "io.inkwellmc.brewery.libs.kotlin")
  relocate("org.yaml.snakeyaml", "io.inkwellmc.brewery.libs.org.yaml.snakeyaml")
  relocate("org.simpleyaml", "io.inkwellmc.brewery.libs.org.simpleyaml")
  relocate("org.jetbrains.annotations", "io.inkwellmc.brewery.libs.org.jetbrains.annotations")
  relocate("org.intellij.lang.annotations", "io.inkwellmc.brewery.libs.org.intellij.lang.annotations")
  relocate("example", "io.inkwellmc.brewery.libs.example")
}