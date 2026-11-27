package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy

import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlag
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.ParseResult
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper

class AddAddressStrategy : BaseResultStrategy {

    override fun createCommand(dataSet: DataSet, result: ParseResult): List<Command> {
        val tags = createTags(result)
        return CommandHelper.toChangeCommands(tags, result.osmPrimitive)
    }

    override fun executeAdditionally(result: ParseResult) {
        val address: Address? = if (result.parsedAddressList.size == 1) result.parsedAddressList[0] else null
        requireNotNull(address) { "ParsedAddress cannot be null while creating building's tags" }
        if (result.matchStreet != address.street.extractedName) {
            address.street.flags.add(ParsingFlag.STREET_NAME_FUZZY_MATCH)
        }

    }

    override val additionalTagsFunction: (String?) -> Map<String, String> =
        { matchStreet ->
            if (matchStreet != null) mapOf("addr:street" to matchStreet) else emptyMap()
        }

    override val tagCreationFunction: (List<String>, Address?, Map<String, String>) -> Map<String, String> = forBuildingFunction
       /* { rawString, address, additionalTags ->
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
        }*/
}