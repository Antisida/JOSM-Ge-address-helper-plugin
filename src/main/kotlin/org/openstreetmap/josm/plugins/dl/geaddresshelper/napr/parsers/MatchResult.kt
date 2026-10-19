package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

data class MatchResult(
    val firstLength: Int,            // количество букв 1 аргумента
    val secondLength: Int,            // количество букв 2 аргумента
    val firstWordCount: Int,            // количество слов 1 аргумента
    val secondWordCount: Int,           // количество слов 2 аргумента
    val wordCountNotMatch: Int,     // количество не совпавших слов
    val wordExactMatch: Int,        // количество совпавших слов
    val wordFuzzyMatch: Int,        // количество не четко совпавших слов
    val totalLevenshtein: Int,
    val flags: List<MatchFlag>
) : Comparable<MatchResult> {
    /** Логика такая, что нам интересны только строки, равные или большие по количеству слов на 1 */
    fun isMatched(): Boolean {
        //нам интересны только строки, равные или большие по количеству слов на 1
        if (firstWordCount - secondWordCount !in 0..1) return false
        // Обязательно должно быть хотя бы 2 совпадения (статусная часть и еще что-нибудь)
        if (wordExactMatch + wordFuzzyMatch < 2) return false
        if (flags.contains(MatchFlag.ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT)) return false
        return true
    }

    /** Меньшее - наиболее совпавшее  */
    override fun compareTo(other: MatchResult): Int {
        val comparator = compareBy<MatchResult> { it.wordCountNotMatch }
            .thenBy { it.flags.size }
            .thenBy { it.totalLevenshtein }
        return comparator.compare(this, other)
    }
}

enum class MatchFlag {
    ABBR, // сокращение
    DIFF_MORE_THEN_ONE_WORD,
    ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT, // разница в одно слово и оно содержит числа
    DIFF_LEV,
//    MATCHED_WORD_LESS_THEN_TWO,
}
