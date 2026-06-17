package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AddressParserTest {
    @Test
    fun parse() {
        val parsedAddress = splitAddress("город , улица, дом")

        assertEquals(listOf("улица", "дом"), parsedAddress)
    }

    @Test
    fun parse1() {
        val parsedAddress = splitAddress("ქალაქი თბილისი , ქუჩა ნაქალაქევი , N 14")

        assertEquals(listOf("ქუჩა ნაქალაქევი", "N 14"), parsedAddress)
    }

    @Test
    fun parse2() {
        val parsedAddress = splitAddress("ქალაქი თბილისი , ქუჩა ნაქალაქევი  N 14")

        assertEquals(listOf("ქუჩა ნაქალაქევი", "N 14"), parsedAddress)
    }

    @Test
    fun parseNu() {
        val parsedAddress = parseHouseNumber(",, N 14 , ^:")

        assertEquals("14", parsedAddress)
    }



}