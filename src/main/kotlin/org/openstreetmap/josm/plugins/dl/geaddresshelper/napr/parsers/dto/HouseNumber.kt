package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags

data class HouseNumber(
    val naprHouseNumber: String,
    val extractedNumber: String,
    val flags: List<ParsingFlags>,
    val isSuccess: Boolean
)