package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.tools.Geometry

object OsmStreetMatcher {

    fun findByNameAndDistance(streets: List<OsmPrimitive>, query: String, building: OsmPrimitive, distance: Int): String? {
        val byDistance: List<OsmPrimitive> = streets.filter { Geometry.getDistance(building, it) < distance }
        val candidateNames: List<String> = byDistance
            .map { listOfNotNull(it.get("name"), it.get("alt_name"), it.get("old_name"), it.get("short_name")) }
            .flatten()
        return getBestOrNull(candidateNames, query)
    }


    //дает самый подходящий результат (если подходящие есть), если нет подходящих - то нулл
    fun getBestOrNull(
        candidateNames: List<String>,
        query: String
    ): String? {
        return  candidateNames.mapNotNull { cand ->
            val result: MatchResult = getMatchResult(cand, query)
            if (result.isMatched()) cand to result else null
        }.minByOrNull { it.second }?.first
    }

 /*   *//**
     * @return: Лист найденных улиц. <имя в осм, вей, результат>
     * Близкий OsmPrimitive (вей) по 'name', 'alt_name', 'old_name', 'short_name' или null
     * кандидат должен быть равен по длине запросу или быть длиннее ровно на 1 слово,
     * одно слово должно совпадать точно(статусная часть), другие могут отличаться на 1 символ
     *//*
    private fun getMatchFromListWay(
        ways: List<OsmPrimitive>,
        query: String
    ): List<Triple<String, OsmPrimitive, MatchResult>> {
        val matches = ways.mapNotNull { osmCandidateWay ->
            val osmNameToMatchResult: Pair<String, MatchResult>? = checkMatchOneWay(osmCandidateWay, query)
            //имя в osm -- way с этим именем -- matchResult
            if (osmNameToMatchResult != null) Triple(
                osmNameToMatchResult.first,
                osmCandidateWay,
                osmNameToMatchResult.second
            )
            else null
        }
        return matches
    }*/

/*    *//** @return: название улицы к MatchResult для одного OsmPrimitive или null*//*
    private fun checkMatchOneWay(candidate: OsmPrimitive, query: String): Pair<String, MatchResult>? {
        val names = listOfNotNull(
            candidate.get("name"),
            candidate.get("alt_name"),
            candidate.get("old_name"),
            candidate.get("short_name")
        )
        val candidateToResult: Pair<String, MatchResult>? =
            names.mapNotNull { cand ->
                val result: MatchResult = getMatchResult(cand, query)
                if (result.isMatched()) cand to result else null
            }
                .minWithOrNull(compareBy<Pair<String, MatchResult>> { it.second.diff }.thenBy { it.second.totalLevenshtein })
        return candidateToResult
    }*/

    private fun cleanString(text: String): String {
        return text.filter { it.isLetterOrDigit() || it.isWhitespace() }
    }

