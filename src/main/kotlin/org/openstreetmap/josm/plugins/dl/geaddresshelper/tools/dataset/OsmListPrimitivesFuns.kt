package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset

import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType

/** @return Список highway, у которых заполнен тег name */
fun MutableCollection<OsmPrimitive>.getStreets(): List<OsmPrimitive> {
    return this.filter { p -> p.hasKey("highway") && p.type == OsmPrimitiveType.WAY }
}

/** @return Список highway, у которых заполнен тег name */
fun MutableCollection<OsmPrimitive>.getBuildings(): List<OsmPrimitive> {
    return this.filter { p -> p !is Node && p.hasKey("building") }
}