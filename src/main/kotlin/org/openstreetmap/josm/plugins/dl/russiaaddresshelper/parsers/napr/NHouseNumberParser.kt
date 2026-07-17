package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.Patterns
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.IParser
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsedHouseNumber

class NHouseNumberParser : IParser<ParsedHouseNumber> {
    val NOT_PARSED: String = "NOT_PARSED"
    fun parse(sourceString: String): ParsedHouseNumber {
        val parsingFlags = mutableListOf<ParsingFlags>()
        val parseContext = ParseContext(sourceString)
            .removeParenthesesContent(ParsingFlags.PARENTHESES_REMOVED)
            .log("After removeParenthesesContent")
            .collapseDuplicateCommas(ParsingFlags.COMMAS_COLLAPSED)
            .log("After collapseDuplicateCommas")
            .replace("N", "")
            .replace(" ", "")
            // 1. Удаляем не-буквы и не-цифры в самом начале строки
            .replace(Regex("(?U)^[^\\p{L}\\p{N}]+"), "")
            .log("After Удаляем не-буквы и не-цифры в начале строки")
            // 2. Удаляем не-буквы и не-цифры в самом конце строки
            .replace(Regex("(?U)[^\\p{L}\\p{N}]+$"), "")
            .log("After Удаляем не-буквы и не-цифры в конце строки")
        val regex = """(?U)\d{4}""".toRegex() //4 цифры подряд
        if (regex.containsMatchIn(parseContext.str)) {
            parsingFlags.add(ParsingFlags.HOUSENUMBER_CANNOT_BE_PARSED)
            return ParsedHouseNumber(sourceString,NOT_PARSED , parseContext.flags, false)
        }
        return ParsedHouseNumber(sourceString, parseContext.str, parseContext.flags, true)
    }

    override fun parse(address: String, requestCoordinate: EastNorth, editDataSet: DataSet): ParsedHouseNumber {
        return parse(address)


//        val parsingFlags = mutableListOf<ParsingFlags>()
//        for (pattern in patterns) {
//            val match = pattern.find(address)
//
//            if (match != null) {
//
//                var houseNumber =
//                    match.groups["housenumber"]!!.value.filterNot { it == '"' || it == ' ' || it == '-' || it == '«' || it == '»' }
//                        .trim().uppercase()
//                if (houseNumber.matches(Regex("""\d{4,}"""))) {
//                    Logging.error("EGRN-PLUGIN Cant parse housenumber from address: $address, housenumber too big")
//                    parsingFlags.add(ParsingFlags.HOUSENUMBER_TOO_BIG)
//                    return ParsedHouseNumber("", "", null, parsingFlags)
//                }
//                val letter = match.groups["letter"]?.value?.trim()?.uppercase() ?: ""
//                houseNumber += letter
//                val buildingNumber =
//                    match.groups["building"]?.value?.filterNot { it == '"' || it == ' ' }?.trim()?.uppercase()
//                val corpusNumber =
//                    match.groups["corpus"]?.value?.filterNot { it == '"' || it == ' ' }?.trim()?.uppercase()
//
//
//                if (buildingNumber != null) {
//                    //наверное темплейт выхлопа тоже нужно вынести в конфигурацию
//                    houseNumber = "$houseNumber с$buildingNumber"
//                }
//
//                if (corpusNumber != null) {
//                    houseNumber = "$houseNumber к$corpusNumber"
//                }
//
//                val flatNumbers1 = match.groups["flat1"]?.value?.filterNot { it == '-' || it == ' ' }
//                val flatNumbers2 = match.groups["flat2"]?.value
//                val roomNumbers = match.groups["room"]?.value
//                val parsedFlats = flatNumbers1 ?: flatNumbers2 ?: ""
//                if (StringUtils.isNotBlank(flatNumbers1) || StringUtils.isNotBlank(flatNumbers2) || StringUtils.isNotBlank(
//                        roomNumbers
//                    )
//                ) {
//                    parsingFlags.add(ParsingFlags.HOUSENUMBER_HAS_FLATS)
//                    Logging.info("EGRN-PLUGIN Parsed and removed flat numbers from address $address : $parsedFlats $roomNumbers ")
//                }
//
//                return ParsedHouseNumber(houseNumber, parsedFlats, pattern, parsingFlags)
//            }
//            Logging.error("EGRN-PLUGIN Cant parse housenumber from address: $address")
//        }
//        if (address.matches(Regex("""\d"""))) {
//            Logging.error("EGRN-PLUGIN Cant parse housenumber from address: $address, though address contains some numbers")
//            parsingFlags.add(ParsingFlags.HOUSENUMBER_CANNOT_BE_PARSED_BUT_CONTAINS_NUMBERS)
//        } else {
//            parsingFlags.add(ParsingFlags.HOUSENUMBER_CANNOT_BE_PARSED)
//        }
//        return ParsedHouseNumber("", "", null, parsingFlags)
    }

    fun String.removeParenthesesContent(): String {
        // Регулярное выражение находит ( и ) и всё между ними (жадный поиск исключая закрывающую скобку)
        val regex = """(?U)\([^)]*\)""".toRegex()

        return this.replace(regex, "")
            .replace("""(?U)\s+""".toRegex(), " ") // Схлопываем разбежавшиеся пробелы в один
            .trim()                            // Убираем пробелы по краям, если скобки были в начале/конце
    }
}


