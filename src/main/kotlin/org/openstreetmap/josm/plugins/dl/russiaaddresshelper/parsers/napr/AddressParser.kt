package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.success
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.NaprBody
import org.openstreetmap.josm.tools.Logging
import kotlin.text.contains


/**
 * Логика завязана на наличие 'N'. По опыту все номера домов ей предваряются.
 * Если ее нет вычленение номера дома и улицы не происходит.
 */
fun splitAddress(fullNaprAddress: String): List<String> {
    // Ищет букву N, если перед ней (через любые пробелы) НЕТ запятой и вставляет запятую
    val formattedAddress = fullNaprAddress.replace(Regex("([^,\\s])\\s*N"), "$1, N")
    return formattedAddress
        .split(",")
        .takeLast(2)
        .map { it.trim() }
}

fun parseHouseNumber(str: String): String {
    return str.replace("N", "")
        .replace(" ", "")
        // 1. Удаляем не-буквы и не-цифры в самом начале строки
        .replace(Regex("^[^\\p{L}\\p{N}]+"), "")
        // 2. Удаляем не-буквы и не-цифры в самом конце строки
        .replace(Regex("[^\\p{L}\\p{N}]+$"), "")
}

fun parseStreet(str: String): String {
    return str.trim()
}

fun parseResponse(naprBody: NaprBody): MutableMap<String, String> {
    val tags: MutableMap<String, String> = mutableMapOf()
    Logging.info("$naprBody")
    // Собираем все строки в один плоский список
    val allAddrStrings = naprBody.result
        ?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
    val fullAddr = allAddrStrings
        .let { allStrings ->
            // План А: Ищем самую длинную с буквой "N".
            allStrings.filter { it.contains("N") }.maxByOrNull { it.length }
            // План Б: Если План А вернул null, берем самую длинную из всех
                ?: allStrings.maxByOrNull { it.length }
        }
    if (fullAddr == null) {
        Logging.info("Empty response. Body: $naprBody")
    } else {
        tags.putAll(mapOf("addr:GE:napr" to fullAddr))
        if (fullAddr.contains("N")) {
            val split = splitAddress(fullAddr)
            if (split.size == 2) {
                val streetValue = parseStreet(split[0])
                val houseNumberValue = parseHouseNumber(split[1])
                tags.putAll(
                    mapOf(
                        "addr:street" to streetValue,
                        "addr:housenumber" to houseNumberValue
                    )
                )
            } else {
                //Logging.error("size < 2")
            }
        }
    }
    return tags
}

fun processResponse(result: Result<NaprBody, FuelError>): MutableMap<String, String> {
    val defaultTagsForNode: Map<String, String> = mapOf("source:addr" to "napr.gov.ge", "fixme" to "REMOVE ME!")
    val tagsForNode: MutableMap<String, String> = mutableMapOf()
    result.success { naprBody ->
        Logging.info("$naprBody")
        // Собираем все строки в один плоский список
        val allAddrStrings = naprBody.result
            ?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val fullAddr = allAddrStrings
            .let { allStrings ->
                // План А: Ищем самую длинную с буквой "N".
                allStrings.filter { it.contains("N") }.maxByOrNull { it.length }
                // План Б: Если План А вернул null, берем самую длинную из всех
                    ?: allStrings.maxByOrNull { it.length }
            }
        if (fullAddr == null) {
            Logging.info("Empty response. Body: $naprBody")
        } else {
            // Проверяем: если адрес нашли, но в нем нет "N" — взводим флаг warn
            tagsForNode.putAll(defaultTagsForNode)
            tagsForNode.putAll(mapOf("addr:GE:napr" to fullAddr))
            if (fullAddr.contains("N")) {
                val split = splitAddress(fullAddr)
                if (split.size != 2) {
                    return@success
                }
                val streetValue = parseStreet(split[0])
                val houseNumberValue = parseHouseNumber(split[1])
                tagsForNode.putAll(
                    mapOf(
                        "addr:street" to streetValue, "addr:housenumber" to houseNumberValue
                    )
                )
            }
        }
    }
    return tagsForNode
}

