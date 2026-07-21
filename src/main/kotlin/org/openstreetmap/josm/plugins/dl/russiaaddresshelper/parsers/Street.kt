package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsingFlags

data class Street(
    val naprStreetName: String,
    val extractedName: String,
//    val extractedType: StreetType?,
    val flags: MutableList<ParsingFlags>,
    val isSuccess: Boolean
)