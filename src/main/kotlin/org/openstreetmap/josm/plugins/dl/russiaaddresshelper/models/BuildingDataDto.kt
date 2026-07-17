package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.RawNaprDto

data class BuildingDataDto(
    val building: OsmPrimitive,
    val center: EastNorth,
    var naprResponseBody: RawNaprDto? = null
)