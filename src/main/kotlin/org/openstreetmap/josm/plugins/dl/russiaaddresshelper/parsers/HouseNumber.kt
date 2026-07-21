package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsingFlags

data class HouseNumber(
    val naprHouseNumber: String,
    val extractedNumber: String,
    val flags: List<ParsingFlags>,
    val isSuccess: Boolean
)
