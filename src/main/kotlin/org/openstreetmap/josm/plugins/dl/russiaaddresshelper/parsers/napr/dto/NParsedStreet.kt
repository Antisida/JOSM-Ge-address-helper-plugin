package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.dto

import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.StreetType
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.StreetTypes
import org.openstreetmap.josm.tools.Logging

class NParsedStreet(
    val name: String,
    val extractedName: String,
    val extractedType: StreetType?,
    val matchedPrimitives: List<OsmPrimitive>,
    val flags: MutableList<ParsingFlags>
) {

    private fun getOsmObjectsByType(streetType: StreetType): Set<OsmPrimitive> {

        val primitives = OsmDataManager.getInstance().editDataSet.allNonDeletedCompletePrimitives().filter { p ->
            p.hasKey("highway") && p.hasKey("name") && p.type.equals(OsmPrimitiveType.WAY)
        }
        return primitives.filter { p ->
            streetType.osm.asRegExpList().any { p["name"].matches(it) || p["egrn_name"].matches(it) }
        }.toSet()
    }

    fun getMatchingPrimitives(): Set<OsmPrimitive> {
        if (extractedType == null || extractedName.isEmpty() || name.isEmpty()) {
            return emptySet()
        }
        return getOsmObjectsByType(extractedType).filter { name == it["name"] || name == it["egrn_name"] }
            .toSet()
    }

    fun removeEndingWith(address: String): String {
        var matchRange = getMatchRange(address)
        val matchEndIndex = matchRange.last
        return address.slice(matchEndIndex+1 until address.length)
    }

    fun getMatchRange (address: String): IntRange {
        if (extractedType == null) return IntRange.EMPTY
        val matchedPattern = extractedType.egrn.asRegExpList().find { it.containsMatchIn(address) }
        if (matchedPattern == null) {
            Logging.error("EGRN PLUGIN RemoveEndingWith - somehow matched street type ${extractedType.name} doesnt match $address")
            return IntRange.EMPTY
        }
        return matchedPattern.findAll(address).last().groups["street"]!!.range
    }

}