package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset

import org.openstreetmap.josm.data.APIDataSet
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType

/** @return Список name, alt_name, old_name, short_name всех highway */
fun DataSet.getAllStreetNames(): List<String> {
  return this.allNonDeletedCompletePrimitives()
      .filter { p ->
        p.hasKey("highway") && p.hasKey("name") && p.type == OsmPrimitiveType.WAY
      }
      .flatMap {
        listOfNotNull(it.get("name"), it.get("alt_name"), it.get("old_name"), it.get("short_name"))
      }
}

/** @return Список highway, у которых заполнен тег name */
fun DataSet.getNamedStreets(): List<OsmPrimitive> {
  return this.allNonDeletedCompletePrimitives().filter { p ->
    p.hasKey("highway") && p.hasKey("name") && p.type == OsmPrimitiveType.WAY
  }
}

private fun Collection<OsmPrimitive>.containsTmpPrimitives(): Boolean {
    return this.any {
        (it.hasTag("fixme", "REMOVE_ME!") || it.hasTag("fixme", "REMOVE ME!") ||
                (it.hasTag("fixme", "yes") && it.hasTag("source:addr", "ЕГРН"))) && it.isNew
    }
}

fun DataSet.containsTmpPrimitives(): Boolean {
    return this.allPrimitives().containsTmpPrimitives()
}

fun APIDataSet.containsTmpPrimitives(): Boolean {
    return this.primitivesToAdd.containsTmpPrimitives()
}

fun APIDataSet.containsTmpTags(): Boolean {
    return this.primitives.containsTmpTags()
}

fun DataSet.containsTmpTags(): Boolean {
    return this.allPrimitives().containsTmpTags()
}

private fun Collection<OsmPrimitive>.containsTmpTags(): Boolean {
    return this.stream()
        .flatMap { obj: OsmPrimitive -> obj.keys() }
        .anyMatch { o: String -> TEMP_TAGS.contains(o) }
}

