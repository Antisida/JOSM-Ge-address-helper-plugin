package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.PLACE_SET
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.HouseNumber
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Street
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.STREET_STATUS_AND_ABBR_SET
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Place
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.SplitDto
import org.openstreetmap.josm.tools.Logging

object AddressParser {

    fun parse(sourceString: String): Address {
        val (placeString, streetString, houseNumberString) = splitAddressNew(
            sourceString
                .removeParenthesesContent()
                .insertMissingComma()
                .removeCommasBetweenStatuses(STREET_STATUS_AND_ABBR_SET.toList())
        )

        val place: Place = PlaceParser.parse(placeString) ?: Place("", "", mutableListOf(), false)
        val street: Street = StreetParser.parse(streetString) ?: Street("", "", mutableListOf(), false)
        val houseNumber: HouseNumber = HouseNumberParser.parse(houseNumberString) ?: HouseNumber("", "", listOf(), false)
        Logging.info("sourceString $sourceString, street = ${street.extractedName}, hn = ${houseNumber.extractedNumber}")
        return Address(
            sourceString,
            place,
            street,
            houseNumber,
            mutableListOf(),
            street.isSuccess && houseNumber.isSuccess
        )
    }

    /**
     * Удаляет из строки круглые скобки и всё, что находится внутри них.
     * Также очищает лишние двойные пробелы, которые могли образоваться после удаления.
     */
    private fun String.removeParenthesesContent(): String {
        // Регулярное выражение находит ( и ) и всё между ними (жадный поиск исключая закрывающую скобку)
        val regex = """(?U)\([^)]*\)""".toRegex()

        return this.replace(regex, "")
            .replace("""(?U)\s+""".toRegex(), " ") // Схлопываем разбежавшиеся пробелы в один
            .trim()                            // Убираем пробелы по краям, если скобки были в начале/конце
    }

    /** Ищет букву N, если перед ней (через любые пробелы) НЕТ запятой и вставляет запятую. */
    private fun String.insertMissingComma(): String {
        return this.replace(Regex("([^,\\s])\\s*N"), "$1, N")
    }

    private fun String.removeCommasBetweenStatuses(statuses: List<String>): String {
        if (statuses.isEmpty()) return this

        // Экранируем спецсимволы и собираем регулярное выражение для поиска слов
        val escapedWords = statuses.map { Regex.escape(it) }.joinToString("|")
        val wordRegex = """(?U)\b(?:$escapedWords)\b""".toRegex()

        // Находим все совпадения слов из списка в нашей строке
        val matches = wordRegex.findAll(this).toList()

        // Если найдено меньше двух слов, то запятых "между ними" быть не может
        if (matches.size < 2) return this

        // Границы, внутри которых мы будем удалять запятые
        val firstWordEnd = matches.first().range.last + 1
        val lastWordStart = matches.last().range.first

        val result = StringBuilder()
        for (i in this.indices) {
            val char = this[i]
            // Если это запятая и она лежит внутри границ между ключевыми словами — пропускаем её
            if (char == ',' && i >= firstWordEnd && i < lastWordStart) {
                continue
            }
            result.append(char)
        }
        return result.toString()
    }

    private fun splitAddressNew(string: String): SplitDto {
        val list: List<String> = string.split(",")
        val place: String? = list.filter { line -> PLACE_SET.any { status -> status in line } }.takeIf { it.size == 1 }?.get(0)
        val street: String? = list.filter { line -> STREET_STATUS_AND_ABBR_SET.any { status -> status in line } }.takeIf { it.size == 1 }?.get(0)
        val houseNumber: String? = list.filter { line -> "N" in line }.takeIf { it.isNotEmpty() }?.lastOrNull()
        return SplitDto(place, street, houseNumber)
    }

}

