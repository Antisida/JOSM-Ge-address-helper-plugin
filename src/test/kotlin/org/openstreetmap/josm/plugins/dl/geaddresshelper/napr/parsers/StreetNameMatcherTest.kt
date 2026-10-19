package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StreetNameMatcherTest {
    @Test
    fun getMatchResult() {
        val street1 = "შალვა ნუცუბიძის ქუჩა"
        val street2 = "შ ნუცუბიძის ქუჩა"
//        val street2 = "ნუცუბიძის"

        val matchResult = Matcher( street1 , street2,).match()

        assertEquals(matchResult.isMatched(), true)
    }

}