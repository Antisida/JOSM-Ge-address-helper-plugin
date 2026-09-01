package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags

data class Place(
    val source: String,
    val extractedName: String,
    val flags: MutableList<ParsingFlags>,
    val isSuccess: Boolean
)