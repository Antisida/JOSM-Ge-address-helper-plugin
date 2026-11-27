package org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io

import org.openstreetmap.josm.data.preferences.BooleanProperty

class TagSettingsReader {
    companion object {
        /**
         * @since 0.9.6.4
         * Enables overwrite for housenumber, street and place even if they already exist
         */
        val OVERWRITE_ADDRESS = BooleanProperty("dl.geaddresshelper.tag.force_overwrite_address", true)
    }
}