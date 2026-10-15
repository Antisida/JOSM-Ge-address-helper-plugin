package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address

fun removeDuplicated(addresses: List<Address>): List<Address> {
    //исключение полностью одинаковых
    val distinct = addresses.distinctBy { Pair(it.street.extractedName, it.houseNumber.extractedNumber) }

    val byNumber: Map<String, List<Address>> = distinct.groupBy { it.houseNumber.extractedNumber }

    return byNumber
        .map { it.value }
        .map { addressList ->
            if (addressList.size == 1) addressList
            else reduceByStreetName(addressList)
        }
        .flatten()
}

fun reduceByStreetName(list: List<Address>): List<Address> {
    // 1. Сортируем по длине строки (от коротких к длинным)
    val sorted = list.sortedBy { it.street.extractedName.length }
    val toRemove = mutableSetOf<Address>()

    for (i in sorted.indices) {
        val query = sorted[i].street.extractedName
        // Если улица уже помечена на удаление, пропускаем её шаг проверки
        if (sorted[i] in toRemove) continue

        // 2. Ищем все более длинные улицы, которые подходят под наш запрос
        val matchingCandidates: List<Address> = sorted.drop(i + 1)
            .filter { candidate -> candidate !in toRemove }
            .filter { candidate -> OsmStreetMatcher.getMatchResult(candidate.street.extractedName, query).isMatchAndFirstLonger() }

        if (matchingCandidates.isNotEmpty()) {
            // Короткую строку (базис) удаляем, так как нашли более полные описания
            toRemove.add(sorted[i])

            // Из всех найденных кандидатов оставляем только самый длинный (полный),
            // а промежуточные варианты (например, "улица М Лермонтова") отправляем в toRemove
            val longestCandidate = matchingCandidates.maxByOrNull { it.street.extractedName.length }
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