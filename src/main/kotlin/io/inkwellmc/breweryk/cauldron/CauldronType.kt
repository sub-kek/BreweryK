package io.inkwellmc.breweryk.cauldron

import org.bukkit.Material

enum class CauldronType {
  WATER,
  LAVA,
  POWDER_SNOW,
  EMPTY,
  UNKNOWN;

  companion object {
    /**
     * Возвращает тип котла исходя из параметра [material]
     *
     * В случае если материал не котел, то вернет [UNKNOWN]
     *
     * @param material Материал для проверки на тип котла
     **/
    fun fromMaterial(material: Material): CauldronType {
      val materialName = material.name
      entries.forEach { if ("${it.name}_CAULDRON" == materialName) return it }
      if (materialName == "CAULDRON") return EMPTY
      return UNKNOWN
    }

    /**
     * Возвращает тип котла исходя из параметра [string]
     *
     * В случае если строка не тип котла, то вернет [UNKNOWN]
     *
     * @param string Строка для проверки на тип котла
     **/
    fun fromString(string: String): CauldronType {
      entries.forEach { if (it.name == string) return it }
      if (string == "CAULDRON") return EMPTY
      return UNKNOWN
    }
  }
}