package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Street

object StreetParser {

    fun parse(sourceString: String?): Street? {
        if (sourceString == null) return null
        return ParseContext(sourceString)
            .log("StreetParser START")
            .removeParenthesesContent(ParsingFlag.PARENTHESES_REMOVED)
            .trim()
            .log("After removeParenthesesContent")
            .expandAbbreviations(ParsingFlag.HAS_ABBREVIATIONS)
            .trim()
            .log("After expandAbbreviations")
            .cleanRomanNumerals(ParsingFlag.NUMERAL_CLEANED)
            .cleanArabNumerals(ParsingFlag.NUMERAL_CLEANED)
            .log("After cleanRomanNumerals/cleanArabNumerals")
            .trim()
            .moveStatusToBack(ParsingFlag.STATUS_MOVED, ParsingFlag.GENITIVE_APPLIED)
            .log("After moveStatusToBack")
            .trim()
            .normalizeIdioms(ParsingFlag.IDIOMS_NORMALIZED)
            .log("After normalizeIdioms")
            .trim()
            .collapseDuplicateCommas(ParsingFlag.COMMAS_COLLAPSED)
            .log("After collapseDuplicateCommas")
            .replace("N", "")//sometimes 'ქუჩა N 4'
            .trim()
            .let {
                Street(
                    sourceString,
                    it.str,
                    it.flags.toMutableList(),
                    it.str.isNotBlank(),
                )
            }
    }
}
