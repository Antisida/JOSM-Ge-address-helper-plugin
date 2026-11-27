package org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io

import org.openstreetmap.josm.data.preferences.StringProperty

class NaprSettingsReader {
    companion object {
        /**
         * Property for current EGRN server.
         * @since 0.0.1
         */
        val NAPR_URL_REQUEST = StringProperty(
            "dl.geaddresshelper.napr.url",
            //todo
//            "https://maps.gov.ge/map/portal/search"
            "https://maps.gov.ge"
        )



        val NAPR_REQUEST_USER_AGENT = StringProperty(//todo
            "dl.geaddresshelper.ppk.useragent",
            "JOSM/%s JOSM-geaddresshelper/%s"
        )


    }
}