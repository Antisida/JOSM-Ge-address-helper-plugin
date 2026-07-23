package org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.ParsingFlags

data class Street(
    val naprStreetName: String,
    val extractedName: String,
//    val extractedType: StreetType?,
    val flags: MutableList<ParsingFlags>,
    val isSuccess: Boolean
)