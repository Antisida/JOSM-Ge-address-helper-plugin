package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.validation.vocabulary

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VocabularyTest {
    @Test
    fun findBestAddress() {
        val findBestAddress = Vocabulary.findBestAddress("ა. დანელიას ქუჩა")

        assertEquals("ამირან დანელიას ქუჩა", findBestAddress)
    }

    @Test
    fun findBestAddress1() {
        val findBestAddress = Vocabulary.findBestAddress("ამირან დანელიას ქუჩა")

        assertEquals("ამირან დანელიას ქუჩა", findBestAddress)
    }

    @Test
    fun findBestAddress2() {
        val findBestAddress = Vocabulary.findBestAddress("улица")

        assertEquals(null, findBestAddress)
    }

}