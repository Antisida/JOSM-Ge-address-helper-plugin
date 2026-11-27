package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.TagMap
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.ADDR_HOUSENUMBER_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.ADDR_STREET_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlag

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

    fun getValidationFlags() : List<ParsingFlag> {
        return parsedAddressList.map { it.getValidationFlags() }.flatten()
    }

    fun getSingleAddressOrNull(): Address? {
        return if (this.parsedAddressList.size == 1)
            this.parsedAddressList[0]
        else
            null
    }
}