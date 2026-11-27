package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlag

data class Place(
    val source: String,
    val status: String,
    val name: String,
    val flags: MutableList<ParsingFlag>,
    val isSuccess: Boolean
) {
    fun getStatusWithName(): String = "$status $name"
}