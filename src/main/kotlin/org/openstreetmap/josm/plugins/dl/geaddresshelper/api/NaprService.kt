package org.openstreetmap.josm.plugins.dl.geaddresshelper.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.OsmPrimitive

class NaprService(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val semaphore = Semaphore(10)
    suspend fun fetchData(centerToBuildings: List<Pair<EastNorth, OsmPrimitive>>): List<Triple<EastNorth, OsmPrimitive, RawNaprDto>> =
        withContext(ioDispatcher) {
            centerToBuildings.map { data ->
                async {
                    semaphore.withPermit {
                        val response = NaprClient.executeRequest(data.first)
                        // Если ответ null, возвращаем null для этой корутины
                        if (response != null) Triple(data.first, data.second, response) else null
                    }
                }
            }.awaitAll().filterNotNull()
        }
}
