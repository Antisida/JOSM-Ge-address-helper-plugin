package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers

data class MatchDetails(
    val firstLength: Int,          // количество букв 1 аргумента
    val secondLength: Int,      // количество букв 2 аргумента
    val firstWordCount: Int,       // количество слов 1 аргумента
    val secondWordCount: Int,       // количество слов 2 аргумента
    val wordCountNotMatch: Int,   // количество не совпавших слов
    val wordExactMatch: Int,   // количество совпавших слов
    val wordFuzzyMatch: Int,   // количество не четко совпавших слов
    val totalLevenshtein: Int,
    val flags: List<MatchFlag>
): Comparable<MatchDetails> {

    /** Меньшее - наиболее совпавшее  */
    override fun compareTo(other: MatchDetails): Int {
        val comparator = compareBy<MatchDetails> { it.wordCountNotMatch }
            .thenBy { it.flags.size }
            .thenBy { it.totalLevenshtein }
        return comparator.compare(this, other)
    }
}
