package org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.ParsingFlags

data class Address(
//    val parsedPlace: ParsedPlace,
    val naprFullString: String,
    val parsedStreet: Street,
    val parsedHouseNumber: HouseNumber,
    val flags: MutableList<ParsingFlags>,
    val isSuccess: Boolean
) {
    //пустой адрес для неудачного парсинга
    constructor(naprFullString: String) :
            this(
                naprFullString,
                Street("", "", mutableListOf(), false),
                HouseNumber("", "", listOf(), false),
                mutableListOf(ParsingFlags.SPLIT_FAILED),
                false
            )

    fun getAllFlags(): List<ParsingFlags> {
        return (flags + parsedStreet.flags + parsedHouseNumber.flags).distinct()
    }

    fun getValidatedFlags(): List<ParsingFlags> {
        return (flags + parsedStreet.flags + parsedHouseNumber.flags)
            .distinct()
            .filter { flag -> flag == ParsingFlags.STREET_NAME_FUZZY_MATCH }
    }

    fun isBuildingAddress(): Boolean {
        return flags.contains(ParsingFlags.IS_BUILDING)
    }


}
