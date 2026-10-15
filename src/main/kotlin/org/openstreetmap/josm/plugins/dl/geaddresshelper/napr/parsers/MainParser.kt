package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address

object MainParser {

    fun parse(rawAddrStringList: List<String>): List<Address> {
        val allAddresses: List<Address> = rawAddrStringList.map { line -> AddressParser.parse(line) }
        val distinctAddresses = removeDuplicated(allAddresses)
        distinctAddresses.forEach { it.places = getAllPlaces(allAddresses) }
        return distinctAddresses
    }

    private fun getAllPlaces(addresses: List<Address>) =
        addresses.map { address -> address.places }
            .flatten()
            .filter { it.isSuccess }
            .distinctBy { Pair(it.status, it.name) }

}
