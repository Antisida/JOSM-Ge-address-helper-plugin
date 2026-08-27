package org.openstreetmap.josm.plugins.dl.geaddresshelper.api

import com.squareup.moshi.Moshi
import org.openstreetmap.josm.data.Version
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.plugins.dl.geaddresshelper.GeAddressHelperPlugin.Companion.versionInfo
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.EgrnSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.NaprSettingsReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


object NaprClient {

    private val moshi: Moshi by lazy { Moshi.Builder().build() }

    private val httpClient: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(EgrnSettingsReader.REQUEST_TIMEOUT.get()?.toLong() ?: 3000)).build()
    }

    fun executeRequest(coordinate: EastNorth): RawNaprDto? = request(coordinate)

    private fun request(coordinate: EastNorth): RawNaprDto? {
        val (lonStr, latStr) = toLonLatString(coordinate)
        val userAgent = String.format(
            NaprSettingsReader.NAPR_REQUEST_USER_AGENT.get(),
            Version.getInstance().versionString,
            versionInfo
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create(NaprSettingsReader.NAPR_URL_REQUEST.get() + "/map/portal/search"))
            .header("Accept", "application/json; charset=UTF-8")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("User-Agent", userAgent)
            .timeout(Duration.ofMillis(EgrnSettingsReader.REQUEST_TIMEOUT.get()?.toLong() ?: 3000))
            .POST(HttpRequest.BodyPublishers.ofString("keyword=$lonStr,$latStr"))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            return null
        } else {
//            Logging.info("response.body(): " + response.body().toString())
            return moshi.adapter(RawNaprDto::class.java).fromJson(response.body().toString())
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

}