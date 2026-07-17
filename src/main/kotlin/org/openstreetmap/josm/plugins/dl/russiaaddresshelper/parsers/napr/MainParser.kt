package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.actions.checkMatch
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.N_ParsedAddresses
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.RawNaprDto
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.dto.ParsingResult
import kotlin.collections.toList

class MainParser {

    /* fun parse(all: List<Triple<EastNorth, OsmPrimitive, RawNaprDto>>): List<ParsingResult> {
         return all
             .filter { it.third.isUseful() }
             .map { parse(it) }
             .toList()
 //            .map { it -> toResult(it) }
     }*/

    fun parse(rawAddrStringList: List<String>): List<N_ParsedAddress> {
        val parsedAddressList: List<N_ParsedAddress> = rawAddrStringList
            .map { line -> NAddressParser().parse(line) }
            .toList()
        //todo надо подумать как использовать не удачно распарсенные адреса
        val successAddresses = parsedAddressList
            .filter { parsedAddress -> parsedAddress.isSuccess }
            .toList()
        val distinctAddresses = removeDuplicated(successAddresses)
        return distinctAddresses
    }

    fun removeDuplicated(addresses: List<N_ParsedAddress>): List<N_ParsedAddress> {
        val distincted = addresses
            .distinctBy { Pair(it.parsedStreet.extractedName, it.parsedHouseNumber.extractedNumber) }
            .toList()

        val byNumber: Map<String, List<N_ParsedAddress>> = distincted
            .groupBy { it.parsedHouseNumber.extractedNumber }

        return byNumber
//            .filter { it.value.size > 1 }
            .map { it.value }
            .map {addresses ->
                if (addresses.size == 1) addresses
                else filterUnique(addresses)
//                else dropDuble(addresses)
            }
            .flatten()
//            .map { value ->
//                if (value.size > 1) dropDuble(value)
//                else value
//            }
//            .flatten()
    }