    /**
     * Проверяет строки на соответствие правилам.
     * Теперь строго: кандидат должен быть равен по длине запросу или быть длиннее ровно на 1 слово.
     */
    fun getMatchResult(candidate: String, query: String): MatchResult {
        val cleanedCandidate: String = cleanString(candidate)
        val cleanedQuery: String = cleanString(query)

        val w1 = cleanedCandidate.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() } // Кандидат
        val w2 = cleanedQuery.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }    // Запрос

        // Разница (Кандидат - Запрос) должна быть строго 0 или 1. Нас не интересуют более короткие названия, чем запрос.
        // Если кандидат меньше (разница отрицательная) или слишком длинный — сразу отбрасываем.
        val diff = w1.size - w2.size
        if (diff !in 0..1) {
            return MatchResult(
                cleanedCandidate.length,
                cleanedQuery.length,
                w1.size,
                w2.size,
                -1,
                -1,
                w1.size - w2.size,
                -1,
                listOf(MatchFlag.DIFF_MORE_THEN_ONE_WORD)
            )
        }

        // Так как кандидат гарантированно больше или равен запросу:
        val smaller = w2 // Запрос
        val larger = w1  // Кандидат

        val usedInLarger = BooleanArray(larger.size)   //индексы слов в larger, для которых нашлось совпадение
        val usedInSmoller = BooleanArray(smaller.size) //индексы слов в larger, для которых нашлось совпадение
        var exactCount = 0 //счетчик точных совпадений
        var fuzzyCount = 0 //счетчик неточных совпадений
        var totalLev = 0

        val flags: MutableList<MatchFlag> = mutableListOf()

        // Сопоставляем каждое слово из запроса (smaller) со словом из кандидата (larger)
        for (sWord in smaller) {
            /*  var matchedIdx = -1*/

            // 1. Ищем точное совпадение
            ////---------------
            val idx: Int = getIdx(sWord, larger, usedInLarger)
            if (idx != -1) {
                usedInLarger[idx] = true
                usedInSmoller[smaller.indexOf(sWord)] = true
                exactCount++
                /*for (j in larger.indices) {
                    if (!usedInLarger[j] && sWord == larger[j]) {
                        matchedIdx = j
                        break
                    }
                }

                if (matchedIdx != -1) {
                    usedInLarger[matchedIdx] = true
                    usedInSmoller[smaller.indexOf(sWord)] = true
                    exactCount++*/
            } else {
                // 2. Если точного нет, ищем с опечаткой в 1 символ
                val maxLev: Int = 1
                val idxl: Int = getIdx(sWord, larger, usedInLarger, maxLev)
                if (idxl != -1) {
                    flags.add(MatchFlag.DIFF_LEV)
                    fuzzyCount++
                    totalLev += maxLev //fixme тут верно пока значение 1
                    usedInLarger[idxl] = true
                    usedInSmoller[smaller.indexOf(sWord)] = true
                }
                /*for (j in larger.indices) {
                    if (!usedInLarger[j]) {
                        val lev = levenshtein(sWord, larger[j])
                        if (lev <= 1) { // эту проверку надо вынести из этого метода наружу
                            // Запрещаем опечатки в цифрах/числах
                            if (containDigit(sWord) || containDigit(larger[j])) {
                                break
                            }
                            matchedIdx = j
                            totalLev += lev
                            flags.add(MatchFlag.DIFF_ONE_LETTER)
                            break
                        }
                    }
                }
                if (matchedIdx != -1) {
                    exactCount++
                    usedInLarger[matchedIdx] = true
                    usedInSmoller[smaller.indexOf(sWord)] = true
                }*/
            }
        }

        val wordCountMatch = usedInSmoller.count { it }
        val wordCountNotMatch = larger.size - wordCountMatch

        // Обязательно должно быть хотя бы 2 совпадения (статусная часть и еще что-нибудь)
        if (exactCount + fuzzyCount < 2) { //todo вынести в результат
            flags.add(MatchFlag.MATCHED_WORD_LESS_THEN_TWO)
        }

        // 3. количество слов одинаковое, только одно слово не совпало - проверяем не сокращение ли это
        if (larger.size == smaller.size) {
            if (larger.size - exactCount == 1) {
                val idxSmallerNotFound = usedInSmoller.indexOf(false) //индекс не найденное слово в коротком
                val idxLargerNotFound = usedInLarger.indexOf(false)
                if (idxSmallerNotFound != -1 && idxLargerNotFound != -1) {
                    val smallerNotFound = smaller[idxSmallerNotFound]
                    val largerNotFound = larger[idxLargerNotFound]
                    if (largerNotFound.startsWith(smallerNotFound)
                        || smallerNotFound.startsWith(largerNotFound)
                    ) {
                        flags.add(MatchFlag.ABBR)
                    }
                }
            }
        }

        // 4. Если кандидат длиннее на 1 слово, проверяем лишнее слово на "запрещенность" (не должно содержать цифры) случай с В Ленина и Ленина
        if (larger.size > smaller.size) {
            val extraIdx = usedInLarger.indexOf(false)
            if (extraIdx != -1) {
                val extraWord = larger[extraIdx]
                if (containDigit(extraWord)) {
                    flags.add(MatchFlag.ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT)
                }
            }
        }
        return MatchResult(
            cleanedCandidate.length,
            cleanedQuery.length,
            w1.size,
            w2.size,
            wordCountNotMatch,
            wordCountMatch,
            w1.size - w2.size,
            totalLev,
            flags
        )
    }

    private fun getIdx(string: String, list: List<String>, usedInLarger: BooleanArray): Int {
        for (j in list.indices) {
            if (!usedInLarger[j] && string == list[j]) {
                return j
            }
        }
        return -1
    }

    private fun getIdx(string: String, list: List<String>, usedInLarger: BooleanArray, maxLev: Int): Int {
        for (j in list.indices) {
            if (!usedInLarger[j]) {
                val lev = levenshtein(string, list[j])
                if (lev <= maxLev) { // эту проверку надо вынести из этого метода наружу
                    // Запрещаем опечатки в цифрах/числах
                    if (containDigit(string) || containDigit(list[j])) {
                        break
                    }
                    return j
                }
            }
        }
        return -1
    }


//    fun Collection<String>.indexOf(str: String): Integer {
//        return this.filter {
//            it !is Node &&
//                    it.hasTag(BUILDING_TAG) &&
//                    ignoreTags.all { (key, values) ->
//                        values.none { tagValue ->
//                            it.hasTag(key, tagValue) || (it.hasKey(key) && tagValue == "*")
//                        }
//                    }
//        }
//    }
//
//    private fun
//            for (j in larger.indices)
//    {
//        if (!usedInLarger[j] && sWord == larger[j]) {
//            matchedIdx = j
//            break
//        }
//    }

    val romanDigits = setOf('I', 'V', 'X', 'L'/*, 'C', 'D', 'M'*/)

    private fun containDigit(word: String): Boolean {
        val clean = word.uppercase()//.trim('.', ',', '-', ' ')
        if (clean.any { it.isDigit() }) return true

        return clean.any { it in romanDigits }
    }

    private fun levenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                dp[j] = if (s1[i - 1] == s2[j - 1]) prev else minOf(dp[j], dp[j - 1], prev) + 1
                prev = temp
            }
        }
        return dp[s2.length]
    }

}