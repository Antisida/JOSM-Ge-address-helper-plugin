package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.Matcher
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching.clean
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

        // 2. Ищем все более длинные улицы или равные по длине, которые подходят под наш запрос
        // в случае, когда равны, значит есть опечатка и останется рандомное название (возможно не правильное)
        // но оно приведется к нужному на этапе поиска улицы в слое, потому что там используется тот же Matcher.match()
        val matchingCandidates: List<Address> = sorted.drop(i + 1)
            .filter { candidate -> candidate !in toRemove }
            .filter { candidate -> candidate.street.extractedName.clean().sameSizeOrLongerThen(query.clean()) }
            .filter { candidate -> Matcher(candidate.street.extractedName, query).match().isMatched() }


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

fun String.sameSizeOrLongerThen(string: String): Boolean {
    return this.clean().length >= string.clean().length
}