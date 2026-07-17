package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.NSPDLayer
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.OSMAddress
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsedHouseNumber
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsedPlace
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsedStreet

//todo сюда парсить
data class N_ParsedAddress(
//    val parsedPlace: ParsedPlace,
    val naprFullString: String,
    val parsedStreet: N_ParsedStreet,
    val parsedHouseNumber: ParsedHouseNumber,
//    val egrnAddress: String,
    val flags: MutableList<ParsingFlags>,
    val tags: MutableMap<String, String>,
    val isSuccess: Boolean
//    var layer: NSPDLayer? = null
) {
    fun getAllFlags(): List<ParsingFlags> {
        return (flags + parsedStreet.flags + parsedHouseNumber.flags).distinct()
    }

    fun getValidatedFlags(): List<ParsingFlags> {
        return (flags + parsedStreet.flags + parsedHouseNumber.flags)
            .distinct()
            .filter { flag -> flag == ParsingFlags.STREET_NAME_FUZZY_MATCH }
    }
//    fun getOsmAddress(): OSMAddress {
//        return OSMAddress(parsedPlace.name, parsedStreet.mostRelevantOsmName, parsedHouseNumber.houseNumber, parsedHouseNumber.flats)
//    }

    fun isBuildingAddress():Boolean {
        return flags.contains(ParsingFlags.IS_BUILDING)
    }

//    fun isMatchedByStreetOrPlace():Boolean {
//        return isMatchedByStreet() || isMatchedByPlace()
//    }

//    fun isMatchedByStreet() : Boolean {
//        val osmAddress = getOsmAddress()
//        return osmAddress.isFilledStreetAddress() //|| flags.contains(ParsingFlags.CANNOT_FIND_STREET_OBJECT_IN_OSM)
//    }
//
//    fun isMatchedByPlace() : Boolean {
//        val osmAddress = getOsmAddress()
//        return osmAddress.isFilledPlaceAddress() && !flags.contains(ParsingFlags.CANNOT_FIND_STREET_OBJECT_IN_OSM)
//    }

}
