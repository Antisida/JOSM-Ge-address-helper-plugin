package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.HouseNumber
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Place
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Street

class MainParserTest {
    @Test
    @DisplayName("simple")
    fun parse_simple() {
        val raw = " ქალაქი სენაკი, შოთა რუსთაველის ქუჩა, N 289"
        val expectedAddress = Address(
            raw,
            listOf(Place("ქალაქი სენაკი", "ქალაქი", "სენაკი", mutableListOf(),  true)),
            Street( " შოთა რუსთაველის ქუჩა", "შოთა რუსთაველის ქუჩა", mutableListOf(), true),
            HouseNumber(" N 289", "289", emptyList(), true),
            mutableListOf(),
            true )

        val address: Address = MainParser.parse(listOf(raw))[0]

        assertEquals(address, expectedAddress)
    }

}