package org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.napr
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.min

class LevTest {



    // Расстояние Левенштейна (остается прежним)
    fun levenshteinDistance(s1: String, s2: String): Int {
        val memo = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prevPrev = memo[0]
            memo[0] = i
            for (j in 1..s2.length) {
                val prev = memo[j]
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                memo[j] = min(min(memo[j] + 1, memo[j - 1] + 1), prevPrev + cost)
                prevPrev = prev
            }
        }
        return memo[s2.length]
    }

    // Функция поиска лучшего адреса
    fun List<String>.findBestAddress(target: String, maxLevenshteinDistancePerWord: Int = 1): String? {
        if (this.isEmpty()) return null

        // Разбиваем входящий запрос на очищенные слова
        val targetWords = target.lowercase().split(Regex("\\s+|,|\\.")).filter { it.length > 1 } // Игнорируем предлоги/буквы типа "д", "к"
        if (targetWords.isEmpty()) return null

        return this
            .map { candidate ->
                val candidateWords = candidate.lowercase().split(Regex("\\s+|,|\\.")).filter { it.length > 1 }
                var matchedWords = 0

                // Считаем, сколько слов из запроса нашлось в кандидате
                for (tWord in targetWords) {
                    for (cWord in candidateWords) {
                        if (abs(tWord.length - cWord.length) <= maxLevenshteinDistancePerWord) {
                            if (levenshteinDistance(tWord, cWord) <= maxLevenshteinDistancePerWord) {
                                matchedWords++
                                break
                            }
                        }
                    }
                }
                // Сохраняем кандидата и долю совпавших слов (от 0.0 до 1.0)
                candidate to (matchedWords.toDouble() / targetWords.size)
            }
            // Оставляем только те строки, где совпало хотя бы 70% слов из запроса
            .filter { it.second >= 0.5 }
            // Сортируем: сначала те, где совпало больше слов
            .maxByOrNull { it.second }?.first
    }
    @Test
    fun main() {
        val addresses =
            listOf(
            "დავით კლდიაშვილის",
            "ვახტანგ გორგასლის",
            "თამარ მეფის",
            "ილიას"
        )

        val input = "კლდიაშვილი ff dd"

        val result = addresses.findBestAddress(input, maxLevenshteinDistancePerWord = 1)

        println("Ввод: '$input'")
        println("Результат: '$result'")
        // Выведет: Улица Карла Маркса
    }

    fun removeStreetKeywords(input: String, keywords: List<String>): String {
        if (input.isBlank() || keywords.isEmpty()) return input

        // Экранируем спецсимволы на случай, если в списке будут точки (н-р, "ул.")
        // и объединяем через "или" (|)
        val escapedKeywords = keywords.joinToString("|") { Regex.escape(it) }

        // (?Ui) -> U для поддержки Unicode границ слов, i для регистронезависимости
        // \s* по краям подхватывают лишние пробелы, чтобы не оставалось двойных
        val regex = Regex("(?Ui)\\s*\\b($escapedKeywords)\\b\\s*")

        return input
            .replace(regex, " ") // Заменяем найденное слово с пробелами на один пробел
            .trim()              // Убираем пробелы по краям, если слово было в начале или конце
            .replace(Regex("\\s+"), " ") // Схлопываем случайные двойные пробелы внутри
    }

    @Test
    fun main1() {
        val keywords = listOf("улица", "переулок", "проезд")

        val testCases = listOf(
            "улица Ленина",
            "Переулок Сталина",
            "1-я улица ленина",
            "радужная улица",
            "проезд Знаменщиков средний"
        )

        testCases.forEach { case ->
            println("\"$case\" -> \"${removeStreetKeywords(case, keywords)}\"")
        }
    }
}