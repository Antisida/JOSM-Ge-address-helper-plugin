package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import org.openstreetmap.josm.data.Version
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.io.OsmTransferException
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.RussiaAddressHelperPlugin.Companion.versionInfo
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.EgrnSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.LayerShiftSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.NaprSettingsReader
import org.openstreetmap.josm.tools.Logging
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import kotlin.collections.mapOf

//class NaprApi(/*private val url: String, private val userAgent: String, private val referer: String*/) {
object NaprClient {
    //инициация клиента
    private val fuelClient = FuelManager().apply {
        basePath = NaprSettingsReader.NAPR_URL_REQUEST.get()
        timeoutReadInMillisecond = EgrnSettingsReader.REQUEST_TIMEOUT.get()
        timeoutInMillisecond = EgrnSettingsReader.REQUEST_TIMEOUT.get()
        val userAgent = String.format(
            NaprSettingsReader.NAPR_REQUEST_USER_AGENT.get(),
            Version.getInstance().versionString,
            versionInfo
        )
        baseHeaders = mapOf(
            Headers.ACCEPT to "application/json; charset=UTF-8",
            Headers.CONTENT_TYPE to "application/x-www-form-urlencoded; charset=UTF-8",
            Headers.USER_AGENT to userAgent
        )
    }

    fun executeRequest(coordinate: EastNorth): RawNaprDto? =
        createRequest(coordinate)
            .responseObject<RawNaprDto>(jacksonDeserializerOf())
            .third //Result
            .fold(
                success = { data -> data },
                failure = { error ->
                    Logging.error("Ошибка для координаты $coordinate: ${error.message}")
                    null
                }
            )

    //сделать приватным после рефакторинга клика
    fun createRequest(coordinate: EastNorth/*, layer: NSPDLayer, bbox: BBox?*/): Request {
//        val layerShiftCoordinate = getLayerShift(coordinate)
        val (lonStr, latStr) = toLonLatString(coordinate)
        val formData = listOf("keyword" to "$lonStr,$latStr")
        Logging.info("keyword: $lonStr,$latStr")
        return fuelClient.request(Method.POST, "/map/portal/search", formData)
    }
    // Объединять ქალაქი ზუგდიდი , ქუჩა დ. შენგელაია , შესახვევი 1, N12  - г. Зугдиди, улица Д. Шенгелая, переулок 1, N12
    // დ. შენგელაია -> დემნა შენგელაიას Демна Шенгелаия
// ქალაქი - город
    // მუნიციპალიტეტი - муниципалитет
    // todo это недо убрать заполнять урл при создании фуэл клиента
    // todo заменять сокращения,
    // todo переставлять улицу в конец
    // туду проверять "с" должна быть последней перед улецей
    // удалять все внутри скобок ქალაქი ფოთი, შალვა ამირანაშვილის ქ N 3 (ყოფ. ორჯონიკიძის ქ N 3)
    // туду сокращение წმინდა -> წმ. святой
    // туду შესახვევი პირველი / შესახვევი ''პირველი'' ->  I შესახვევი  первый переулок
    // туду ქალაქი ზუგდიდი , ქუჩა სხულუხია , შესახვევი I , N 21 - улица и переулок через запятую
    private fun makeUrl(url: String/*, lat: String, lon: String*/): URL {
        return try {
            URI(url).toURL()
        } catch (e: MalformedURLException) {
            throw OsmTransferException(e)
        }
    }

    private fun toLonLatString(coordinate: EastNorth): Pair<String, String> {
        val mercator = Projections.getProjectionByCode("EPSG:3857")
        val projected = mercator.eastNorth2latlonClamped(coordinate)
        val formatter = DecimalDegreesCoordinateFormat.INSTANCE
        val lon = formatter.lonToString(projected)
        val lat = formatter.latToString(projected)
//        Logging.info("Request lonlat: $lon,$lat")
        return Pair(lon, lat)
    }


    private fun getLayerShift(coordinate: EastNorth): EastNorth {
//        Logging.info("RequestURL $coordinate")

        val shiftLayerSetting = LayerShiftSettingsReader.LAYER_SHIFT_SOURCE

        val shiftLayer = LayerShiftSettingsReader.getValidShiftLayer(shiftLayerSetting) ?: return coordinate
        val subtract = coordinate.subtract(shiftLayer.displaySettings.displacement)
//        Logging.info("RequestURL $subtract")
        return subtract
    }
}