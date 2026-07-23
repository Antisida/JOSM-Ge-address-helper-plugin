package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools

import org.openstreetmap.josm.plugins.dl.geaddresshelper.GeAddressHelperPlugin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class FileHelper {
    companion object {
        fun getCurrentExportFilename() :String {
            val formatter = DateTimeFormatter.ofPattern("YYYY_MM_dd")
            val date = LocalDateTime.now()
            return "addressExport_${GeAddressHelperPlugin.versionInfo}_${formatter.format(date)}.csv"
        }
    }
}