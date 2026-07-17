package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsedHouseNumber
import org.openstreetmap.josm.tools.Logging
import kotlin.collections.any
import kotlin.collections.map


/**
 * Логика завязана на наличие 'N'. По опыту все номера домов ей предваряются.
 * Если ее нет вычленение номера дома и улицы не происходит.
 */
class NAddressParser {

    val statuses =
        setOf("ქუჩა", "ქ.", "გამზირი", "გამზ.", "ბულვარი", "ჩიხი", "შესახვევი", "შეს.", "გასასვლელი", "აღმართი")

    /**
     * Удаляет из строки круглые скобки и всё, что находится внутри них.
     * Также очищает лишние двойные пробелы, которые могли образоваться после удаления.
     */
    fun String.removeParenthesesContent(): String {
        // Регулярное выражение находит ( и ) и всё между ними (жадный поиск исключая закрывающую скобку)
        val regex = """(?U)\([^)]*\)""".toRegex()

        return this.replace(regex, "")
            .replace("""(?U)\s+""".toRegex(), " ") // Схлопываем разбежавшиеся пробелы в один
            .trim()                            // Убираем пробелы по краям, если скобки были в начале/конце
    }


    //todo написать тест для этого ქალაქი ფოთი , ქუჩა ერთობა , N 43 (ყოფ.ძნელაძის ქ. N 43)
    fun parse(sourceFullString: String): N_ParsedAddress {
        val split = splitAddress(sourceFullString.removeParenthesesContent().insertMissingComma().removeCommasBetweenStatuses(statuses.toList()))
        if (split.size == 2) {
            val parsedStreet: N_ParsedStreet = NStreetParser().parse(split[0])
            val parsedNumber: ParsedHouseNumber = NHouseNumberParser().parse(split[1])

            val tags = composeBuildingTag(sourceFullString, parsedStreet, parsedNumber)
            return N_ParsedAddress(
                sourceFullString,
                parsedStreet,
                parsedNumber,
                mutableListOf(),
                tags,
                parsedStreet.isSuccess && parsedNumber.isSuccess
            )
        }
        return N_ParsedAddress(
            sourceFullString,
            N_ParsedStreet("", "", mutableListOf(), false),
            ParsedHouseNumber("", "", listOf(), false),
            mutableListOf(ParsingFlags.SPLIT_FAILED),
            emptyMap<String, String>().toMutableMap(),
            false
        )
    }

    fun composeBuildingTag(
        sourceFullString: String,
        parsedStreet: N_ParsedStreet,
        parsedNumber: ParsedHouseNumber
    ): MutableMap<String, String> {
        val tags = mutableMapOf<String, String>()
        if (parsedNumber.extractedNumber != null) {
            tags.put("addr:housenumber", parsedNumber.extractedNumber)
        }
        tags.put("addr:street", parsedStreet.extractedName)
        tags.put("napr:addr", sourceFullString)
        return tags
    }


    /** Ищет букву N, если перед ней (через любые пробелы) НЕТ запятой и вставляет запятую. */
    fun String.insertMissingComma(): String {
        return this.replace(Regex("([^,\\s])\\s*N"), "$1, N")
    }

