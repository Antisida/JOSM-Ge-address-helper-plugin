package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.validation

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.Address

data class N_ValidationRecord(
//    val data: NSPDResponse,
    val coordinate: EastNorth, //fixme to del???
    val address: Address,
//    val flags: Set<ParsingFlags>,
    val ignored: MutableSet<EGRNTestCode> = mutableSetOf(), //для исключения элемента из валидации после нажатия ИГНОРИРОВАТЬ
    val processed: MutableSet<EGRNTestCode> = mutableSetOf() //не понятно для чего
) {
    fun ignore (code : EGRNTestCode) {
        ignored.add(code)
    }

    fun ignoreAll() {
        ignored.addAll(EGRNTestCode.values())
    }

    fun isIgnored (code : EGRNTestCode) : Boolean{
        return ignored.contains(code)
    }

    fun isIgnored () : Boolean{
        return ignored.isNotEmpty()
    }

    fun process (code : EGRNTestCode) {
        processed.add(code)
    }

    fun isProcessed() :Boolean {
        return processed.isNotEmpty()
    }

    fun isProcessed(code : EGRNTestCode) :Boolean {
        return processed.contains(code)
    }


}
