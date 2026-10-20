package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.Matcher
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.StreetNameMatcher

class StreetNameMatcherTest {
    @Test
    fun getMatchResult() {
        val street1 = "შალვა ნუცუბიძის ქუჩა"
        val street2 = "შ ნუცუბიძის ქუჩა"
//        val street2 = "ნუცუბიძის"

        val matchResult = Matcher(street1, street2,).match()

        assertEquals(matchResult.isMatched(), true)
    }


    @Test
    @DisplayName("длинная имя не должно матчиться с коротким")
    fun bestLongerMatchedOrNull() {
        val street1 = "მიხეილ ლერმონტოვის ქუჩა"
        val street2 = "ლერმონტოვის ქუჩა"
        val found = StreetNameMatcher.bestLongerMatchedOrNull(listOf(street2), street1)

        assertNull(found)
    }

    @Test
    @DisplayName("короткое имя  должно матчиться с длинным")
    fun bestLongerMatchedOrNull_2() {
        val street1 = "მიხეილ ლერმონტოვის ქუჩა"
        val street2 = "ლერმონტოვის ქუჩა"
        val found = StreetNameMatcher.bestLongerMatchedOrNull(listOf(street1), street2)

        assertEquals("მიხეილ ლერმონტოვის ქუჩა", found)
    }


}