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


    @Test
    @DisplayName("")
    fun parse() {
        val raw1 = "ქალაქი ფოთი, გურიის ქუჩა N97"
        val raw2 = " გურიას ქუჩა, N 97"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse1() {
        val raw1 = "ქალაქი ფოთი, შოთა რუსთაველის რკალი, N 20"
        val raw2 = " შოთა რუსთაველის რკალი, N 20"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse2() {
        val raw1 = "ქალაქი ფოთი , ქუჩა მცხეთა , N 10"
        val raw2 = " მცხეთის ქუჩა, N 10"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse3() {
        val raw1 = "ქალაქი ფოთი , ქუჩა მცხეთა , N 8"
        val raw2 = " მცხეთის ქუჩა, N 8"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse4() {
        val raw1 = "ქალაქი ფოთი , ქუჩა მცხეთა , N 11"
        val raw2 = " მცხეთის ქუჩა, N 11"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse5() {
        val raw1 = "ქალაქი ფოთი ,   კონსტიტუციის ქუჩა  N143 (ყოფილი N125)"
        val raw2 = " კონსტიტუციას ქუჩა, N 143"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse6() {
        val raw1 = "ქალაქი ქუთაისი , ქუჩა ლ. ქიაჩელი , N 5"
        val raw2 = " ლეო ქიაჩელის ქუჩა, N 5"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse7() {
        val raw1 = "ქალაქი ქუთაისი , ქუჩა წმ. ლუკა , N 56"
        val raw2 = " წმინდა ლუკას ქუჩა, N 56"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse8() {
        val raw1 = "ქალაქი ქუთაისი , ქუჩა წმინდა ლუკას ,   ქუჩა N62"
        val raw2 = " წმინდა ლუკას ქუჩა, N 62"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse9() {
        val raw1 = "ქალაქი ქუთაისი , ქუჩა აკ. ბელიაშვილი , N 24"
        val raw2 = " აკაკი ბელიაშვილის ქუჩა, N 24"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }


    @Test
    @DisplayName("")
    fun parse10() {
        val raw1 = "ქალაქი ფოთი , ქუჩა გ. სააკაძე , N 113"
        val raw2 = " გიორგი სააკაძის ქუჩა, N 113"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
    }

    @Test
    @DisplayName("")
    fun parse11() {
        val raw1 = "ქალაქი ფოთი , ქუჩა გ. სააკაძე , N 228"
        val raw2 = " გიორგი სააკაძის ქუჩა, N 228"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
        assertEquals(addresses[0].street.extractedName, "გიორგი სააკაძის ქუჩა")
    }

    @Test
    @DisplayName("замена 1-ელი на 1-ლი")
    fun parse12() {
        val raw1 = "ამბროლაურის მუნიციპალიტეტი, სოფელი ხვანჭკარა, 1-ლი ქუჩის II შესახვევი, N 5"
        val raw2 = " სოფელი ხვანჭკარა, 1-ელი ქუჩის II შესახვევი, N 5"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
        assertEquals(addresses[0].street.extractedName, "1-ლი ქუჩის II შესახვევი")
    }

    @Test
    @DisplayName("не добавляем с если содержатся цифры")
    fun parse13() {
        val raw1 = "რაიონი ამბროლაური , სოფელი წესი , ქუჩა 22-ე , N 6"
        val addresses: List<Address> = MainParser.parse(listOf(raw1))
        assertEquals(addresses.size, 1)
        assertEquals(addresses[0].street.extractedName, "22-ე ქუჩა")
    }

    @Test
    @DisplayName("не добавляем с если содержатся цифры")
    fun parse14() {
        val raw1 = "რაიონი ამბროლაური , სოფელი მუხლი , ქუჩა მე-5 , N 3"
        val addresses: List<Address> = MainParser.parse(listOf(raw1))
        assertEquals(addresses.size, 1)
        assertEquals(addresses[0].street.extractedName, "მე-5 ქუჩა")
    }

    @Test
    @DisplayName("")
    fun parse15() {
        val raw1 = "ქალაქი წალკა , ქუჩა მ. ჭელიძის ( სტალინის)  , სახლი N 41"
        val raw2 = " ქალაქი წალკა, მერაბ ჭელიძის ქუჩა, N 41"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
        assertEquals(addresses[0].street.extractedName, "მერაბ ჭელიძის ქუჩა")
    }

    @Test
    @DisplayName("")
    fun parse16() {
        val raw1 = "მუნიცპალიტეტი წალკა, ადმინისტრაციული ერთეული წალკა. ქალაქი წალკა , ქუჩა დ. აღმაშენებლის ( კოოპერაციის ) , სახლი N 2"
        val raw2 = " ქალაქი წალკა, დავით აღმაშენებლის ქუჩა, N 2"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
        assertEquals(addresses[0].street.extractedName, "დავით აღმაშენებლის ქუჩა")
        assertEquals(addresses[0].houseNumber.extractedNumber, "2")
    }

    @Test
    @DisplayName("")
    fun parse17() {
        val raw1 = "ქალაქი ოზურგეთი , ქუჩა შ. რუსთაველი , N 103"
        val raw2 = " ქალაქი ოზურგეთი, შოთა რუსთაველის ქუჩა, N 103"
        val addresses: List<Address> = MainParser.parse(listOf(raw1,raw2))
        assertEquals(addresses.size, 1)
        assertEquals(addresses[0].street.extractedName, "შოთა რუსთაველის ქუჩა")
    }
}