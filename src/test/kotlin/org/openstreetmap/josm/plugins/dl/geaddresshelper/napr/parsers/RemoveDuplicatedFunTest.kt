package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.HouseNumber
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Place
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Street

class RemoveDuplicatedFunTest {
    @Test
    fun removeDuplicated1() {
    }

    @Test
    fun removeDuplicated() {
        val raw1 = " ქალაქი სენაკი, შოთა რუსთაველის ქუჩა, N 289"
        val expectedAddress1 = Address(
            raw1,
            listOf(Place("ქალაქი სენაკი", "ქალაქი", "სენაკი", mutableListOf(),  true)),
            Street( " შოთა რუსთაველის ქუჩა", "შოთა რუსთაველის ქუჩა", mutableListOf(), true),
            HouseNumber(" N 289", "289", emptyList(), true),
            mutableListOf(),
            true )
        val raw2 = " ქალაქი სენაკი, შოთა რუსთაველის ქუჩა, N 289"
        val expectedAddress2 = Address(
            raw2,
            listOf(Place("ქალაქი სენაკი", "ქალაქი", "სენაკი", mutableListOf(),  true)),
            Street( " შოთა რუსთაველის ქუჩა", "შოთა რუსთაველის ქუჩა", mutableListOf(), true),
            HouseNumber(" N 289", "289", emptyList(), true),
            mutableListOf(),
            true )

        val reduceByStreetName = removeDuplicated(listOf(expectedAddress1, expectedAddress2))

        assert(reduceByStreetName.size == 1)
    }

    @Test
    fun removeDuplicated2() {
        val raw1 = " ქალაქი სენაკი, აქვსენტი ცაგარელის ქუჩა, N 25"
        val raw2 = "ქალაქი სენაკი , ქუჩა ა. ცაგარელი , N 25"
        val addrs = MainParser.parse(listOf(raw1, raw2))

        val reduceByStreetName = removeDuplicated(addrs)

        assert(reduceByStreetName.size == 1)
    }



}