package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class AddressParserTest {
//    @Test
//    fun parse() {
//        val parsedAddress = splitAddress("город , улица, дом")
//
//        assertEquals(listOf("улица", "дом"), parsedAddress)
//    }

    //todo
    @Disabled
    @Test
    fun parse0() {
        val parsedAddress = splitAddress("ქალაქი ზესტაფონი , ქუჩა დემეტრე თავდადებული , შესახვევი I , N 5")
        assertEquals(listOf("ქუჩა დემეტრე თავდადებული შესახვევი I", "N 5"), parsedAddress)
    }

    //todo
    @Disabled
    @Test
    fun parse011() {
        val parsedAddress = splitAddress("ქალაქი ზუგდიდი , ქუჩა ლომთათიძე , შესახვევი I , N 46")
        assertEquals(listOf("ქუჩა დემეტრე თავდადებული შესახვევი I", "N 5"), parsedAddress)
    }

    @Test
    fun parse01() {
        val parsedAddress = splitAddress("ქალაქი ზესტაფონი , ქუჩა დემეტრე თავდადებული , შესახვევი I , N 5")
        assertEquals(listOf("შესახვევი I", "N 5"), parsedAddress)
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

    @Test
    fun splitAddress() {
        val parsedAddress = splitAddress("ქალაქი სენაკი , ქუჩა სანკტ-პეტერბურგი , N 33-35")

        assertEquals(listOf( "ქუჩა სანკტ-პეტერბურგი" , "N 33-35"), parsedAddress)

        val parseStreet = parseStreet(parsedAddress[0])
        assertEquals("სანკტ-პეტერბურგის ქუჩა", parseStreet)
    }

    @Test
    fun moveToBack() {
        val actual = moveToBack("ქუჩა ვაჟა ფშაველა", listOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))

        assertEquals("ვაჟა ფშაველას ქუჩა", actual)
    }

    @Test
    fun moveToBack1() {
        val actual = moveToBack("შესახვევი I", listOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))

        assertEquals("I შესახვევი", actual)
    }

    @Test
    fun moveToBack11() {
        val actual = moveToBack("ქუჩა დემეტრე თავდადებულის", listOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))

        assertEquals("დემეტრე თავდადებულის ქუჩა", actual)
    }
    @Test
    fun moveToBack111() {
        val actual = moveToBack("ქუჩა გია აბესაძის", listOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))

        assertEquals("გია აბესაძის ქუჩა", actual)
    }

//    " ქუჩა ვაჟა ფშაველა "

    @Test
    fun parseStreet() {
        val actual = parseStreet(" ქუჩა ვაჟა ფშაველა ")

        assertEquals("ვაჟა-ფშაველას ქუჩა", actual)
    }

    @Test
    fun parseStreet1() {
        val actual = parseStreet(" ქუჩა სანკტ-პეტერბურგი ")

        assertEquals("სანკტ-პეტერბურგის ქუჩა", actual)
    }

    @Test
    fun cleanGeorgianRomanNumerals() {
        val actual = "ქუთაისის Iს ჩიხი".cleanGeorgianRomanNumerals()

        assertEquals("ქუთაისის I ჩიხი", actual)
    }








}