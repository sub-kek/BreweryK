package io.inkwellmc.breweryk.cauldron

enum class CauldronHeatSource {
  FIRE,
  SOUL_FIRE,
  CAMPFIRE(true),
  SOUL_CAMFIRE(true),
  LAVA,
  MAGMA_BLOCK;

  var needLitCheck = false
    private set

  constructor()
  constructor(needLitCheck: Boolean) {
    this.needLitCheck = needLitCheck
  }
}