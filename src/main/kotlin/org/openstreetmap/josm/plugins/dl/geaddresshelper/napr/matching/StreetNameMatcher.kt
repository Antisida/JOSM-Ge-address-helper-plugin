package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching

import org.openstreetmap.josm.data.osm.OsmPrimitive

object StreetNameMatcher {

    fun findBestOrNull(streets: List<OsmPrimitive>, query: String): String? {
        val candidateNames: List<String> = streets
            .map { listOfNotNull(it.get("name"), it.get("alt_name"), it.get("old_name"), it.get("short_name")) }
            .flatten()
        return bestLongerMatchedOrNull(candidateNames, query)
    }


    /**
     * @return: самый подходящий результат (если подходящие есть) из списка candidateNames, если нет подходящих - null.
     * Подходящий должен быть равным или длиннее
     */
    fun bestLongerMatchedOrNull(candidateNames: List<String>, query: String): String? {
        return candidateNames
            .filter { candidate -> !candidate.clean().shorterThen(query.clean()) }
            .mapNotNull { candidate ->
                val match = Matcher(candidate, query).match()
                if (match.isMatched()) candidate to match else null
            }
            .minByOrNull { it.second }?.first
    }

}