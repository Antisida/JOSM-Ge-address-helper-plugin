package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.ParseResult

internal interface BaseResultStrategy : ResultStrategy {
    val additionalTagsFunction: (String?) -> Map<String, String>
    val tagCreationFunction: (List<String>, Address?, Map<String, String>) -> Map<String, String>

    fun createTags(result: ParseResult): Map<String, String> {
        val additionalTags = additionalTagsFunction.invoke(result.matchStreet)
        val address = getAddress(result)
        return tagCreationFunction.invoke(result.rawNaprStrings, address, additionalTags)
    }

    private fun getAddress(result: ParseResult): Address? =
        if (result.parsedAddressList.size == 1) result.parsedAddressList[0] else null


}





