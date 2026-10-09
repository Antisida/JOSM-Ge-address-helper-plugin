package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.PLACE_SET
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Place

object PlaceParser {

    fun parse(sourceString: String?): Place? {
        if (sourceString == null) return null
        val trimmed = sourceString.trim()
        val status = PLACE_SET.filter { trimmed.contains(it) }.takeIf { it.size == 1 }?.get(0) ?: "-"
        val name = getPlaceName(trimmed, PLACE_SET)
        return Place(
            sourceString,
            status,
            name,
            mutableListOf(),
            true
        )
    }

    private fun getPlaceName(string: String, statuses: Set<String>): String {
        // 1. Экранируем спецсимволы и объединяем слова через |
        val pattern = statuses.joinToString("|") { Regex.escape(it) }

        // 2. Строим регулярное выражение с проверкой границ (пробел, начало или конец строки)
        // (?<=^|\s) — слева должен быть старт строки или пробел
        // (?=$|\s) — справа должен быть конец строки или пробел
        val regex = "(?U)(?<=^|\\s)($pattern)(?=$|\\s)".toRegex()

        // 3. Удаляем слова и очищаем лишние двойные пробелы, которые могли остаться
        return string.replace(regex, "").replace("\\s+".toRegex(), " ").trim()
    }
}