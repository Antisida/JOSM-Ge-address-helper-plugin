package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.actions
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.findBestMatchingAddress
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.getOsmStreetName
import kotlin.math.abs

    fun findBestMatchingAddressXXX(query: String, dataSet: DataSet) : String? {
        return findBestMatch(query, getOsmStreetName(dataSet))
    }

    fun main() {
        val list = listOf(
            "улица Михаила Лермонтова!", // +1 слово к запросу "улица Лермонтов" -> ПОДХОДИТ
            "улица Лермонтова...",       // Равна по количеству слов -> ПОДХОДИТ
            "Лермонтова"                 // Меньше запроса "улица Лермонтов" -> НЕ ПОДХОДИТ
        )

        val query = "улица Лермонтов"

        // В тесте "Лермонтова" (1 слово) отсеется сразу, так как она меньше запроса (2 слова)
        println("Запрос: \"$query\"")
        println("Найдено:  \"${findBestMatch(query, list)}\"")
        // Ожидаемо выведет: "улица Лермонтова..." (так как у нее разница в словах 0, а у Михаила — 1)
    }

    /**
     * Ищет самую близкую подходящую строку из списка. Тут  кандидат должен быть равен по длине запросу или быть длиннее ровно на 1 слово.
     */
    fun findBestMatch(query: String, list: List<String>): String? {
        val matches = list.mapNotNull { candidate ->
            val result = checkMatch(candidate, query)
            if (result != null) candidate to result else null
        }

        return matches.minWithOrNull(
            compareBy<Pair<String, MatchResult>> { it.second.wordCountDiff }
                .thenBy { it.second.totalLevenshtein }
        )?.first
    }

    data class MatchResult(val wordCountDiff: Int, val totalLevenshtein: Int)

    fun cleanString(text: String): String {
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

    fun isForbiddenWord(word: String): Boolean {
        val clean = word.lowercase().trim('.', ',', '-', ' ')
        if (clean.any { it.isDigit() }) return true

        val romanRegex = Regex("^[ivxlcdm]+(-[а-яa-z]+)?$")
        return romanRegex.matches(clean)
    }

    fun levenshtein(s1: String, s2: String): Int {
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