    fun filterUniqueStrings(list: List<String>): List<String> {
        // 1. Сортируем по длине строки (от коротких к длинным)
        val sorted = list.sortedBy { it.length }
        val toRemove = mutableSetOf<String>()

        for (i in sorted.indices) {
            val query = sorted[i]
            // Если строка уже помечена на удаление, пропускаем её шаг проверки
            if (query in toRemove) continue

            // 2. Ищем все более длинные строки, которые подходят под наш запрос
            val matchingCandidates = sorted.drop(i + 1).filter { candidate ->
                candidate !in toRemove && checkMatch(candidate, query) != null //fixme
            }

            if (matchingCandidates.isNotEmpty()) {
                // Короткую строку (базис) удаляем, так как нашли более полные описания
                toRemove.add(query)

                // Из всех найденных кандидатов оставляем только самый длинный (полный),
                // а промежуточные варианты (например, "улица М Лермонтова") отправляем в toRemove
                val longestCandidate = matchingCandidates.maxByOrNull { it.length }
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

    fun filterUnique(list: List<N_ParsedAddress>): List<N_ParsedAddress> {
        // 1. Сортируем по длине строки (от коротких к длинным)
        val sorted = list.sortedBy { it.parsedStreet.extractedName.length }
        val toRemove = mutableSetOf<N_ParsedAddress>()

        for (i in sorted.indices) {
            val query = sorted[i].parsedStreet.extractedName
            // Если строка уже помечена на удаление, пропускаем её шаг проверки
            if (sorted[i] in toRemove) continue

            // 2. Ищем все более длинные строки, которые подходят под наш запрос
            val matchingCandidates = sorted.drop(i + 1).filter { candidate ->
                candidate !in toRemove
                        && checkMatch(candidate.parsedStreet.extractedName, query) != null //fixme
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

    fun dropDuble1(addrs: List<N_ParsedAddress>): List<N_ParsedAddress> {


        return addrs.filter { current ->
            // Оставляем строку 'current' только если в списке НЕ НАШЛОСЬ строки 'other', которая:
            // 1. Отличается от 'current' ровно на 1 слово (наша функция возвращает true)
            // 2. Является более длинной по количеству слов
            addrs.none { other ->
                compareStringsWithFilter(current.parsedStreet.extractedName, other.parsedStreet.extractedName)
                        && isLonger(other.parsedStreet.extractedName, current.parsedStreet.extractedName)
            }
        }
    }

    fun dropDuble(addrs: List<N_ParsedAddress>): List<N_ParsedAddress> {
        return addrs.filter { current ->
            // Оставляем строку 'current' только если в списке НЕ НАШЛОСЬ строки 'other', которая:
            // 1. Отличается от 'current' ровно на 1 слово (наша функция возвращает true)
            // 2. Является более длинной по количеству слов
            addrs.none { other ->
                compareStringsWithFilter(current.parsedStreet.extractedName, other.parsedStreet.extractedName)
                        && isLonger(other.parsedStreet.extractedName, current.parsedStreet.extractedName)
            }
        }
    }

    // Вспомогательный метод для определения, в какой строке больше слов
    private fun isLonger(str1: String, str2: String): Boolean {
        val count1 = str1.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        val count2 = str2.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        return count1 > count2
    }

    fun compareStringsWithFilter(
        str1: String?,
        str2: String?,
    ): Boolean {
        if (str1 == null || str2 == null) {
            return false
        }
        if (str1 == str2) {
            return false
        }
        // 1. Очищаем строки и преобразуем в множества (Set) для сравнения состава слов
        val words1 = str1.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        val words2 = str2.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()

        val size1 = words1.size
        val size2 = words2.size

        return when {
            size1 == size2 -> {
                false
            }

            // Сценарий 2: В первой строке на одно слово больше (например, 3 и 2)
            // Все слова из второй строки должны полностью содержаться в первой.
            size1 == size2 + 1 -> {
                words1.containsAll(words2)
            }

            // Сценарий 3: Во второй строке на одно слово больше (например, 2 и 3)
            // Все слова из первой строки должны полностью содержаться во второй.
            size2 == size1 + 1 -> {
                words2.containsAll(words1)
            }

            // Любые другие случаи (разница в длине 2 и более слов)
            else -> false
        }
    }

    /* fun parse(data: Triple<EastNorth, OsmPrimitive, RawNaprDto>): ParsingResult {
 //  todo добавить
 //   val rowAddrTags: Map<String, String> = allAddrStrings
 //            .distinct()
 //            .mapIndexed { index, string -> "napr:addr:row:${index + 1}" to string }
 //            .toMap()
         val rawAddrStringList = getUsefulString(data.third)
         val best =
             findBest(rawAddrStringList)//todo нужно определять адреса где есть номер дома и улица и парсить каждый адрес
 //        if (best != null) {
         val parsedAddress = NAddressParser().parse(best, OsmDataManager.getInstance().editDataSet)
 //        } else {
 //            return
 //        }
         var buildingTags = emptyList<Pair<String, String>>()
         if (parsedAddress.parsedHouseNumber.extractedNumber.isNotEmpty()) {
             buildingTags = composeBuildingTag(parsedAddress)
         }
         val nodeTags = composeNodeTags(rawAddrStringList)
         val flags = listOf<ParsingFlags>().toMutableList()
         flags.addAll(parsedAddress.parsedHouseNumber.flags)
         flags.addAll(parsedAddress.parsedStreet.flags)
         flags.addAll(parsedAddress.flags)//fixme а нужно ли это поле у адреса
         return ParsingResult(
             data.first,
             data.second,
             buildingTags,
             nodeTags,
             flags,
             N_ParsedAddresses(listOf(parsedAddress))
         )

     }*/

    fun composeBuildingTag(parsedAddress: N_ParsedAddress): MutableMap<String, String> {
        val tags = mutableMapOf<String, String>()
        if (parsedAddress.parsedHouseNumber?.extractedNumber != null) {
            tags.put("addr:housenumber", parsedAddress.parsedHouseNumber.extractedNumber)
        }
        if (parsedAddress.parsedStreet?.extractedName != null) {
//            Logging.info("2: ${parsedAddress.parsedStreet.extractedName}")
            tags.put("addr:street", parsedAddress.parsedStreet.extractedName)
        }
        tags.put("napr:addr", parsedAddress.naprFullString)
        return tags
    }

    //грязные данные напр в теги fixme удалить первый параметр, добавить теги для удаления
    fun composeNodeTags(rawAddrStrings: List<String>): List<Pair<String, String>> {
        val rowAddrTags: List<Pair<String, String>> = rawAddrStrings
            .distinct()
            .mapIndexed { index, string -> Pair("napr:addr:raw:${index + 1}", string) }
            .toList()
        return rowAddrTags
    }


    val statuses =
        setOf("ქუჩა", "ქ.", "გამზირი", "გამზ.", "ბულვარი", "ჩიხი", "შესახვევი", "შეს.", "გასასვლელი", "აღმართი")

    fun split(addrString: String): List<String> {
        val allAddrStrings = addrString
            .split(",")
            .filter { line -> "N" in line || statuses.any { status -> status in line } }
            .takeLast(2)
            .map { it.trim() }
        return allAddrStrings
    }

    fun getUsefulString(naprBody: RawNaprDto): List<String> {
        val allAddrStrings = naprBody.result
            ?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
            ?.filter { it.isNotEmpty() }
            ?.filter { line -> "N" in line } //todo не теряем ли мы номера без N
            ?.filter { line -> statuses.any { status -> status in line } }
        //    ?.filter { line -> !line.contains("ნაკვეთი") } //todo участок удалять при сплите
            ?: emptyList()
        return allAddrStrings
    }

    /**
     * Расширяет сокращения в названиях грузинских улиц.
     */
    fun String.expandStreetPrefixes(): String {
        return this.replace("ქ. ", "ქუჩა ")
            .replace("გამზ. ", "გამზირი ")
            .replace("შეს. ", "შესახვევი ")
    }

    /*  */
    /**  Поиск лучшей строки для парсинга *//*
    fun findBest(addrStrings: List<String>): String {
        // План А: Ищем самую длинную с буквой "N" (исключая "ნაკვეთი" 'участок')
        val withN = addrStrings
            .filter { "N" in it && "ნაკვეთი" notIn it }
            .maxByOrNull { it.length }
        if (withN != null) return withN

        // План Б: Если строки с N нет, берем самую длинную из всех
        return addrStrings.maxBy { it.length }
    }

    private infix fun String.notContains(string: String): Boolean = this.contains(string)//!string.contains(this)

    fun normalize(addrString: String): String {
        return addrString
            .insertMissingComma()
            .expandAbbreviations()
            .removeParenthesesContent()
            .collapseDuplicateCommas() //ქალაქი სენაკი , ქუჩა ლ. თეფანია ,   (ყოფ. ჯორჯიაშვილი) , N 16
    }*/

    /** Ищет букву N, если перед ней (через любые пробелы) НЕТ запятой и вставляет запятую. */
    fun String.insertMissingComma(): String {
        return this.replace(Regex("([^,\\s])\\s*N"), "$1, N")
    }


    /**
     * Удаляет из строки круглые скобки и всё, что находится внутри них.
     * Также очищает лишние двойные пробелы, которые могли образоваться после удаления.
     */
    fun String.removeParenthesesContent(): String {
        // Регулярное выражение находит ( и ) и всё между ними (жадный поиск исключая закрывающую скобку)
        val regex = """(?U)\([^)]*\)""".toRegex()

        return this.replace(regex, "")
            .replace("""(?U)\s+""".toRegex(), " ") // Схлопываем разбежавшиеся пробелы в один
            .trim()                            // Убираем пробелы по краям, если скобки были в начале/конце
    }

    /**
     * Заменяет две запятые, между которыми есть только пробелы (или нет ничего), на одну запятую.
     * Например: ", ," -> ", " или ",," -> ","
     */
    fun String.collapseDuplicateCommas(): String {
        // Регулярное выражение ищет: запятую, затем ноль или более пробелов, затем вторую запятую
        val regex = """(?U),\s*,""".toRegex()

        // Заменяем найденное комбо на одну запятую с последующим пробелом для красоты
        return this.replace(regex, ", ")
            .replace("""(?U)\s+""".toRegex(), " ") // Схлопываем случайные двойные пробелы
            .trim()
    }
}
