package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address

object TagCreator {
    const val REMOVE_ME = "REMOVE ME!"
    const val FIXME_TAG = "fixme"
    const val BUILDING_TAG = "building"
    val STREET_STATUS_AND_ABBR_SET =
        setOf(
            "ქუჩა",
            "ქ.",
            "გამზირი",
            "გამზ.",
            "ბულვარი",
            "ჩიხი",
            "შესახვევი",
            "შეს.",
            "გასასვლელი",
            "აღმართი",
            "გზატკეცილი"
        )
    val STATUSES_SET =
        setOf(
            "ქუჩა", // улица
            "გამზირი", // проспект
            "ბულვარი", // Бульвар
            "ჩიხი", // тупик
            "შესახვევი", // переулок
            "გასასვლელი", // съезд
            "აღმართი", // склон, подъем, спуск
            "ხევი", // овраг
            "გზატკეცილი" //шоссе
        )

    val PLACE_SET =
        setOf(
            "სოფელი",  //деревня, село
            "ქალაქი",   //город
        )

    //  "მუნიციპალიტეტი", //муниципалитет


    fun create(
        type: TagType,
        osmStreet: String?,
        address: Address?,
        rawNaprString: List<String>,
        additionalTags: Map<String, String>,
    ): Map<String, String> {
        return when (type) {
            TagType.NODE -> forNode(rawNaprString, address, additionalTags)
            TagType.BUILDING -> {
                requireNotNull(address) { "ParsedAddress cannot be null while creating building's tags" }
                requireNotNull(osmStreet) { "OsmStreetName cannot be null while creating building's tags" }
                forBuilding(osmStreet, address, rawNaprString, additionalTags)
            }
        }
    }

    private fun forNode(
        rawString: List<String>,
        address: Address?,
        additionalTags: Map<String, String>,
    ): Map<String, String> = buildMap {
        putAll(forNode(rawString))

        if (address != null) {
            // 2. Добавляем фиксированные теги
            put("napr:pl", address.place.extractedName)
            put("addr:street", address.street.extractedName)
            put("addr:housenumber", address.houseNumber.extractedNumber)

            if (address.street.flags.contains(ParsingFlags.GENITIVE_APPLIED)) {
                put("napr:warn", "TO GENITIVE CASE")
            }
        }
        putAll(additionalTags)
    }

    private fun forNode(rawString: List<String>): Map<String, String> = buildMap {
        put("fixme", "REMOVE ME!")
        putAll(toRawTags(rawString))
    }

    private fun forBuilding(
        osmStreetName: String,
        address: Address,
        rawString: List<String>,
        additionalTags: Map<String, String>,
    ): MutableMap<String, String> {
        val tags = mutableMapOf<String, String>()
        tags.put("napr:pl", address.place.extractedName)
        tags.put("addr:street", osmStreetName)
        tags.put("addr:housenumber", address.houseNumber.extractedNumber)

        tags.put("napr:addr", address.source)
        tags.putAll(toRawTags(rawString))

        tags.putAll(additionalTags)
        return tags
    }

    private fun toRawTags(rawString: List<String>): Map<String, String> = buildMap {
        // 1. Наполняем мапу сырыми адресами
        rawString.distinct().forEachIndexed { index, string ->
            put("napr:raw:${index + 1}", string)
        }
    }

    enum class TagType {
        NODE,
        BUILDING,
    }
}