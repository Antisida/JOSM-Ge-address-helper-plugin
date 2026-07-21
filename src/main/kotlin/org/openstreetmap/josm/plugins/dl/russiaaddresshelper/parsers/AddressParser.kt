package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagCreator.STATUSES_AND_ABBR_SET

object AddressParser {

    fun parse(sourceFullString: String): Address {
        val split = splitAddress(
            sourceFullString
                .removeParenthesesContent()
                .insertMissingComma()
                .removeCommasBetweenStatuses(STATUSES_AND_ABBR_SET.toList())
        )
        if (split.size == 2) {
            val parsedStreet: Street = StreetParser.parse(split[0])
            val parsedNumber: HouseNumber = HouseNumberParser.parse(split[1])

            return Address(
                sourceFullString,
                parsedStreet,
                parsedNumber,
                mutableListOf(),
                parsedStreet.isSuccess && parsedNumber.isSuccess
            )
        }
        return Address(sourceFullString)
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
        val wordRegex = "(?U)\\b(?:$escapedWords)\\b".toRegex()

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

    private fun splitAddress(preparedStr: String): List<String> {
        return preparedStr
            .split(",")
            //тут отсекаются все, что не входит в статусы, например, участок
            .filter { line -> "N" in line || STATUSES_AND_ABBR_SET.any { status -> status in line } } //todo не отсекаются ли номера без N? Есть ли такие?
            .takeLast(2)
            .map { it.trim() }
    }

}

