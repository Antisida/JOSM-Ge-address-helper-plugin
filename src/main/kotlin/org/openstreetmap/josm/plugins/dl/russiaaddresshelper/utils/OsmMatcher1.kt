package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.utils
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.getOsmStreetName
import kotlin.math.abs
class OsmMatcher1 {

    fun findBestMatchingAddress(query: String, dataSet: DataSet) : String? {
        return findBestMatch(query, getOsmStreetName(dataSet))
    }

    fun main() {
        val list = listOf(
            "улица Михаила Лермонтова",
            "улица Лермонтова II",      // Не должна совпасть из-за римской цифры
            "улица Лермонтова",
            "1-я улица Лермонтова"      // Не должна совпасть с "2-я..." из-за цифры
        )

        // Тест 1: Ищем наиболее близкую строку для "улица Лермонтов"
        // Должна выбраться "улица Лермонтова", так как у нее совпадает кол-во слов (нет лишнего "Михаила")
        val query1 = "улица Лермонтов"
        println("Запрос: \"$query1\" -> Найдено: \"${findBestMatch(query1, list)}\"")
        // Выведет: улица Лермонтова

        // Тест 2: Запрос с римской цифрой
        val query2 = "улица Лермонтова I"
        println("Запрос: \"$query2\" -> Найдено: \"${findBestMatch(query2, list)}\"")
        // Выведет: null (так как "I" и "II" — римские цифры)
    }

    /**
     * Ищет самую близкую подходящую строку из списка.
     */
    fun findBestMatch(query: String, list: List<String>): String? {
        val matches = list.mapNotNull { candidate ->
            val result = checkMatch2(candidate, query) // есть еще одна checkMatch
            if (result != null) candidate to result else null
        }

        // Сортируем результаты:
        // 1. Сначала те, где разница в количестве слов меньше (wordCountDiff)
        // 2. Затем те, где меньше опечаток (totalLevenshtein)
        return matches.minWithOrNull(
            compareBy<Pair<String, MatchResult>> { it.second.wordCountDiff }
                .thenBy { it.second.totalLevenshtein }
        )?.first
    }

    // Простой класс для хранения параметров совпадения
    data class MatchResult(val wordCountDiff: Int, val totalLevenshtein: Int)

    fun cleanString(text: String): String {
        return text.filter { it.isLetterOrDigit() || it.isWhitespace() }
    }


    //перед стравнением cleanString
    fun checkMatch2(candidate: String, query: String): MatchResult? {
        // 1. Предварительно очищаем обе строки от спецсимволов перед сравнением
        val cleanedCandidate = cleanString(candidate)
        val cleanedQuery = cleanString(query)

        // 2. Дальнейшая логика работает уже с очищенными строками
        val w1 = cleanedCandidate.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val w2 = cleanedQuery.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

        val diff = abs(w1.size - w2.size)
        if (diff > 1) return null

        val smaller = if (w1.size <= w2.size) w1 else w2
        val larger = if (w1.size <= w2.size) w2 else w1

        val usedInLarger = BooleanArray(larger.size)
        var exactCount = 0
        var totalLev = 0

        // Сопоставляем каждое слово из меньшей строки со словом из большей
        for (sWord in smaller) {
            // Сначала ищем точное совпадение
            var matchedIdx = -1
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
                // Если точного нет, ищем слово с отличием максимум в 1 символ
                for (j in larger.indices) {
                    if (!usedInLarger[j]) {
                        val lev = levenshtein(sWord, larger[j])
                        if (lev <= 1) {
                            // Если слова отличаются, они не должны содержать цифры/римские числа
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
                    return null // Слово не сопоставилось вообще
                }
            }
        }

        // Обязательное условие: хотя бы одно слово должно совпасть на 100%
        if (exactCount < 1) return null

        // Если в большей строке осталось лишнее слово, проверяем его
        if (larger.size > smaller.size) {
            val extraIdx = usedInLarger.indexOf(false)
            if (extraIdx != -1) {
                val extraWord = larger[extraIdx]
                if (isForbiddenWord(extraWord)) {
                    return null // Лишнее слово оказалось цифрой/числом
                }
            }
        }

        return MatchResult(wordCountDiff = diff, totalLevenshtein = totalLev)
    }

    /**
     * Проверяет строки на соответствие правилам и возвращает детали совпадения.
     */
    fun checkMatch(candidate: String, query: String): MatchResult? {
        // Разбиваем на слова по пробелам и приводим к нижнему регистру
        val w1 = candidate.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val w2 = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

        val diff = abs(w1.size - w2.size)
        if (diff > 1) return null

        val smaller = if (w1.size <= w2.size) w1 else w2
        val larger = if (w1.size <= w2.size) w2 else w1

        val usedInLarger = BooleanArray(larger.size)
        var exactCount = 0
        var totalLev = 0

        // Сопоставляем каждое слово из меньшей строки со словом из большей
        for (sWord in smaller) {
            // 1. Сначала ищем точное совпадение
            var matchedIdx = -1
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
                // 2. Если точного нет, ищем слово с отличием максимум в 1 символ
                for (j in larger.indices) {
                    if (!usedInLarger[j]) {
                        val lev = levenshtein(sWord, larger[j])
                        if (lev <= 1) {
                            // Если слова отличаются, они не должны содержать цифры/римские числа
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
                    return null // Слово не сопоставилось вообще
                }
            }
        }

        // Обязательное условие: хотя бы одно слово должно совпасть на 100%
        if (exactCount < 1) return null

        // 3. Если в большей строке осталось лишнее слово, проверяем его
        if (larger.size > smaller.size) {
            val extraIdx = usedInLarger.indexOf(false)
            if (extraIdx != -1) {
                val extraWord = larger[extraIdx]
                if (isForbiddenWord(extraWord)) {
                    return null // Лишнее слово оказалось цифрой/числом
                }
            }
        }

        return MatchResult(wordCountDiff = diff, totalLevenshtein = totalLev)
    }

    /**
     * Проверяет, является ли слово "запрещенным" (содержит арабские цифры или римские числа)
     */
    fun isForbiddenWord(word: String): Boolean {
        val clean = word.lowercase().trim('.', ',', '-', ' ')

        // Содержит обычные цифры (0-9)
        if (clean.any { it.isDigit() }) return true

        // Содержит римские цифры (включая окончания типа "ii-я", "iv-й")
        // Проверка идет только по латинским буквам, русская "и" или "в" сюда не попадут
        val romanRegex = Regex("^[ivxlcdm]+(-[а-яa-z]+)?$")
        return romanRegex.matches(clean)
    }

    /**
     * Расстояние Левенштейна (простая реализация)
     */
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




}