package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags

data class Address(
    val source: String,
    val place: Place,
    val street: Street,
    val houseNumber: HouseNumber,
    val flags: MutableList<ParsingFlags>,
    val isSuccess: Boolean
) {
    //пустой адрес для неудачного парсинга
    constructor(sourceString: String) :
            this(
                sourceString,
                Place("", "", mutableListOf(), false),
                Street("", "", mutableListOf(), false),
                HouseNumber("", "", listOf(), false),
                mutableListOf(ParsingFlags.SPLIT_FAILED),
                false
            )

    fun getAllFlags(): List<ParsingFlags> {
        return (flags + street.flags + houseNumber.flags).distinct()
    }

    fun getValidatedFlags(): List<ParsingFlags> {
        return (flags + street.flags + houseNumber.flags)
            .distinct()
            .filter { flag -> flag == ParsingFlags.STREET_NAME_FUZZY_MATCH }
    }

    fun isBuildingAddress(): Boolean {
        return flags.contains(ParsingFlags.IS_BUILDING)
    }

}