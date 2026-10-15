package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OsmStreetMatcherTest {
    @Test
    fun checkMatch() {
        val raw1 = "აქვსენტი ცაგარელის ქუჩა"
        val raw2 = "ქუჩა ა. ცაგარელი"
        val result = OsmStreetMatcher.getMatchResult(raw1, query = raw2)

        assertNotNull(result)
    }

    @Test
    @DisplayName("одинаковые имена и разные фамилии")
    fun getBestOrNull_1() {
        val street1 = "შალვა ნუცუბიძის ქუჩა"
        val street2 = "შალვა ამირანაშვილის ქუჩა"
        val raw = " შალვა ნუცუბიძის ქუჩა"
        val result = OsmStreetMatcher.getBestOrNull(listOf(street1, street2), query = raw)

        assertEquals(result, street1)
    }

    @Test
    @DisplayName("одинаковые имена и разные фамилии, кандидат без имени")
    fun getBestOrNull_2() {
        val street1 = "შალვა ნუცუბიძის ქუჩა"
        val street2 = "შალვა ამირანაშვილის ქუჩა"
        val raw = "ნუცუბიძის ქუჩა"
        val result = OsmStreetMatcher.getBestOrNull(listOf(street1, street2), query = raw)

        assertEquals(result, street1)
    }

    @Test
    @DisplayName("одинаковые имена и разные фамилии, кандидат с сокращением")
    fun getBestOrNull_3() {
        val street1 = "შალვა ნუცუბიძის ქუჩა"
        val street2 = "შალვა ამირანაშვილის ქუჩა"
        val raw = "შ ნუცუბიძის ქუჩა"
        val result = OsmStreetMatcher.getBestOrNull(listOf(street1, street2), query = raw)

        assertEquals(result, street1)
    }

}