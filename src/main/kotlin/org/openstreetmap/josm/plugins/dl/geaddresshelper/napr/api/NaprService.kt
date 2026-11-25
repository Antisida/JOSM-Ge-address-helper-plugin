package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.tools.Logging
import kotlin.random.Random

class NaprService(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) {
    private val semaphore = Semaphore(10)
    suspend fun fetchData(
        centerToBuildings: List<Pair<EastNorth, OsmPrimitive>>
    ): List<Triple<EastNorth, OsmPrimitive, RawNaprDto>> =
        withContext(ioDispatcher) {
            centerToBuildings.map { (coordinate, primitive) ->
                async {
                    val response = retryWhen(
                        policy = retryPolicy,
                        shouldRetryException = { ex -> isTransientNetworkError(ex) },
                        shouldRetryResult = { dto -> isRetryableDto(dto) }
                    ) {
                        semaphore.withPermit {
                            NaprClient.executeRequest(coordinate)
                        }
                    }
                    response?.let { Triple(coordinate, primitive, it) }
                }
            }.awaitAll().filterNotNull()
        }

    private fun isTransientNetworkError(ex: Throwable): Boolean =
        ex is java.io.IOException

    private fun isRetryableDto(dto: RawNaprDto?): Boolean {
        // Пример условия по ответу, если DTO умеет сообщать о временной ошибке/лимите:
        // return dto?.isTemporaryFailure == true
        return false
    }


    data class RetryPolicy(
        val maxRetries: Int = 3,
        val initialDelayMs: Long = 100L,
        val maxDelayMs: Long = 500L,
        val factor: Double = 2.0
    )

    private suspend fun <T> retryWhen(
        policy: RetryPolicy = RetryPolicy(),
        shouldRetryException: (Throwable) -> Boolean = { it !is CancellationException },
        shouldRetryResult: (T?) -> Boolean = { false },
        requestFunction: suspend () -> T?
    ): T? {
        var attempt = 0
        var currentDelay = policy.initialDelayMs

        while (true) {
            try {
                val result = requestFunction()
                //если надо проверить результат
//                if (attempt < policy.maxRetries && shouldRetryResult(result)) {
//                    attempt++
//                    delay(withJitter(currentDelay))
//                    currentDelay = (currentDelay * policy.factor).toLong().coerceAtMost(policy.maxDelayMs)
//                    continue
//                }
                return result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (attempt >= policy.maxRetries || !shouldRetryException(e)) {
                    //после всех неуспешных ретраев
                    null
                    //throw e
                }
                attempt++
                delay(withJitter(currentDelay))
                currentDelay = (currentDelay * policy.factor).toLong().coerceAtMost(policy.maxDelayMs)
                Logging.debug("to retry: $attempt")
            }
        }
    }

    private fun withJitter(baseDelayMs: Long): Long {
        val jitter = (baseDelayMs * 0.2 * Random.nextDouble()).toLong()
        return baseDelayMs + jitter
    }
}
