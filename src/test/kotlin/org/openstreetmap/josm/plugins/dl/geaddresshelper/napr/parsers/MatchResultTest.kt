package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.MatchFlag
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.MatchResult
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.MatchStatus
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.Matcher
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.MutablePair
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.StreetNameMatcher

class MatchResultTest {
    @Test
    fun compareTo_wordCountNotMatch() {
        val matchResult1 = MatchResult(
            listOf(MutablePair("", MatchStatus.NOT_MATCHED), MutablePair("", MatchStatus.NOT_MATCHED)),
            listOf(MutablePair("", MatchStatus.NOT_MATCHED)),
            mutableListOf(),
            0,
        )
        val matchResult2 = MatchResult(
            listOf(MutablePair("", MatchStatus.NOT_MATCHED)),
            listOf(MutablePair("", MatchStatus.NOT_MATCHED)),
            mutableListOf(),
            0,
        )

        val compareTo = matchResult1.compareTo(matchResult2)

        assert(compareTo > 0)
    }

    @Test
    fun compareTo_flags() {
        val matchResult1 = MatchResult(
            listOf(MutablePair("", MatchStatus.NOT_MATCHED)),
            listOf(MutablePair("", MatchStatus.NOT_MATCHED)),
            mutableListOf(MatchFlag.ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT),
            0,
        )
        val matchResult2 = MatchResult(
            listOf(MutablePair("", MatchStatus.NOT_MATCHED)),
            listOf(MutablePair("", MatchStatus.NOT_MATCHED)),
            mutableListOf(),
            0,
        )

        val compareTo = matchResult1.compareTo(matchResult2)

        assert(compareTo > 0)
    }


    @Test
    @DisplayName("должно быть не не более 1 отличающегося слова")
    fun match_2() {
        val street1 = "ლევან II დადიანის I ჩიხი"
        val street2 = "დავით აღმაშენებლის II ჩიხი"
        val isMatched = Matcher(street1, street2).match().isMatched()
        val isMatched2 = Matcher(street2, street1).match().isMatched()
        assertEquals(false, isMatched)
        assertEquals(false, isMatched2)
    }

}