package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.validation.vocabulary

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.min

object Vocabulary {
    private const val FILE_NAME = "street-list/georgian_streets.csv"

    val usersList: List<StreetTranslate> by lazy {
        val inputStream = javaClass.classLoader.getResourceAsStream(FILE_NAME)
            ?: throw IllegalArgumentException("File $FILE_NAME not found in classpath!")

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
            lines.drop(1) // Пропускаем заголовок
                .map { line ->
                    val tokens = line.split(",")
                    StreetTranslate(
                        name = tokens[0].trim(),
                        nameKa = tokens[1].trim(),
                        nameEn = tokens[2].trim(),
                        nameRu = tokens[3].trim()
                    )
                }.toList()
        }
    }

    fun getByName(name: String): StreetTranslate? {
        return usersList.find { it.name.equals(name, ignoreCase = true) }
    }

    fun isExists(name: String): Boolean {
        return usersList.any { it.name.equals(name, ignoreCase = true) }
    }

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
    fun findBestAddress(target: String, maxDistancePerWord: Int = 1): String? {
        if (usersList.isEmpty()) return null

        // Разбиваем входящий запрос на очищенные слова
        val targetWords = target.lowercase().split(Regex("(?U)\\s+|,|\\.")).filter { it.length > 1 } // Игнорируем предлоги/буквы типа "д", "к"
        if (targetWords.isEmpty()) return null

        return usersList
            .map { it.name.lowercase() }
            .map { candidate ->
                val candidateWords = candidate.lowercase().split(Regex("(?U)\\s+|,|\\.")).filter { it.length > 1 }
                var matchedWords = 0

                // Считаем, сколько слов из запроса нашлось в кандидате
                for (tWord in targetWords) {
                    for (cWord in candidateWords) {
                        if (abs(tWord.length - cWord.length) <= maxDistancePerWord) {
                            if (levenshteinDistance(tWord, cWord) <= maxDistancePerWord) {
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
            .filter { it.second >= 0.7 }
            // Сортируем: сначала те, где совпало больше слов
            .maxByOrNull { it.second }
            ?.first
    }
}