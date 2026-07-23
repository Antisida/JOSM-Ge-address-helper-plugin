package org.openstreetmap.josm.plugins.dl.geaddresshelper.uploadhooks

import org.openstreetmap.josm.actions.upload.UploadHook
import org.openstreetmap.josm.data.APIDataSet
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.plugins.dl.geaddresshelper.GeAddressHelperPlugin
import org.openstreetmap.josm.tools.Logging


class EGRNCleanPluginCache : UploadHook {
    override fun checkUpload(apiDataSet: APIDataSet): Boolean {
        val removedCount = GeAddressHelperPlugin.cache.size()
//        if (CommonSettingsReader.EXPORT_PARSED_DATA_TO_CSV.get()) {
//            val filename = FileHelper.getCurrentExportFilename()
//            geaddresshelperPlugin.cache.exportData(filename)
//        }
        GeAddressHelperPlugin.cache.emptyCache()
        val editLayer = MainApplication.getLayerManager().editLayer
        editLayer?.validationErrors?.clear()

        Logging.info("EGRN-PLUGIN Removed uploaded addresses from plugin validation cache ($removedCount)")

        return true
    }

}