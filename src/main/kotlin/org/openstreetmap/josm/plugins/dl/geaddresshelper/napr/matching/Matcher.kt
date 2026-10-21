package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching


class Matcher(private val first: String, private val second: String) {

    var res: MatchResult? = null


    fun match(): MatchResult {
        val flags: MutableList<MatchFlag> = mutableListOf()
        if (listOf(first, second).any { containDigit(it) }) {
            flags.add(MatchFlag.CONTAINS_DIGIT)
        }

        val firstWordList: List<String> = first.clean()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        val secondWordList: List<String> = second.clean()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }

        val fWordList: List<MutablePair<String, MatchStatus>> = firstWordList
            .map { MutablePair(it, MatchStatus.NOT_MATCHED) }
        val sWordList: List<MutablePair<String, MatchStatus>> = secondWordList
            .map { MutablePair(it, MatchStatus.NOT_MATCHED) }

        var totalLev = 0
        for (f in fWordList) {
            //ищем полное совпадение
            if (f.second == MatchStatus.NOT_MATCHED)
                for (s in sWordList) {
                    if (s.second == MatchStatus.NOT_MATCHED)
                        if (f == s) {
                            f.second = MatchStatus.MATCHED
                            s.second = MatchStatus.MATCHED
                        }
                }
            //ищем частичное совпадение
            if (f.second == MatchStatus.NOT_MATCHED) {
                for (s in sWordList) {
                    if (s.second == MatchStatus.NOT_MATCHED) {
                        // только для строк без цифт. исключаем нумерованные улицы
                        if (!containDigit(f.first) && !containDigit(s.first)) {
                            val lev = levenshtein(f.first, s.first)
                            if (lev <= 2) {
                                f.second = MatchStatus.FUZZY_MATCHED
                                s.second = MatchStatus.FUZZY_MATCHED
                                totalLev++
                            }
                        }
                    }
                }
            }
        }

        // 4. Если кандидат длиннее на 1 слово, проверяем лишнее слово на "запрещенность" (не должно содержать цифры) случай с В Ленина и Ленина
        if (fWordList.size - sWordList.size == 1 || sWordList.size - fWordList.size == 1) {

            val fNotMatch = fWordList.filter { it.second == MatchStatus.NOT_MATCHED }
            val sNotMatch = sWordList.filter { it.second == MatchStatus.NOT_MATCHED }

            val notMatchWord = listOf(fNotMatch.size, sNotMatch.size).max()
            if (notMatchWord == 1) {
                if (listOf(fNotMatch, sNotMatch).flatten().any { containDigit(it.first) }) {
                    flags.add(MatchFlag.ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT)
                }
            }
        }
        return MatchResult(fWordList, sWordList, flags, totalLev)
    }


    val romanDigits = setOf('i', 'v', 'x', 'l', 'I', 'V', 'X', 'L'/*, 'C', 'D', 'M'*/)

    private fun containDigit(word: String): Boolean {
        if (word.any { it.isDigit() }) return true
        return word.any { it in romanDigits }
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


fun String.longerThen(string: String): Boolean {
    return this.clean().length > string.clean().length
}

fun String.shorterThen(string: String): Boolean {
    return this.clean().length < string.clean().length
}

fun String.clean(): String {
    return this.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim()
}