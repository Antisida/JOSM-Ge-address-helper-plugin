package org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.ParsingFlags

data class HouseNumber(
    val naprHouseNumber: String,
    val extractedNumber: String,
    val flags: List<ParsingFlags>,
    val isSuccess: Boolean
)
