package org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.ParsingFlags

object HouseNumberParser {
    private const val NOT_PARSED: String = "NOT_PARSED"
    fun parse(sourceString: String): HouseNumber {
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
            return HouseNumber(sourceString, NOT_PARSED, parseContext.flags, false)
        }
        return HouseNumber(sourceString, parseContext.str, parseContext.flags, true)
    }
}


