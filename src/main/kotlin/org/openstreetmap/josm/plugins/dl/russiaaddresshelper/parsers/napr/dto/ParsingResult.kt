package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.dto

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.N_ParsedAddresses
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags

data class ParsingResult(
    val eastNorth: EastNorth,
    val osmPrimitive: OsmPrimitive,
    val bulgingTags: List<Pair<String, String>>,
    val nodeTags: List<Pair<String, String>>,
    val validationFlags: List<ParsingFlags>,
    val parsedAddresses: N_ParsedAddresses
)