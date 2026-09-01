package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Street

object StreetParser {

    fun parse(sourceString: String?): Street? {
        if (sourceString == null) return null
        return ParseContext(sourceString)
            //            .log("StreetParser START")
            .removeParenthesesContent(ParsingFlags.PARENTHESES_REMOVED)
            //            .log("After removeParenthesesContent")
            .expandAbbreviations(ParsingFlags.HAS_ABBREVIATIONS)
            //            .log("After expandAbbreviations")
            .cleanRomanNumerals(ParsingFlags.ROMAN_NUMERAL_CLEANED)
            //            .log("After cleanRomanNumerals")
            .trim()
            .moveStatusToBack(ParsingFlags.STATUS_MOVED, ParsingFlags.GENITIVE_APPLIED)
            //            .log("After moveStatusToBack")
            .normalizeIdioms(ParsingFlags.IDIOMS_NORMALIZED)
            //            .log("After normalizeIdioms")
            .collapseDuplicateCommas(ParsingFlags.COMMAS_COLLAPSED)
            //            .log("After collapseDuplicateCommas")
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
