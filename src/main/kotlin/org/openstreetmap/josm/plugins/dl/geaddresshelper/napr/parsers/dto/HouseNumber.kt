package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlag

data class HouseNumber(
    val source: String,
    val extractedNumber: String,
    val flags: List<ParsingFlag>,
    val isSuccess: Boolean
)