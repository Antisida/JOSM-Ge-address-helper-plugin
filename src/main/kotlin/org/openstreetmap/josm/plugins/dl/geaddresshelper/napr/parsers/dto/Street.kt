package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags

data class Street(
    val naprStreetName: String,
    val extractedName: String,
//    val extractedType: StreetType?,
    val flags: MutableList<ParsingFlags>,
    val isSuccess: Boolean
)