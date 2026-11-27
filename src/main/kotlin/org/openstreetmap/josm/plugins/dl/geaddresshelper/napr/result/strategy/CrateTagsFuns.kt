package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy

import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.ADDR_HOUSENUMBER_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.ADDR_STREET_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.FIXME_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAPR_ADDR_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAPR_PL_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAPR_RAW_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAPR_WARN_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.REMOVE_ME
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlag
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.Transliterator

val forBuildingFunction: (List<String>, Address?, Map<String, String>) -> Map<String, String> =
    { rawString, address, additionalTags ->
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

val forNodeFunction: (List<String>, Address?, Map<String, String>) -> Map<String, String> =
    { rawString, address, additionalTags ->
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

                if (address.street.flags.contains(ParsingFlag.GENITIVE_APPLIED)) {
                    put(NAPR_WARN_TAG, "TO GENITIVE CASE")
                }
            }
            putAll(additionalTags)
        }
    }

val forStreetFunction: (List<String>, Address?, Map<String, String>) -> Map<String, String> =
    { rawString, address, additionalTags ->
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


fun toTagsIndexed(tagTemplate: String, strings: List<String>): Map<String, String> = buildMap {
    strings.distinct().forEachIndexed { index, string ->
        put("$tagTemplate:${index + 1}", string)
    }
}

fun forNode(rawString: List<String>): Map<String, String> = buildMap {
    put(FIXME_TAG, REMOVE_ME)
    putAll(toTagsIndexed(NAPR_RAW_TAG, rawString))
}