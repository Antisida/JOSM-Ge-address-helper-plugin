package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsedHouseNumber

class MainParserTest {
    @Test
    fun dropDuble() {
        val mainParser = MainParser() // Убедитесь, что методы выше лежат внутри этого класса
        val listOf = listOf(
            N_ParsedAddress(
                "sourceFullString",
                N_ParsedStreet("", "улица Ильи Соколова", listOf(), false),
                ParsedHouseNumber("", "", listOf(), false),
                mutableListOf(ParsingFlags.SPLIT_FAILED),
                emptyMap<String, String>().toMutableMap(),
                false
            ),
            N_ParsedAddress(
                "sourceFullString",
                N_ParsedStreet("", "улица Соколова", listOf(), false),
                ParsedHouseNumber("", "", listOf(), false),
                mutableListOf(ParsingFlags.SPLIT_FAILED),
                emptyMap<String, String>().toMutableMap(),
                false
            )
        )

        val actual = mainParser.dropDuble(listOf).first().parsedStreet.extractedName
        // Правильный порядок: сначала ожидаемое (expected), затем фактическое (actual)
        assertEquals("улица Ильи Соколова", actual)
    }

}