package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.ADDR_HOUSENUMBER_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.ADDR_STREET_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.BUILDING_TAG

fun Collection<OsmPrimitive>.getBuildings(
  ignoreTags: Map<String, List<String>>
): List<OsmPrimitive> {
  return this.filter {
    it !is Node &&
      it.hasTag(BUILDING_TAG) &&
      ignoreTags.all { (key, values) ->
        values.none { tagValue ->
          it.hasTag(key, tagValue) || (it.hasKey(key) && tagValue == "*")
        }
      }
  }
}

fun Collection<OsmPrimitive>.getAddrBuildings(
  ignoreTags: Map<String, List<String>>
): List<OsmPrimitive> {
  val filter =
    this.filter { it !is Node }
      .filter {
        it.hasTag(BUILDING_TAG) &&
          ignoreTags.all { (key, values) ->
            values.none { tagValue ->
              it.hasTag(key, tagValue) || (it.hasKey(key) && tagValue == "*")
            }
          }
      }
//  val primitive = filter[0]
//  primitive.hasTag(ADDR_STREET_TAG)
//  primitive.hasTag(ADDR_HOUSENUMBER_TAG)
//  primitive.hasTag("building")
  val filter1 = filter.filter { it.hasTag(ADDR_STREET_TAG) && it.hasTag(ADDR_HOUSENUMBER_TAG) }
  return filter1
}
