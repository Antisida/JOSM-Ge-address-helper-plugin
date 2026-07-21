package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools

import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.Address
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsingFlags

object TagCreator {
    const val REMOVE_ME = "REMOVE ME!"
    const val FIXME_TAG = "fixme"
    const val BUILDING_TAG = "building"
    val STATUSES_AND_ABBR_SET =
        setOf("ქუჩა", "ქ.", "გამზირი", "გამზ.", "ბულვარი", "ჩიხი", "შესახვევი", "შეს.", "გასასვლელი", "აღმართი")
    val STATUSES_SET = setOf(
        "ქუჩა", //улица
        "გამზირი", //проспект
        "ბულვარი", //Бульвар
        "ჩიხი", //тупик
        "შესახვევი", //переулок
        "გასასვლელი", //съезд
        "აღმართი", //склон, подъем, спуск
        "ხევი" //овраг
    )

    fun create(
        type: TagType,
        osmStreet: String?,
        address: Address?,
        rawNaprString: List<String>
    ): Map<String, String> {
        return when (type) {
            TagType.NODE -> forNode(rawNaprString, address)
            TagType.BUILDING -> {
                requireNotNull(address) { "ParsedAddress cannot be null while creating building's tags" }
                requireNotNull(osmStreet) { "OsmStreetName cannot be null while creating building's tags" }
                forBuilding(osmStreet, address, rawNaprString)
            }
        }
    }

    private fun forNode(
        rawString: List<String>,
        address: Address?
    ): Map<String, String> = buildMap {
        putAll(forNode(rawString))

        if (address != null) {
            // 2. Добавляем фиксированные теги
            put("addr:street", address.parsedStreet.extractedName)
            put("addr:housenumber", address.parsedHouseNumber.extractedNumber)

            if (address.parsedStreet.flags.contains(ParsingFlags.GENITIVE_APPLIED)) {
                put("warn:1", "to genitive case")
            }
        }
    }

    private fun forNode(rawString: List<String>): Map<String, String> = buildMap {
        put("fixme", "REMOVE ME!")
        putAll(toRawTags(rawString))
    }

    private fun forBuilding(
        osmStreetName: String,
        address: Address,
        rawString: List<String>
    ): MutableMap<String, String> {
        val tags = mutableMapOf<String, String>()
        tags.put("addr:street", osmStreetName)
        tags.put("addr:housenumber", address.parsedHouseNumber.extractedNumber)

        tags.put("napr:addr", address.naprFullString)
        tags.putAll(toRawTags(rawString))
        return tags
    }

    private fun toRawTags(
        rawString: List<String>
    ): Map<String, String> = buildMap {
        // 1. Наполняем мапу сырыми адресами
        rawString.distinct().forEachIndexed { index, string ->
            put("napr:addr:raw:${index + 1}", string)
        }
    }

    enum class TagType { NODE, BUILDING }
}