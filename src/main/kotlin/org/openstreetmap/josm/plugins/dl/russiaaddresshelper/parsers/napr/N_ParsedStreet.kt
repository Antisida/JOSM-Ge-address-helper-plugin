package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import com.github.kittinunf.result.Result
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.StreetType
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.StreetTypes
import org.openstreetmap.josm.tools.Logging

data class N_ParsedStreet(
    val naprStreetName: String,
//    val mostRelevantOsmName: String, //наиболее подходящая из слоя
    val extractedName: String,
//    val extractedType: StreetType?,
//    val matchingPrimitives: List<OsmPrimitive>,
    val flags: MutableList<ParsingFlags>,
    val isSuccess: Boolean

) {

}