package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.api

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.STREET_STATUS_AND_ABBR_SET

data class RawNaprDto(
    var result: List<NaprResult>? = null,
) {
    fun isUseful(): Boolean {
        return result?.any {
            it.descript.orEmpty().isNotEmpty() ||
                    it.resulttext.orEmpty().isNotEmpty() ||
                    it.name.orEmpty().isNotEmpty()
        } ?: false
    }

    fun getUsefulString(): List<String> {
        val allAddrStrings =
            result
                ?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
                ?.filter { it.isNotEmpty() }
//                ?.filter { line -> "N" in line } // todo не теряем ли мы номера без N // это мешает парсить улицы, когда нет номера дома
                ?.filter { line -> STREET_STATUS_AND_ABBR_SET.any { status -> status in line } }
                ?: emptyList()
        return allAddrStrings
    }

    fun getDataString(): List<String> {
        val allAddrStrings =
            result?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
                ?.filter { line -> line.any { it.isLetter() } } //удаляем строки типа "22.28.05.054"
                ?: emptyList()
        return allAddrStrings
    }
}

@SuppressWarnings("kotlin:S117")
data class NaprResult(
    var id: Int? = null, //  10616182
    var name: String? = null, //  "20.42.09.560"
    var descript: String? = null, //  "ქობულეთის მუნიციპალიტეტი, ქალაქი ქობულეთი, გიორგი ლეონიძის შესახვევი N 8"
    var resulttext: String? = null, //  "ქობულეთის მუნიციპალიტეტი, ქალაქი ქობულეთი, გიორგი ლეონიძის შესახვევი N 8"
    var resultlink: String? = null, //  "/map/portal/getbylbl?lbl=lr_parcels:AAArWyACtAAAnd2AAQ"
    var details: NaprDetails? = null,
    var geo_coords: String? = null, //  ""
    //    val layers: List<>,           //  ?
    var sdo_gtype: String? = null, //  "2003"
    var dist: String? = null, //  "0"
    var rank: String? = null, //  "0"
    var sdo_srid: String? = null, //  "4326"
    var shape_wkt: String? = null, //  ""
    var env: String? = null, //  "alpha"
)

@SuppressWarnings("kotlin:S117")
data class NaprDetails(
    var type: String? = null, //  "address"
    var mode: String? = null, //  "absolute"
    var info_link: String? = null, //  "/lr/bo/mg/getinfo.alpha?lbl=lr_parcels:AAArWyACtAAAnd2AAQ"
    var geometry_link: String? = null, //  "/lr/bo/mg/getinfo.alpha?lbl=lr_parcels:AAArWyACtAAAnd2AAQ&res=shp"
)
