package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.junit.jupiter.api.Test

class MatchResultTest {
    @Test
    fun compareTo_wordCountNotMatch() {
        val matchResult1 = MatchResult(
            0,
            0,
            1,
            1,
            77, //
            22,
            11,
            100,
            listOf(MatchFlag.ABBR, MatchFlag.ABBR, MatchFlag.ABBR,)
        )
        val matchResult2 = MatchResult(
            0,
            0,
            1,
            1,
            33, //
            22,
            11,
            100,
            listOf(MatchFlag.ABBR, MatchFlag.ABBR, MatchFlag.ABBR,)
        )

        val compareTo = matchResult1.compareTo(matchResult2)

        assert(compareTo > 0)
    }

    @Test
    fun compareTo_flags() {
        val matchResult1 = MatchResult(
            0,
            0,
            1,
            1,
            77,
            22,
            11,
            100,
            listOf(MatchFlag.ABBR, MatchFlag.ABBR, MatchFlag.ABBR,MatchFlag.ABBR,) //
        )
        val matchResult2 = MatchResult(
            0,
            0,
            1,
            1,
            77,
            22,
            11,
            100,
            listOf(MatchFlag.ABBR, MatchFlag.ABBR, MatchFlag.ABBR,)
        )

        val compareTo = matchResult1.compareTo(matchResult2)

        assert(compareTo > 0)
    }

}