/*
package org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.napr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.ParsingFlags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.HouseNumber
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.Street

class MainParserTest {
    @Test
    fun dropDuble() {
        val mainParser = MainParser() // Убедитесь, что методы выше лежат внутри этого класса
        val listOf = listOf(
            Address(
                "sourceFullString",
                Street("", "улица Ильи Соколова", mutableListOf(), false),
                HouseNumber("", "", listOf(), false),
                mutableListOf(ParsingFlags.SPLIT_FAILED),
                emptyMap<String, String>().toMutableMap(),
                false
            ),
            Address(
                "sourceFullString",
                Street("", "улица Соколова", mutableListOf(), false),
                HouseNumber("", "", listOf(), false),
                mutableListOf(ParsingFlags.SPLIT_FAILED),
                emptyMap<String, String>().toMutableMap(),
                false
            )
        )

        val actual = mainParser.dropDuble(listOf).first().parsedStreet.extractedName
        // Правильный порядок: сначала ожидаемое (expected), затем фактическое (actual)
        assertEquals("улица Ильи Соколова", actual)
    }

}*/
