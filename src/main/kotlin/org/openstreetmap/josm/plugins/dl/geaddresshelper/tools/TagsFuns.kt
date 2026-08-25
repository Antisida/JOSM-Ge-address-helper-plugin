package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools
// todo переписать логики, чтобы из диалога возвращалась сразу мапа
fun toTags(tagsText: String?): Map<String, String> {
  if (tagsText == null) return mapOf()
  return tagsText
      .trimIndent()
      .lineSequence() // Построчный разбор
      .map { it.trim() }
      .filter { it.isNotEmpty() && it.contains("=") }
      .associate { line ->
        val (key, value) = line.split("=", limit = 2)
        key.trim() to value.trim()
      }
}
