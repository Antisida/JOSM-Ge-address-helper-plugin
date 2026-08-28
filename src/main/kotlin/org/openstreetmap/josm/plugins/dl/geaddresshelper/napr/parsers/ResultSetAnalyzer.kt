package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.TagMap
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.ParseResult
import org.openstreetmap.josm.tools.Geometry
import org.openstreetmap.josm.tools.Geometry.getDistance
import kotlin.collections.iterator

class ResultSetAnalyzer {

  private val addressRegistry: MutableMap<TagMap, MutableSet<OsmPrimitive>> = mutableMapOf()

  constructor(addrBuildings: List<OsmPrimitive>, results: List<ParseResult>) {
    tagsToPrimitives1(addrBuildings).forEach { (tagMap, primitives) ->
      this.addressRegistry.merge(tagMap, primitives.toMutableSet()) { existingSet, newSet ->
        existingSet.apply { addAll(newSet) }
      }
    }
    tagsToPrimitives2(results).forEach { (tagMap, primitives) ->
      this.addressRegistry.merge(tagMap, primitives.toMutableSet()) { existingSet, newSet ->
        existingSet.apply { addAll(newSet) }
      }
    }
  }

  private fun isDouble(tags: TagMap, osmPrimitive: OsmPrimitive): Boolean {
    val buildings = addressRegistry[tags]
    if (buildings == null) return false
    val maxAreaBuilding =
      buildings.filter { getDistance(osmPrimitive, it) < 100 }.maxBy { Geometry.computeArea(it) }
    return maxAreaBuilding != osmPrimitive
  }

  private fun tagsToPrimitives1(
    buildings: List<OsmPrimitive>
  ): MutableMap<TagMap, MutableSet<OsmPrimitive>> {
    return buildings
      .groupBy { getAddrTags(it.keys) }
      .mapValues { it.value.toMutableSet() }
      .toMutableMap()
  }

  private fun tagsToPrimitives2(
    results: List<ParseResult>
  ): MutableMap<TagMap, MutableSet<OsmPrimitive>> {
    return results
      .groupBy { it.getTags() }
      .mapValues { entry ->
        entry.value.map { it.osmPrimitive }.toMutableSet()
      }
      .toMutableMap()
  }

  private fun getAddrTags(tagMap: TagMap): TagMap {
    val addrTags = tagMap.filterKeys { key ->
      key == "addr:street" || key == "addr:housenumber"
    }
    val result = TagMap()
    if (addrTags.isEmpty()) return result
    for (addr in addrTags) {
      result.put(addr.key, addr.value)
    }
    //    Logging.info("getAddrTags res: $result")
    return result
  }

  fun defineAction(result: ParseResult): ActionType {
    if (result.parsedAddressList.size != 1) return ActionType.CREATE_MULTIPLE_ADDR_NODE

    return when {
      result.matchStreet == null -> ActionType.CREATE_NODE
      isDouble(result.getTags(), result.osmPrimitive) -> ActionType.CREATE_NODE_DOUBLE
      else -> ActionType.ADD_ADDR_TAGS
    }
  }
}
