package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlag

data class Address(
    val source: String,
    var places: List<Place>,
    val street: Street,
    val houseNumber: HouseNumber,
    val flags: MutableList<ParsingFlag>,
    val isSuccess: Boolean
) {
    //пустой адрес для неудачного парсинга
    constructor(sourceString: String) :
            this(
                sourceString,
                emptyList(),
                Street("", "", mutableListOf(), false),
                HouseNumber("", "", listOf(), false),
                mutableListOf(ParsingFlag.SPLIT_FAILED),
                false
            )

    fun getAllFlags(): List<ParsingFlag> {
        return (flags + street.flags + houseNumber.flags).distinct()
    }

    /** Флаги значимые для валидатора */
    fun getValidationFlags(): List<ParsingFlag> {
        return (flags + street.flags + houseNumber.flags)
            .distinct()
            .filter { flag -> flag == ParsingFlag.STREET_NAME_FUZZY_MATCH }
    }

    fun isBuildingAddress(): Boolean {
        return flags.contains(ParsingFlag.IS_BUILDING)
    }

}