    fun String.removeCommasBetweenStatuses(statuses: List<String>): String {
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

//    /** Расширяет сокращения в названиях грузинских улиц. */
//    fun String.expandAbbreviations(): String {
//        return this.replace("""(?Ui)\bქ\.\s*""".toRegex(), "ქუჩა ")
//            .replace("""(?Ui)\bგამზ\.\s*""".toRegex(), "გამზირი ")
//            .replace("""(?Ui)\bშეს\.\s*""".toRegex(), "შესახვევი ")
//    }


   /* fun parse(address: String, editDataSet: DataSet): N_ParsedAddress {
        val split = splitAddress(address)
        //todo слишком примитивно надо доработать
        if (split.size == 2) {
            //todo нужен рефакторинг: парсинг отдельно, флаги отдельно
            val streetParseResult: N_ParsedStreet = NStreetParser().parse(split[0], editDataSet)
            val houseNumberParseResult: ParsedHouseNumber = NHouseNumberParser().parse1(split[1])

            val allParsingFlags: MutableList<ParsingFlags> = mutableListOf()
//            allParsingFlags.addAll(placeParseResult.flags)
            allParsingFlags.addAll(streetParseResult.flags)
            allParsingFlags.addAll(houseNumberParseResult.flags)

            if (TagSettingsReader.ADDRESS_STOP_WORDS.get().any { address.contains(it, true) }) {
                allParsingFlags.add(ParsingFlags.STOP_LIST_WORDS) //todo
            }
            Logging.info("1: ${streetParseResult.extractedName}")
            return N_ParsedAddress(
//            ParsedPlace("", "", null, emptyList(), emptyList()), //fixme
                streetParseResult,
                houseNumberParseResult,
//            "",
                allParsingFlags
            )
        } else {
            return N_ParsedAddress(
//            ParsedPlace("", "", null, emptyList(), emptyList()), //fixme
                N_ParsedStreet("", "", "", listOf(), emptyList<ParsingFlags>().toMutableList()),
                ParsedHouseNumber("", "", null, emptyList<ParsingFlags>().toMutableList()),
//            "",
                emptyList<ParsingFlags>().toMutableList()

            )
        }
    }*/
//    else {
//
//        return ParsedAddress(
//            ParsedPlace("", "", null, emptyList(), emptyList()), //fixme
//            streetParseResult,
//            houseNumberParseResult,
//            "",
//            allParsingFlags
//        )
//        }

    /* fun splitAddress(fullNaprAddress: String): List<String> {
         // Ищет букву N, если перед ней (через любые пробелы) НЕТ запятой и вставляет запятую
         return fullNaprAddress.replace(Regex("([^,\\s])\\s*N"), "$1, N")
             .expandStreetPrefixes()
             .removeParenthesesContent()
             .collapseDuplicateCommas() //ქალაქი სენაკი , ქუჩა ლ. თეფანია ,   (ყოფ. ჯორჯიაშვილი) , N 16
             .split(",")
             .filter { line -> "N" in line || statuses.any { status -> status in line } }
             .takeLast(2)
             .map { it.trim() }
     }*/

    fun splitAddress(preparedStr: String): List<String> {
        return preparedStr
            .split(",")
            //тут отсекаются все, что не входит в статусы, например, участок
            .filter { line -> "N" in line || statuses.any { status -> status in line } } //todo не отсекаются ли номера без N? Есть ли такие?
            .takeLast(2)
            .map { it.trim() }
    }

   /* fun parseHouseNumber(str: String): String {
        val hn = str
            .removeParenthesesContent()
            .replace("N", "")
            .replace(" ", "")
            // 1. Удаляем не-буквы и не-цифры в самом начале строки
            .replace(Regex("(?U)^[^\\p{L}\\p{N}]+"), "")
            // 2. Удаляем не-буквы и не-цифры в самом конце строки
            .replace(Regex("(?U)[^\\p{L}\\p{N}]+$"), "")
        val regex = """(?U)\d{4}""".toRegex() //4 цифры подряд
        if (regex.containsMatchIn(hn)) throw IllegalArgumentException("4 цифры подряд в номере дома")
        return hn
    }*/

    fun String.cleanGeorgianRomanNumerals(): String {
        val regex = """(?U)\b(I|II|III|IV|V|VI|VII|VIII|IX|X)ს\b""".toRegex()
        return this.replace(regex, "$1")
    }


    fun String.normalizeIdioms(): String {
        return this
            .replace("ვაჟა ფშაველა", "ვაჟა-ფშაველა")
            .replace("ზ. გამსახურდია", "ზვიად გამსახურდია")
            .replace("ც. დადიანი", "ცოტნე დადიანი")
            .replace("ნ. ბარათაშვილი", "ნიკოლოზ ბარათაშვილი")
            .replace("პუშკინი", "ალექსანდრე პუშკინი")
    }


    /**
     * Расширяет сокращения в названиях грузинских улиц.
     */
    fun String.expandStreetPrefixes(): String {
        return this.replace("ქ. ", "ქუჩა ")
            .replace("გამზ. ", "გამზირი ")
            .replace("შეს. ", "შესახვევი ")
    }

    fun String.log(index: String): String {
        Logging.info("$index: $this")
        return this
    }

   /* //todo переделать этот класс в объект
    fun parseStreet(str: String): String {
        var s = str
            .log("Начало")
            .removeParenthesesContent()
            .log("После удаления скобок")
            .cleanGeorgianRomanNumerals()
            .log("После преобразования латинских цифр")
            .trim()
        if (statuses.count { s.contains(it) } == 1) {
            s = moveToBack(s, listOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))
        }
        s = s.log("После перемещения статуса в конец")
            .normalizeIdioms()
            .log("После идиом")
            .trim()

        return s
    }*/

    fun moveToBack(input: String, movables: List<String>): String {
        if (movables.isEmpty()) return input
        Logging.info("4: $input")
        val keywordsPattern = movables.joinToString("|") { Regex.escape(it) }
        Logging.info("5: $keywordsPattern")
        // Регулярное выражение ищет любое из слов в начале строки
        val regex = """(?U)^($keywordsPattern)\b\s*(.*)""".toRegex(RegexOption.IGNORE_CASE)

        return regex.replace(input) { matchResult ->
            val keyword = matchResult.groupValues[1]
            val restOfString = matchResult.groupValues[2]
            Logging.info("6: $keyword")
            Logging.info("7: $restOfString")

            if (restOfString.isNotEmpty()) {
                if (restOfString in listOf("I", "V", "X") || restOfString.endsWith("ს")) "$restOfString $keyword"
                else "${restOfString}ს $keyword"
            } else keyword
        }
    }


   /* //todo убирать тире перед буквой в номере дома 9-ა
    public fun parseResponse(naprBody: RawNaprDto?): MutableMap<String, String> {
        val tags: MutableMap<String, String> = mutableMapOf()
        if (naprBody == null) {
            return tags
        }
        Logging.info("$naprBody")
        // Собираем все строки в один плоский список
        val allAddrStrings = naprBody.result
            ?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val rowAddrTags: Map<String, String> = allAddrStrings
            .distinct()
            .mapIndexed { index, string -> "napr:addr:row:${index + 1}" to string }
            .toMap()
        tags.putAll(rowAddrTags)
        val fullAddr = allAddrStrings
            .let { allStrings ->
                // План А: Ищем самую длинную с буквой "N".
                allStrings
                    .filter { !it.contains("ნაკვეთი") } // ნაკვეთი - участок
                    .filter { it.contains("N") }.maxByOrNull { it.length }
                // План Б: Если строки с N нет, берем самую длинную из всех остальных
                    ?: allStrings.maxByOrNull { it.length } //todo тут надо возвращать два набора тэгов, которые идут в точку и которые в здание
            }
        if (fullAddr == null) {
            Logging.info("Empty response. Body: $naprBody")
        } else {
            tags.putAll(mapOf("addr:GE:napr" to fullAddr.trim()))
            if (fullAddr.contains("N")) {
                try {
                    val split = splitAddress(fullAddr)
                    if (split.size == 2) {
                        val streetValue = parseStreet(split[0])
                        Logging.info("10: $streetValue")
                        val houseNumberValue = parseHouseNumber(split[1])
                        tags.putAll(
                            mapOf(
                                "addr:street" to streetValue,
                                "addr:housenumber" to houseNumberValue
                            )
                        )
                    }
                } catch (_: IllegalArgumentException) {
                }
            } else {
                //Logging.error("size < 2")
            }
        }
        return tags
    }
*/
    /*fun processResponse(result: Result<RawNaprDto, FuelError>): MutableMap<String, String> {
        val defaultTagsForNode: Map<String, String> = mapOf("source:addr" to "napr.gov.ge", "fixme" to "REMOVE ME!")
        val tagsForNode: MutableMap<String, String> = mutableMapOf()
        result.success { naprBody ->
            Logging.info("$naprBody")
            // Собираем все строки в один плоский список
            val allAddrStrings = naprBody.result
                ?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            val fullAddr = allAddrStrings
                .let { allStrings ->
                    // План А: Ищем самую длинную с буквой "N".
                    allStrings.filter { it.contains("N") }.maxByOrNull { it.length }
                    // План Б: Если План А вернул null, берем самую длинную из всех
                        ?: allStrings.maxByOrNull { it.length }
                }
            if (fullAddr == null) {
                Logging.info("Empty response. Body: $naprBody")
            } else {
                // Проверяем: если адрес нашли, но в нем нет "N" — взводим флаг warn
                tagsForNode.putAll(defaultTagsForNode)
                tagsForNode.putAll(mapOf("addr:GE:napr" to fullAddr))
                if (fullAddr.contains("N")) {
                    val split = splitAddress(fullAddr)
                    if (split.size != 2) {
                        return@success
                    }
                    val streetValue = parseStreet(split[0])
                    val houseNumberValue = parseHouseNumber(split[1])
                    tagsForNode.putAll(
                        mapOf(
                            "addr:street" to streetValue, "addr:housenumber" to houseNumberValue
                        )
                    )
                }
            }
        }
        return tagsForNode
    }*/
}

