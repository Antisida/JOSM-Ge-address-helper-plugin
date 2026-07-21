package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.utils.OsmStreetMatcher

object MainParser {

    fun parse(rawAddrStringList: List<String>): List<Address> {
        val parsedAddressList: List<Address> = rawAddrStringList
            .map { line -> AddressParser.parse(line) }
        val successAddresses = parsedAddressList
            .filter { parsedAddress -> parsedAddress.isSuccess }
        val distinctAddresses = removeDuplicated(successAddresses)
        return distinctAddresses
    }

    private fun removeDuplicated(addresses: List<Address>): List<Address> {
        val distinct = addresses
            .distinctBy { Pair(it.parsedStreet.extractedName, it.parsedHouseNumber.extractedNumber) }

        val byNumber: Map<String, List<Address>> = distinct
            .groupBy { it.parsedHouseNumber.extractedNumber }

        return byNumber
            .map { it.value }
            .map { addresses ->
                if (addresses.size == 1) addresses
                else filterUnique(addresses)
            }
            .flatten()
    }

    private fun filterUnique(list: List<Address>): List<Address> {
        // 1. Сортируем по длине строки (от коротких к длинным)
        val sorted = list.sortedBy { it.parsedStreet.extractedName.length }
        val toRemove = mutableSetOf<Address>()

        for (i in sorted.indices) {
            val query = sorted[i].parsedStreet.extractedName
            // Если строка уже помечена на удаление, пропускаем её шаг проверки
            if (sorted[i] in toRemove) continue

            // 2. Ищем все более длинные строки, которые подходят под наш запрос
            val matchingCandidates = sorted.drop(i + 1).filter { candidate ->
                candidate !in toRemove
                        && OsmStreetMatcher.checkMatch(candidate.parsedStreet.extractedName, query) != null //fixme
            }

            if (matchingCandidates.isNotEmpty()) {
                // Короткую строку (базис) удаляем, так как нашли более полные описания
                toRemove.add(sorted[i])

                // Из всех найденных кандидатов оставляем только самый длинный (полный),
                // а промежуточные варианты (например, "улица М Лермонтова") отправляем в toRemove
                val longestCandidate = matchingCandidates.maxByOrNull { it.parsedStreet.extractedName.length }
                matchingCandidates.forEach { candidate ->
                    if (candidate != longestCandidate) {
                        toRemove.add(candidate)
                    }
                }
            }
        }

        // Возвращаем только те строки, которые не попали в список на удаление
        return sorted.filter { it !in toRemove }
    }

}
