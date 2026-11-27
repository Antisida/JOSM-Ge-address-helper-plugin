package org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io


import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.model.MapStringListProperty
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.ADDR_HOUSENUMBER_TAG

class MassActionSettingsReader {
    companion object {

        /**
         * Store list of maps of lists of tags to filter out when doing mass request.
         * @since 0.9.4.8
         */
       val EGRN_MASS_ACTION_FILTER_LIST = MapStringListProperty(
            "dl.geaddresshelper.mass.filtering_tag_map",
            mapOf("fixme" to listOf("*"), "building" to listOf("garages","garage","shed","roof","shack","greenhouse"), ADDR_HOUSENUMBER_TAG to listOf("*"))
         )

        val FILTER_LIST_ADDR = MapStringListProperty(
            "dl.geaddresshelper.mass.filtering_tag_map",
            mapOf("fixme" to listOf("*"), "building" to listOf("garage","shed","roof","shack"))
        )

    }
}