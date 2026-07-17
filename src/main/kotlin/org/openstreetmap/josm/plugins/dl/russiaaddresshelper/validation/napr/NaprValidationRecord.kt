package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.validation.napr

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.N_ParsedAddresses
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.validation.EGRNTestCode
import kotlin.collections.addAll

data class NaprValidationRecord(
//    val data: NSPDResponse, // fixme удалить ???
//    val coordinate: EastNorth?, //fixme почему nullable
    val coordinate: EastNorth, // fixme удалить ???
    val addressInfo: N_ParsedAddresses?, // fixme нужен ли тут список - пока оставлю
    val ignored: MutableSet<EGRNTestCode> = mutableSetOf(), //списки валидаторов
    val processed: MutableSet<EGRNTestCode> = mutableSetOf()
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
