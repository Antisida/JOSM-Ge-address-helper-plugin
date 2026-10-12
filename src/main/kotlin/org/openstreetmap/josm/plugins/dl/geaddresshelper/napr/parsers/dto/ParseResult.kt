package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.TagMap
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.ADDR_HOUSENUMBER_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.ADDR_STREET_TAG

data class ParseResult(
    val eastNorth: EastNorth,
    val osmPrimitive: OsmPrimitive,
    val parsedAddressList: List<Address>,
    val rawNaprStrings: List<String>,
    val matchStreet: String?
) {

    fun getAddrTags(): TagMap {
        require(parsedAddressList.size == 1) { "Not applicable" }
        return TagMap(
            ADDR_HOUSENUMBER_TAG, parsedAddressList.first().houseNumber.extractedNumber,
            ADDR_STREET_TAG, matchStreet,
        )
    }
}