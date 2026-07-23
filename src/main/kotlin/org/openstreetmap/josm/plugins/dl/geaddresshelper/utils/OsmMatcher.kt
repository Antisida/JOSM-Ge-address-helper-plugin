/*
package org.openstreetmap.josm.plugins.dl.geaddresshelper.utils

import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.napr.MatchScore
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.napr.getOsmStreetName
import kotlin.math.abs

class OsmMatcher {


    fun findBestMatchingAddress(query: String, dataSet: DataSet) : String? {
        return findBestMatchingAddress(query, getOsmStreetName(dataSet))
    }

    */
/**
     * Ищет наиболее близкую строку из списка на основе весов MatchScore.
     *//*

    fun findBestMatchingAddress(query: String, list: List<String>): String? {
        return list
            .mapNotNull { candidate ->
                val score = getMatchScore(candidate, query)
                if (score != null) candidate to score else null
            }
            .maxByOrNull { it.second } // Находит пару с максимальным (наилучшим) MatchScore
            ?.first
    }

    */
/**
     * Вычисляет оценку схожести между кандидатом и запросом.
     * Возвращает null, если строки не соответствуют базовым правилам.
     *//*

    fun getMatchScore(candidate: String, query: String): MatchScore? {
        val candWords = normalize(candidate)
        val queryWords = normalize(query)

        // Базовое правило: разница в количестве слов не более 1
        if (abs(candWords.size - queryWords.size) > 1) {
            return null
        }

        val (smaller, larger) = if (candWords.size <= queryWords.size) {
            candWords to queryWords
        } else {
            queryWords to candWords
        }

        return calculateBestAlignment(smaller, larger)
    }

    */
/**
     * Перебором находит наилучший вариант сопоставления слов меньшей строки с большей.
     *//*

    private fun calculateBestAlignment(sWords: List<String>, lWords: List<String>): MatchScore? {
        val n = sWords.size
        val m = lWords.size
        val usedInLarger = BooleanArray(m)

        var bestScore: MatchScore? = null

        fun search(sIdx: Int, currentExact: Int, currentLev: Int, currentInitials: Int) {
            if (sIdx == n) {
                // Базовое правило: должно быть хотя бы одно точное совпадение слова
                if (currentExact >= 1) {
                    val score = MatchScore(
                        exactMatchesCount = currentExact,
                        wordCountDiff = m - n,
                        totalLevenshtein = currentLev,
                        initialMatchesCount = currentInitials
                    )
                    if (bestScore == null || score > bestScore!!) {
                        bestScore = score
                    }
                }
                return
            }

            val sWord = sWords[sIdx]
            for (j in 0 until m) {
                if (!usedInLarger[j]) {
                    val lWord = lWords[j]

                    val exact = sWord == lWord
                    val isAbbrev = isInitial(sWord, lWord)
                    val lev = if (exact || isAbbrev) 0 else levenshtein(sWord, lWord)

                    // Слово подходит под условия
                    if (exact || isAbbrev || lev <= 1) {
                        usedInLarger[j] = true
                        search(
                            sIdx + 1,
                            currentExact + if (exact) 1 else 0,
                            currentLev + lev,
                            currentInitials + if (isAbbrev) 1 else 0
                        )
                        usedInLarger[j] = false // Backtrack
                    }
                }
            }
        }

        search(0, 0, 0, 0)
        return bestScore
    }

    private fun normalize(text: String): List<String> {
        return text.lowercase()
            .split(Regex("\\s+"))
            .map { it.trim('.', ',', ' ', ';', ':') }
            .filter { it.isNotEmpty() }
    }

    private fun isInitial(w1: String, w2: String): Boolean {
        if (w1.length == 1 && w2.isNotEmpty()) return w2.startsWith(w1)
        if (w2.length == 1 && w1.isNotEmpty()) return w1.startsWith(w2)
        return false
    }

    private fun levenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                if (s1[i - 1] == s2[j - 1]) {
                    dp[j] = prev
                } else {
                    dp[j] = minOf(dp[j], dp[j - 1], prev) + 1
                }
                prev = temp
            }
        }
        return dp[s2.length]
    }



}*/
