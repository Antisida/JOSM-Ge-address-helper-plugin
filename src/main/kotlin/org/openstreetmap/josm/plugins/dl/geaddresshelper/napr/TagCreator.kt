package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ActionType
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ActionType.*
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.Transliterator

object TagCreator {
    const val BUILDING_TAG = "building"
    const val ADDR_STREET_TAG = "addr:street"
    const val ADDR_HOUSENUMBER_TAG = "addr:housenumber"

    const val REMOVE_ME = "REMOVE ME!"
    const val FIXME_TAG = "fixme"

    const val NAPR_PL_TAG = "napr:pl"
    const val NAPR_RAW_TAG = "napr:raw"
    const val NAPR_ADDR_TAG = "napr:addr"
    const val NAPR_WARN_TAG = "napr:warn"

    const val NAME_TAG = "name"
    const val NAME_KA_TAG = "name:ka"
    const val NAME_EN_TAG = "name:en"
    const val NAME_RU_TAG = "name:ru"


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


    fun forStreetFunction(): (List<String>, Address?, Map<String, String>) -> Map<String, String> {
        return { rawString, address, additionalTags ->
            val tags = mutableMapOf<String, String>()
            if (address != null) {
                tags.putAll(
                    toTagsIndexed(
                        NAPR_PL_TAG,
                        address.places.map { place -> place.name + " : " + Transliterator.transliterate(place.getStatusWithName()) })
                )
//              tags.putAll(toTagsIndexed("napr:pl:tr", address.places.map { place -> Transliterator.transliterate(place.getStatusWithName()) }))
                tags.put("name", address.street.extractedName)

                tags.put(NAPR_ADDR_TAG, address.source)
            }
            tags.putAll(toTagsIndexed(NAPR_RAW_TAG, rawString))
            tags.putAll(additionalTags)
            tags
        }
    }

    fun forNodeFunction(): (List<String>, Address?, Map<String, String>) -> Map<String, String> {
        return { rawString, address, additionalTags ->
            buildMap {
                putAll(forNode(rawString))

                if (address != null) {
                    putAll(
                        toTagsIndexed(
                            NAPR_PL_TAG,
                            address.places.map { place -> place.name + " : " + Transliterator.transliterate(place.getStatusWithName()) })
                    )
//                  putAll(toTagsIndexed("napr:pl:tr", address.places.map { place -> Transliterator.transliterate(place.getStatusWithName()) }))
                    put(ADDR_STREET_TAG, address.street.extractedName)
                    put(ADDR_HOUSENUMBER_TAG, address.houseNumber.extractedNumber)

                    if (address.street.flags.contains(ParsingFlags.GENITIVE_APPLIED)) {
                        put(NAPR_WARN_TAG, "TO GENITIVE CASE")
                    }
                }
                putAll(additionalTags)
            }
        }
    }

    private fun forNode(rawString: List<String>): Map<String, String> = buildMap {
        put(FIXME_TAG, REMOVE_ME)
        putAll(toTagsIndexed(NAPR_RAW_TAG, rawString))
    }

    fun forBuildingFunction(): (List<String>, Address?, Map<String, String>) -> Map<String, String> {
        return { rawString, address, additionalTags ->
            val tags = mutableMapOf<String, String>()
            requireNotNull(address) { "ParsedAddress cannot be null while creating building's tags" }
            tags.putAll(
                toTagsIndexed(
                    NAPR_PL_TAG,
                    address.places.map { place -> place.name + " : " + Transliterator.transliterate(place.getStatusWithName()) })
            )
//          tags.putAll(toTagsIndexed("napr:pl:tr", address.places.map { place -> Transliterator.transliterate(place.getStatusWithName()) }))
            tags.put("addr:housenumber", address.houseNumber.extractedNumber)

            tags.put(NAPR_ADDR_TAG, address.source)
            tags.putAll(toTagsIndexed(NAPR_RAW_TAG, rawString))

            tags.putAll(additionalTags)
            tags
        }
    }

    private fun toTagsIndexed(tagTemplate: String, strings: List<String>): Map<String, String> = buildMap {
        strings.distinct().forEachIndexed { index, string ->
            put("$tagTemplate:${index + 1}", string)
        }
    }

    fun getTegCreationFunction(resultAction: ActionType): (List<String>, Address?, Map<String, String>) -> Map<String, String> {
        return when (resultAction) {
            CREATE_NODE, CREATE_NODE_DOUBLE, CREATE_MULTIPLE_ADDR_NODE -> forNodeFunction()
            ADD_ADDR_TAGS -> forBuildingFunction()
        }
    }

}