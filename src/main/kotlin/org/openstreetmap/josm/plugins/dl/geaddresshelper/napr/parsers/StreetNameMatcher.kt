package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

import org.openstreetmap.josm.data.osm.OsmPrimitive

object StreetNameMatcher {

    fun findBestOrNull(streets: List<OsmPrimitive>, query: String): String? {
        val candidateNames: List<String> = streets
            .map { listOfNotNull(it.get("name"), it.get("alt_name"), it.get("old_name"), it.get("short_name")) }
            .flatten()
        return bestLongerMatchedOrNull(candidateNames, query)
    }

    /* */
    /** @return: самый подходящий результат (если подходящие есть) из списка candidateNames, если нет подходящих - null *//*
    fun bestOrNull(candidateNames: List<String>, query: String): String? {
        return candidateNames.mapNotNull { candidate ->
            val result: MatchResult = getMatchResult(candidate, query)
            if (result.isMatched()) candidate to result else null
        }.minByOrNull { it.second }?.first
    }*/

    /** @return: самый подходящий результат (если подходящие есть) из списка candidateNames, если нет подходящих - null */
    fun bestLongerMatchedOrNull(candidateNames: List<String>, query: String): String? {
        return candidateNames
            .mapNotNull { candidate ->
                val match = Matcher(candidate, query).match()
                if (match.isMatched()) candidate to match else null
            }
            .minByOrNull { it.second }?.first
    }

/*    private fun cleanString(text: String): String {
        return text.filter { it.isLetterOrDigit() || it.isWhitespace() }
    }*/

/*    fun getMatchResult(candidate: String, query: String): MatchResult {
        val cleanedCandidate: String = cleanString(candidate)
        val cleanedQuery: String = cleanString(query)

        val w1 = cleanedCandidate.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() } // Кандидат
        val w2 = cleanedQuery.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }    // Запрос


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
            // 1. Ищем точное совпадение
            val idx: Int = getIdx(sWord, larger, usedInLarger)
            if (idx != -1) {
                usedInLarger[idx] = true
                usedInSmoller[smaller.indexOf(sWord)] = true
                exactCount++
            } else {
                // 2. Если точного нет, ищем с опечаткой в 1 символ
                val maxLev = 2
                val idxl: Int = getIdx(sWord, larger, usedInLarger, maxLev)
                if (idxl != -1) {
                    fuzzyCount++
                    totalLev += maxLev //fixme тут верно пока значение 1
                    usedInLarger[idxl] = true
                    usedInSmoller[smaller.indexOf(sWord)] = true
                }
            }
        }

        val wordCountMatch = usedInSmoller.count { it }

        val wordCountNotMatch =
            if (larger.size > smaller.size) larger.size - wordCountMatch
            else smaller.size - wordCountMatch

        // 3. количество слов одинаковое, только одно слово не совпало - проверяем не сокращение ли это
        // не используется. потому что избыточно: если larger.size - exactCount == 1 считаем что совпало
        *//*        if (larger.size == smaller.size) {
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
                }*//*

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
            exactCount,
            fuzzyCount,
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

    val romanDigits = setOf('I', 'V', 'X', 'L'*//*, 'C', 'D', 'M'*//*)

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
    }*/



    /*
        data class MutablePair<A, B>(var first: A, var second: B)

        //todo refactoring
        fun getMatchResult2(candidate: String, query: String): MatchResult {
            val cleanedCandidate: String = cleanString(candidate)
            val cleanedQuery: String = cleanString(query)

            val w1 = cleanedCandidate
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .map{ MutablePair(it, -1) }// Кандидат
            val w2 = cleanedQuery.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() } .map{ MutablePair(it, -1) }   // Запрос

            // Разница (Кандидат - Запрос) должна быть строго 0 или 1. Нас не интересуют более короткие названия, чем запрос.
            // Если кандидат меньше (разница отрицательная) или слишком длинный — сразу отбрасываем.
            *//*   val diff = w1.size - w2.size
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
                   listOf()
               )
           }*//*

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
            var matchedIdx = -1
            // 1. Ищем точное совпадение
            for (lWord in larger) {
                if (lWord.second != 1 && sWord.first == lWord.first) {
                    sWord.second = 1
                    lWord.second = 1
                    matchedIdx = 1
                    break
                }
            }

            if (matchedIdx != -1) {
                usedInLarger[matchedIdx] = true
                exactCount++
            } else {
                // 2. Если точного нет, ищем с опечаткой в 1 символ
                for (lWord in larger) {
                    if (lWord.second != 1) {
                        val lev = levenshtein(sWord.first, lWord.first)
                        if (lev <= 2) {
                            // Запрещаем опечатки в цифрах/числах
                            if (containDigit(sWord.first) || containDigit(sWord.first)) {
                                break
                            }
                            matchedIdx = 1
                            sWord.second = 2
                            lWord.second = 2
                            totalLev += lev
                            break
                        }
                    }
                }
                if (matchedIdx != -1) {

//                    flags.add(MatchFlag.DIFF_LEV)
                    fuzzyCount++
                    totalLev += maxLev //fixme тут верно пока значение 1
                    usedInLarger[idxl] = true
                    usedInSmoller[smaller.indexOf(sWord)] = true
                }
            }
        }

        val wordCountMatch = usedInSmoller.count { it }

        val wordCountNotMatch = if (larger.size > smaller.size) larger.size - wordCountMatch else smaller.size - wordCountMatch
//        val wordCountNotMatch = larger.size - wordCountMatch

        // Обязательно должно быть хотя бы 2 совпадения (статусная часть и еще что-нибудь)
//        if (exactCount + fuzzyCount < 2) { //todo вынести в результат
//            flags.add(MatchFlag.MATCHED_WORD_LESS_THEN_TWO)
//        }

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
            exactCount,
            fuzzyCount,
//            w1.size - w2.size,
            totalLev,
            flags
        )
    }*/

}