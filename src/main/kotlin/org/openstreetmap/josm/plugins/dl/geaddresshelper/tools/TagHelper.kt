package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.ADDR_HOUSENUMBER_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.ADDR_STREET_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.TagSettingsReader

class TagHelper {
    companion object {
        fun isOverwriteEnabled(key: String, oldValue: String): Boolean {
            val forceAddressOverwrite = TagSettingsReader.OVERWRITE_ADDRESS.get()
            return when (key) {
                "building" -> if (oldValue == "yes") return true else false
                ADDR_HOUSENUMBER_TAG -> if (forceAddressOverwrite) return true else false
                ADDR_STREET_TAG -> if (forceAddressOverwrite) return true else false
                "addr:place" -> if (forceAddressOverwrite) return true else false
                else -> {
                    false
                }
            }
        }

    }
}