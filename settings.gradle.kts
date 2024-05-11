dependencyResolutionManagement {
  repositories {
    maven("https://repo.bambooland.fun/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://nexuslite.gcnt.net/repos/other/")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

rootProject.name = "BreweryK"