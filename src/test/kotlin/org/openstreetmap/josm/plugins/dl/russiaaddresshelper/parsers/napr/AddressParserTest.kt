//package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr
//
//import org.apache.commons.text.similarity.JaroWinklerSimilarity
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.Disabled
//import org.junit.jupiter.api.Test
//
//class AddressParserTest {
////    @Test
////    fun parse() {
////        val parsedAddress = splitAddress("город , улица, дом")
////
////        assertEquals(listOf("улица", "дом"), parsedAddress)
////    }
//
//    //todo
//    @Disabled
//    @Test
//    fun parse0() {
//        val parsedAddress = NAddressParser().splitAddress("ქალაქი ზესტაფონი , ქუჩა დემეტრე თავდადებული , შესახვევი I , N 5")
//        assertEquals(listOf("ქუჩა დემეტრე თავდადებული შესახვევი I", "N 5"), parsedAddress)
//    }
//
//    //todo
//    @Disabled
//    @Test
//    fun parse011() {
//        val parsedAddress = NAddressParser().splitAddress("ქალაქი ზუგდიდი , ქუჩა ლომთათიძე , შესახვევი I , N 46")
//        assertEquals(listOf("ქუჩა დემეტრე თავდადებული შესახვევი I", "N 5"), parsedAddress)
//    }
//
//    @Test
//    fun parse01() {
//        val parsedAddress = NAddressParser().splitAddress("ქალაქი ზესტაფონი , ქუჩა დემეტრე თავდადებული , შესახვევი I , N 5")
//        assertEquals(listOf("შესახვევი I", "N 5"), parsedAddress)
//    }
//
//    @Test
//    fun parse1() {
//        val parsedAddress = NAddressParser().splitAddress("ქალაქი თბილისი , ქუჩა ნაქალაქევი , N 14")
//
//        assertEquals(listOf("ქუჩა ნაქალაქევი", "N 14"), parsedAddress)
//    }
//
////    @Test
////    fun parse2() {
////        val parsedAddress = NAddressParser().splitAddress("ქალაქი თბილისი , ქუჩა ნაქალაქევი  N 14")
////
////        assertEquals(listOf("ქუჩა ნაქალაქევი", "N 14"), parsedAddress)
////    }
//
//    @Test
//    fun parseNu() {
//        val parsedAddress = NHouseNumberParser().parse(",, N 14 , ^:")
//
//        assertEquals("14", parsedAddress.extractedNumber)
//    }
//
////    @Test
////    fun splitAddress() {
////        val parsedAddress = NAddressParser().splitAddress("ქალაქი სენაკი , ქუჩა სანკტ-პეტერბურგი , N 33-35")
////
////        assertEquals(listOf( "ქუჩა სანკტ-პეტერბურგი" , "N 33-35"), parsedAddress)
////
////        val parseStreet = NStreetParser().parse(parsedAddress[0])
////        assertEquals("სანკტ-პეტერბურგის ქუჩა", parseStreet)
////    }
//
//    @Test
//    fun moveToBack() {
//        val actual = StreetParser().moveToBack("ქუჩა ვაჟა ფშაველა", setOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))
//
//        assertEquals("ვაჟა ფშაველას ქუჩა", actual)
//    }
//
//    @Test
//    fun moveToBack1() {
//        val actual = StreetParser().moveToBack("შესახვევი I", setOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))
//
//        assertEquals("I შესახვევი", actual)
//    }
//
//    @Test
//    fun moveToBack11() {
//        val actual = StreetParser().moveToBack("ქუჩა დემეტრე თავდადებულის", setOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))
//
//        assertEquals("დემეტრე თავდადებულის ქუჩა", actual)
//    }
//    @Test
//    fun moveToBack111() {
//        val actual = StreetParser().moveToBack("ქუჩა გია აბესაძის", setOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))
//
//        assertEquals("გია აბესაძის ქუჩა", actual)
//    }
//
////    " ქუჩა ვაჟა ფშაველა "
//
////    @Test
////    fun parseStreet() {
////        val actual = NStreetParser().parse(" ქუჩა ვაჟა ფშაველა ")
////
////        assertEquals("ვაჟა-ფშაველას ქუჩა", actual)
////    }
////
////    @Test
////    fun parseStreet1() {
////        val actual = NStreetParser().parse(" ქუჩა სანკტ-პეტერბურგი ")
////
////        assertEquals("სანკტ-პეტერბურგის ქუჩა", actual)
////    }
////
////    @Test
////    fun cleanGeorgianRomanNumerals() {
////        val actual = "ქუთაისის Iს ჩიხი".cleanGeorgianRomanNumerals()
////
////        assertEquals("ქუთაისის I ჩიხი", actual)
////    }
//
//
//
//    @Test
//    fun sd() {
//        val jwSimilarity = JaroWinklerSimilarity()
//        val apply = jwSimilarity.apply("კლდიაშვილის", "დავით კლდიაშვილის")
//       // assertEquals(1.0, apply)
//    }
//
//
//
//
//}