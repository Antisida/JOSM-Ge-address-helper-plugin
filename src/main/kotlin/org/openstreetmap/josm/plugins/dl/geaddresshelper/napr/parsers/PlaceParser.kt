package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Place

object PlaceParser {

    fun parse(sourceString: String?): Place? {
        if (sourceString == null) return null
        return Place(sourceString, sourceString, mutableListOf(), true)
    }
}