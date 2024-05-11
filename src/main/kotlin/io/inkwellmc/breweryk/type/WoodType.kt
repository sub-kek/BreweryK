package io.inkwellmc.breweryk.type

enum class WoodType(val id: Byte) {
  BIRCH(1),
  OAK(2),
  JUNGLE(3),
  SPRUCE(4),
  ACACIA(5),
  DARK_OAK(6),
  CRIMSON(7),
  WARPED(8),
  MANGROVE(9),
  CHERRY(10),
  BAMBOO(11);

  companion object {
    fun getHighest(): WoodType {
      var highest = BIRCH
      WoodType.entries.forEach { if (highest.id < it.id) highest = it }
      return highest
    }
  }
}