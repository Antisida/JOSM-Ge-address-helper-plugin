package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools

import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.TagSettingsReader

class TagHelper {
    companion object {


        fun isOverwriteEnabled(key: String, oldValue: String, value: String): Boolean {
            val forceAddressOverwrite = TagSettingsReader.OVERWRITE_ADDRESS.get()
            return when (key) {
                "building" -> if (oldValue == "yes") return true else false
                "addr:housenumber" -> if (forceAddressOverwrite) return true else false
                "addr:street" -> if (forceAddressOverwrite) return true else false
                "addr:place" -> if (forceAddressOverwrite) return true else false
                else -> {
                    false
                }
            }
        }

    }
}