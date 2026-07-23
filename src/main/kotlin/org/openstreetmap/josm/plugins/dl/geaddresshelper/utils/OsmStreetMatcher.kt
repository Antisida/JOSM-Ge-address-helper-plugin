package org.openstreetmap.josm.plugins.dl.geaddresshelper.utils

import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.getNamedStreets
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.getAllStreetNames
import org.openstreetmap.josm.tools.Geometry.getDistance

object OsmStreetMatcher {

    data class MatchResult(val wordCountDiff: Int, val totalLevenshtein: Int)

    //без расстояния (не используется)
    fun findByName(dataSet: DataSet, query: String): String? {
        return findBestMatch(query, dataSet.getAllStreetNames())
    }

    fun findByNameAndDistance(dataSet: DataSet, query: String, building: OsmPrimitive, distance: Int): String? {
        val byDistance: List<Triple<String, OsmPrimitive, MatchResult>> =
            findBestMatch(dataSet.getNamedStreets(), query)
                .filter { getDistance(building, it.second) < distance }

        if (byDistance.isNotEmpty()) {
            val best = byDistance.minWithOrNull(
                compareBy<Triple<String, OsmPrimitive, MatchResult>> { it.third.wordCountDiff }
                    .thenBy { it.third.totalLevenshtein }
            )
            //имя в osm
            if (best != null) return best.first
        }
        return null
    }

    /**
     * @return: Лист найденных улиц. <имя в осм, вей, результат>
     * Близкий OsmPrimitive (вей) по 'name', 'alt_name', 'old_name', 'short_name' или null
     * кандидат должен быть равен по длине запросу или быть длиннее ровно на 1 слово,
     * одно слово должно совпадать точно(статусная часть), другие могут отличаться на 1 символ
     */
    private fun findBestMatch(
        list: List<OsmPrimitive>,
        query: String
    ): List<Triple<String, OsmPrimitive, MatchResult>> {
        val matches = list.mapNotNull { osmCandidateWay ->
            val osmNameToMatchResult: Pair<String, MatchResult>? =
                checkMatch(osmCandidateWay, query)
            //имя в osm -- way с этим именем -- matchResult
            if (osmNameToMatchResult != null) Triple(
                osmNameToMatchResult.first,
                osmCandidateWay,
                osmNameToMatchResult.second
            )
            else null
        }

        return matches
    }

    /** @return: MatchResult для одного OsmPrimitive или null*/
    private fun checkMatch(candidate: OsmPrimitive, query: String): Pair<String, MatchResult>? {
        val names = listOfNotNull(
            candidate.get("name"),
            candidate.get("alt_name"),
            candidate.get("old_name"),
            candidate.get("short_name")
        )
        val candidateToResult: Pair<String, MatchResult>? = names.mapNotNull { cand ->
            val result = checkMatch(cand, query)
            if (result != null) cand to result else null
        }.minWithOrNull(
            compareBy<Pair<String, MatchResult>> { it.second.wordCountDiff }
                .thenBy { it.second.totalLevenshtein }
        )
        return candidateToResult
    }

    /**
     * Ищет самую близкую подходящую строку из списка.
     * Тут кандидат должен быть равен по длине запросу или быть длиннее ровно на 1 слово.
     */
    private fun findBestMatch(query: String, list: List<String>): String? {
        val matches = list.mapNotNull { candidate ->
            val result = checkMatch(candidate, query)
            if (result != null) candidate to result else null
        }

        return matches.minWithOrNull(
            compareBy<Pair<String, MatchResult>> { it.second.wordCountDiff }
                .thenBy { it.second.totalLevenshtein }
        )?.first
    }

    private fun cleanString(text: String): String {
        return text.filter { it.isLetterOrDigit() || it.isWhitespace() }
    }

    /**
     * Проверяет строки на соответствие правилам.
     * Теперь строго: кандидат должен быть равен по длине запросу или быть длиннее ровно на 1 слово.
     */
    fun checkMatch(candidate: String, query: String): MatchResult? {
        val cleanedCandidate = cleanString(candidate)
        val cleanedQuery = cleanString(query)

        val w1 = cleanedCandidate.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() } // Кандидат
        val w2 = cleanedQuery.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }    // Запрос

        // НОВОЕ ПРАВИЛО: Разница (Кандидат - Запрос) должна быть строго 0 или 1.
        // Если кандидат меньше (разница отрицательная) или слишком длинный — сразу отбрасываем.
        val diff = w1.size - w2.size
        if (diff !in 0..1) {
            return null
        }

        // Так как кандидат гарантированно больше или равен запросу:
        val smaller = w2 // Запрос
        val larger = w1  // Кандидат

        val usedInLarger = BooleanArray(larger.size)
        var exactCount = 0
        var totalLev = 0

        // Сопоставляем каждое слово из запроса (smaller) со словом из кандидата (larger)
        for (sWord in smaller) {
            var matchedIdx = -1

            // 1. Ищем точное совпадение
            for (j in larger.indices) {
                if (!usedInLarger[j] && sWord == larger[j]) {
                    matchedIdx = j
                    break
                }
            }

            if (matchedIdx != -1) {
                usedInLarger[matchedIdx] = true
                exactCount++
            } else {
                // 2. Если точного нет, ищем с опечаткой в 1 символ
                for (j in larger.indices) {
                    if (!usedInLarger[j]) {
                        val lev = levenshtein(sWord, larger[j])
                        if (lev <= 1) {
                            // Запрещаем опечатки в цифрах/числах
                            if (isForbiddenWord(sWord) || isForbiddenWord(larger[j])) {
                                return null
                            }
                            matchedIdx = j
                            totalLev += lev
                            break
                        }
                    }
                }
                if (matchedIdx != -1) {
                    usedInLarger[matchedIdx] = true
                } else {
                    return null // Слово из запроса не нашлось в кандидате
                }
            }
        }

        // Обязательно должно быть хотя бы одно точное совпадение
        if (exactCount < 1) return null

        // 3. Если кандидат длиннее на 1 слово, проверяем лишнее слово на "запрещенность"
        if (larger.size > smaller.size) {
            val extraIdx = usedInLarger.indexOf(false)
            if (extraIdx != -1) {
                val extraWord = larger[extraIdx]
                if (isForbiddenWord(extraWord)) {
                    return null
                }
            }
        }

        return MatchResult(wordCountDiff = diff, totalLevenshtein = totalLev)
    }

    private fun isForbiddenWord(word: String): Boolean {
        val clean = word.lowercase().trim('.', ',', '-', ' ')
        if (clean.any { it.isDigit() }) return true

        val romanRegex = Regex("^[ivxlcdm]+(-[а-яa-z]+)?$")
        return romanRegex.matches(clean)
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
                candidate !in toRemove && checkMatch(candidate, query) != null
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
}




