package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.TagMap
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.ADDR_HOUSENUMBER_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.ADDR_STREET_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.ParseResult
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy.AddAddressStrategy
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy.CreateNodeStrategy
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy.DoubleAddrStrategy
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy.ResultStrategy
import org.openstreetmap.josm.tools.Geometry
import org.openstreetmap.josm.tools.Geometry.getDistance
import kotlin.collections.iterator

class ResultSetAnalyzer {

  private val addrCreateStrategy = AddAddressStrategy()
  private val createNodeStrategy = CreateNodeStrategy()
  private val doubleAddrStrategy = DoubleAddrStrategy()

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
      .groupBy { it.getAddrTags() }
      .mapValues { entry ->
        entry.value.map { it.osmPrimitive }.toMutableSet()
      }
      .toMutableMap()
  }

  private fun getAddrTags(tagMap: TagMap): TagMap {
    val addrTags = tagMap.filterKeys { key ->
      key == ADDR_STREET_TAG || key == ADDR_HOUSENUMBER_TAG
    }
    val result = TagMap()
    if (addrTags.isEmpty()) return result
    for (addr in addrTags) {
      result.put(addr.key, addr.value)
    }
    //    Logging.info("getAddrTags res: $result")
    return result
  }
  fun defineStrategy(result: ParseResult): ResultStrategy {
    if (result.parsedAddressList.size != 1) return createNodeStrategy
    return when {
      result.matchStreet == null -> createNodeStrategy
      isDouble(result.getAddrTags(), result.osmPrimitive) -> doubleAddrStrategy
      else -> addrCreateStrategy
    }
  }

}
