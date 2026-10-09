package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.Transliterator

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
            TagType.STREET -> {
                requireNotNull(address) { "ParsedAddress cannot be null while creating street's tags" }
                forStreet(rawNaprString, address, additionalTags)
            }
        }
    }

    private fun forStreet(
        rawString: List<String>,
        address: Address,
        additionalTags: Map<String, String>
    ): Map<String, String> {
        val tags = mutableMapOf<String, String>()
        tags.putAll(toTagsIndexed("napr:pl", address.places.map { place -> place.name }))
        tags.putAll(toTagsIndexed("napr:pl:tr", address.places.map { place -> Transliterator.transliterate(place.getStatusWithName()) }))
        tags.put("name", address.street.extractedName)

        tags.put("napr:addr", address.source)
        tags.putAll(toTagsIndexed("napr:raw", rawString))

        tags.putAll(additionalTags)
        return tags
    }

    private fun forNode(
        rawString: List<String>,
        address: Address?,
        additionalTags: Map<String, String>,
    ): Map<String, String> = buildMap {
        putAll(forNode(rawString))

        if (address != null) {
            putAll(toTagsIndexed("napr:pl", address.places.map { place -> place.name + " : " + Transliterator.transliterate(place.getStatusWithName()) }))
//            putAll(toTagsIndexed("napr:pl:tr", address.places.map { place -> Transliterator.transliterate(place.getStatusWithName()) }))
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
        putAll(toTagsIndexed("napr:raw", rawString))
    }

    private fun forBuilding(
        osmStreetName: String,
        address: Address,
        rawString: List<String>,
        additionalTags: Map<String, String>,
    ): MutableMap<String, String> {
        val tags = mutableMapOf<String, String>()
        tags.putAll(toTagsIndexed("napr:pl", address.places.map { place -> place.name + " : " + Transliterator.transliterate(place.getStatusWithName()) }))
//        tags.putAll(toTagsIndexed("napr:pl:tr", address.places.map { place -> Transliterator.transliterate(place.getStatusWithName()) }))
        tags.put("addr:street", osmStreetName)
        tags.put("addr:housenumber", address.houseNumber.extractedNumber)

        tags.put("napr:addr", address.source)
        tags.putAll(toTagsIndexed("napr:raw", rawString))

        tags.putAll(additionalTags)
        return tags
    }

    private fun toTagsIndexed(tagTemplate: String, strings: List<String>): Map<String, String> = buildMap {
        strings.distinct().forEachIndexed { index, string ->
            put("$tagTemplate:${index + 1}", string)
        }
    }

    enum class TagType {
        NODE,
        BUILDING,
        STREET
    }
}