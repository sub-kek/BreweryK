package io.inkwellmc.brewery.config

enum class Language(val label: String) {
  RUSSIAN("ru_RU"),
  ENGLISH("ru_RU");

  companion object {
    val allSeparatedComma: String
      get() {
        val labels = ArrayList<String>()
        for (language in entries) {
          labels.add(language.label)
        }
        return java.lang.String.join(", ", labels)
      }
  }
}
