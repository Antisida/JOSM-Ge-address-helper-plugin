//package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.validation
//
//import org.openstreetmap.josm.data.coor.EastNorth
//import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.NSPDResponse
//import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.N_ParsedAddresses
//import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
//
//data class ValidationRecord(
//    val data: NSPDResponse,
//    val coordinate: EastNorth?,
//    val addressInfo: N_ParsedAddresses?,
//    val flags: ParsingFlags,
//    val ignored: MutableSet<EGRNTestCode> = mutableSetOf(),
//    val processed: MutableSet<EGRNTestCode> = mutableSetOf()
//) {
//    fun ignore (code : EGRNTestCode) {
//        ignored.add(code)
//    }
//
//    fun ignoreAll() {
//        ignored.addAll(EGRNTestCode.values())
//    }
//
//    fun isIgnored (code : EGRNTestCode) : Boolean{
//        return ignored.contains(code)
//    }
//
//    fun isIgnored () : Boolean{
//        return ignored.isNotEmpty()
//    }
//
//    fun process (code : EGRNTestCode) {
//        processed.add(code)
//    }
//
//    fun isProcessed() :Boolean {
//        return processed.isNotEmpty()
//    }
//
//    fun isProcessed(code : EGRNTestCode) :Boolean {
//        return processed.contains(code)
//    }
//
//
//}
