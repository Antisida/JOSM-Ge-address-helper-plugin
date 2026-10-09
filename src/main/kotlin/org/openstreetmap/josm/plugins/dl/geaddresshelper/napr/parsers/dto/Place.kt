package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags

data class Place(
    val source: String,
    val status: String,
    val name: String,
    val flags: MutableList<ParsingFlags>,
    val isSuccess: Boolean
) {
    fun getStatusWithName(): String = "$status $name"
}