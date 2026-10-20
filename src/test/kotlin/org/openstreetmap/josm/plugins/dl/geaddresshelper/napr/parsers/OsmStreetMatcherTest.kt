package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.Matcher
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.StreetNameMatcher

class OsmStreetMatcherTest {
    @Test
    fun checkMatch() {
        val raw1 = "აქვსენტი ცაგარელის ქუჩა"
        val raw2 = "ქუჩა ა. ცაგარელი"
        val result = Matcher(raw1, raw2).match()

        assertNotNull(result)
    }

    @Test
    @DisplayName("одинаковые имена и разные фамилии")
    fun bestOrNull_1() {
        val street1 = "შალვა ნუცუბიძის ქუჩა"
        val street2 = "შალვა ამირანაშვილის ქუჩა"
        val raw = " შალვა ნუცუბიძის ქუჩა"
        val result = StreetNameMatcher.bestLongerMatchedOrNull(listOf(street1, street2), query = raw)

        assertEquals(result, street1)
    }

    @Test
    @DisplayName("одинаковые имена и разные фамилии, кандидат без имени")
    fun bestOrNull_2() {
        val street1 = "შალვა ნუცუბიძის ქუჩა"
        val street2 = "შალვა ამირანაშვილის ქუჩა"
        val raw = "ნუცუბიძის ქუჩა"
        val result = StreetNameMatcher.bestLongerMatchedOrNull(listOf(street1, street2), query = raw)

        assertEquals(result, street1)
    }

    @Test
    @DisplayName("одинаковые имена и разные фамилии, кандидат с сокращением")
    fun bestOrNull_3() {
        val street1 = "შალვა ნუცუბიძის ქუჩა"
        val street2 = "შალვა ამირანაშვილის ქუჩა"
        val raw = "შ ნუცუბიძის ქუჩა"
        val result = StreetNameMatcher.bestLongerMatchedOrNull(listOf(street1, street2), query = raw)

        assertEquals(result, street1)
    }

}