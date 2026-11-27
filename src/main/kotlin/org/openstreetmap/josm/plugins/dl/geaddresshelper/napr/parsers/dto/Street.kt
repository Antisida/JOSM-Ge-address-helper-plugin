package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlag

data class Street(
    val source: String,
    val extractedName: String,
//    val extractedType: StreetType?,
    val flags: MutableList<ParsingFlag>,
    val isSuccess: Boolean
)