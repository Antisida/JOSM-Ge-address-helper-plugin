package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api

import kotlinx.serialization.Serializable

@Serializable
data class RawNaprDto(
    val status: Boolean?,
    val result: List<NaprResult>?
) {
    fun isUseful(): Boolean {
        return result?.any {
            it.descript.orEmpty().isNotEmpty()
                    || it.resulttext.orEmpty().isNotEmpty()
                    || it.name.orEmpty().isNotEmpty()
        } ?: false
    }
}

@Serializable
@SuppressWarnings("kotlin:S117")
data class NaprResult(
    val id: Int?,                    //  10616182
    val name: String?,               //  "20.42.09.560"
    val descript: String?,           //  "ქობულეთის მუნიციპალიტეტი, ქალაქი ქობულეთი, გიორგი ლეონიძის შესახვევი N 8"
    val resulttext: String?,         //  "ქობულეთის მუნიციპალიტეტი, ქალაქი ქობულეთი, გიორგი ლეონიძის შესახვევი N 8"
    val resultlink: String?,         //  "/map/portal/getbylbl?lbl=lr_parcels:AAArWyACtAAAnd2AAQ"
    val details: NaprDetails?,
    val geo_coords: String?,         //  ""
//    val layers: List<>,           //  ?
    val sdo_gtype: String?,          //  "2003"
    val dist: String?,               //  "0"
    val rank: String?,               //  "0"
    val sdo_srid: String?,           //  "4326"
    val shape_wkt: String?,          //  ""
    val env: String?                 //  "alpha"
)

@Serializable
@SuppressWarnings("kotlin:S117")
data class NaprDetails(
    val type: String?,               //  "address"
    val mode: String?,               //  "absolute"
    val info_link: String?,          //  "/lr/bo/mg/getinfo.alpha?lbl=lr_parcels:AAArWyACtAAAnd2AAQ"
    val geometry_link: String?       //  "/lr/bo/mg/getinfo.alpha?lbl=lr_parcels:AAArWyACtAAAnd2AAQ&res=shp"
)
