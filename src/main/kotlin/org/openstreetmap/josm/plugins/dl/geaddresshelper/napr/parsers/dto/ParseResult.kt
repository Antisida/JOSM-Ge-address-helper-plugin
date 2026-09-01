package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.TagMap
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ActionType

data class ParseResult(
    val eastNorth: EastNorth,
    val osmPrimitive: OsmPrimitive,
    val parsedAddressList: List<Address>,
    val rawNaprStrings: List<String>,
    val matchStreet: String?,
    var resultAction: ActionType? = null,
) {

    fun getTags(): TagMap {
        require(parsedAddressList.size == 1) { "Not applicable" }
        return TagMap(
            "addr:housenumber", parsedAddressList.first().houseNumber.extractedNumber,
            "addr:street", matchStreet,
        )
    }
}