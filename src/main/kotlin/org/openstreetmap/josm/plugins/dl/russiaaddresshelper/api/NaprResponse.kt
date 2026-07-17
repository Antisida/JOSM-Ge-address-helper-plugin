package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api

data class NaprResponse(val responses: MutableList<RawNaprDto> = mutableListOf()) {

    fun addResponse(res: RawNaprDto) {
        responses.add(res)
    }

    fun isNotEmpty(): Boolean {
        return responses.isNotEmpty()
    }

    fun isEmpty(): Boolean {
        return responses.isEmpty()
    }

//    fun hasReadableAddress(): Boolean {
//        return responses.values.any { res ->
//            res.features.any { feat ->
//                feat.properties?.options?.readableAddress?.isNotBlank()
//                    ?: false
//            }
//        }
//    }

//    fun parseAddresses(coordinate: EastNorth): ParsedAddressInfo {
//        val addresses: MutableList<ParsedAddress> = mutableListOf()
//        val existingAddresses: MutableList<String> = mutableListOf()
//        responses.forEach { response ->
//            val layer = response.key
//            val parsedAddressInfo = response.value.parseAddresses(coordinate)
//            parsedAddressInfo.addresses.forEach { parsedAddress ->
//                //нужно модифицировать валидаторы с учетом проверки слоя
//                if (layer.isBuilding()) parsedAddress.flags.add(ParsingFlags.IS_BUILDING)
//                parsedAddress.layer = layer
//                val key = parsedAddress.getOsmAddress().getInlineAddress()
//                    ?: response.value.getAddressMatchKeyForUnparsedAddress(parsedAddress)
//                if (!existingAddresses.contains(key)) {
//                    addresses.add(parsedAddress)
//                    existingAddresses.add(key)
//                }
//            }
//        }
//        return ParsedAddressInfo(addresses)
//    }
}