package org.openstreetmap.josm.plugins.dl.geaddresshelper.api

import kotlinx.serialization.json.Json
import org.openstreetmap.josm.data.Version
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.plugins.dl.geaddresshelper.GeAddressHelperPlugin.Companion.versionInfo
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.EgrnSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.NaprSettingsReader
import org.openstreetmap.josm.tools.Logging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


object NaprClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(EgrnSettingsReader.REQUEST_TIMEOUT.get()?.toLong() ?: 3000)).build()
    }

    fun executeRequest(coordinate: EastNorth): RawNaprDto? {
        return try {
            request(coordinate)
        } catch (e: Exception) {
            Logging.warn("Exception for $coordinate : $e")
            null
        }
    }

    private fun request(coordinate: EastNorth): RawNaprDto? {
        val response = httpClient.send(buildRequest(coordinate), HttpResponse.BodyHandlers.ofString())
        return decode(response)
    }

    private fun decode(response: HttpResponse<String>): RawNaprDto? {
        if (response.statusCode() != 200) return null
        Logging.info("response.body(): " + response.body().toString())
        return json.decodeFromString<RawNaprDto>(response.body())
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

    private fun buildRequest(coordinate: EastNorth): HttpRequest {
        val (lonStr, latStr) = toLonLatString(coordinate)
        val userAgent = String.format(
            NaprSettingsReader.NAPR_REQUEST_USER_AGENT.get(),
            Version.getInstance().versionString,
            versionInfo
        )
        return HttpRequest.newBuilder()
            .uri(URI.create(NaprSettingsReader.NAPR_URL_REQUEST.get() + "/map/portal/search"))
            .header("Accept", "application/json; charset=UTF-8")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("User-Agent", userAgent)
            .timeout(Duration.ofMillis(EgrnSettingsReader.REQUEST_TIMEOUT.get()?.toLong() ?: 3000))
            .POST(HttpRequest.BodyPublishers.ofString("keyword=$lonStr,$latStr"))
            .build()
    }

}