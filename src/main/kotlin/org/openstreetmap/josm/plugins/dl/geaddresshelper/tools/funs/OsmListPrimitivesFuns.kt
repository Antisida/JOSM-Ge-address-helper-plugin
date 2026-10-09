package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.funs

import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.FIXME_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.REMOVE_ME
import javax.sql.rowset.Predicate
import kotlin.collections.filter

/** @return Список highway */
fun Collection<OsmPrimitive>.getStreets(): List<OsmPrimitive> {
    return this.filter(OsmPrimitive::isStreet)
}

private fun OsmPrimitive.isStreet(): Boolean =
    this.hasKey("highway") && this.type == OsmPrimitiveType.WAY

/** @return Список building */
fun Collection<OsmPrimitive>.getBuildings(): List<OsmPrimitive> {
    return this.filter { p -> p !is Node && p.hasKey("building") }
}

/** @return Временные объекты, удаляемые при загрузке */
fun Collection<OsmPrimitive>.getForDelete(): List<OsmPrimitive> {
    return this
        .filter { it.hasTag(FIXME_TAG, REMOVE_ME) }
        .filter { it.isNew }
}

/** @return Список highway */
fun Collection<OsmPrimitive>.containsStreetsOnly(): Boolean {
    return this.any(OsmPrimitive::isStreet)
}