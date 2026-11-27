package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching

import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.STREET_STATUS_AND_ABBR_SET

data class MatchResult(
    val fWordList: List<MutablePair<String, MatchStatus>>,
    val sWordList: List<MutablePair<String, MatchStatus>>,
    val flags: MutableList<MatchFlag>,
    val totalLevenshtein: Int
) : Comparable<MatchResult> {
    /** Логика такая, что нам интересны только строки, равные или большие по количеству слов на 1 */
    fun isMatched(): Boolean {
        // если есть цифры - то должно быть полное совпадение
        if (flags.contains(MatchFlag.CONTAINS_DIGIT) && getNotExactMatchWordCount() > 0) return false
        //нам интересны только строки, равные или большие по количеству слов на 1
        if (fWordList.size - sWordList.size !in -1..1) return false
        // должно быть не более 1 отличающегося слова
        if (getNotMatchWordCount() > 1) return false
        // Обязательно должно быть хотя бы 2 совпадения (статусная часть и еще что-нибудь)
        if (getMatchWordCount() < 2) return false
        // улица Ивана Иванова и переулок Ивана Иванова
        if (getNotExactMatchWordCount() == 1 && STREET_STATUS_AND_ABBR_SET.contains(getNotMatchWord()[0])) return false
        return true
    }

    private fun getNotMatchWord() : List<String> {
        return fWordList.filter { it.second != MatchStatus.MATCHED }
            .map { it.first}
    }

    /** @return количество не совпавших слов */
    private fun getNotMatchWordCount(): Int {
        return listOf(
            fWordList.count { it.second == MatchStatus.NOT_MATCHED },
            sWordList.count { it.second == MatchStatus.NOT_MATCHED })
            .max()
    }

    /** @return количество совпавших слов, включая FUZZY_MATCHED */
    private fun getMatchWordCount(): Int {
        return fWordList.count { it.second != MatchStatus.NOT_MATCHED }
    }

    /** @return количество не совпавших полностью слов */
    private fun getNotExactMatchWordCount(): Int {
        return fWordList.count { it.second != MatchStatus.MATCHED  }
    }

    /** Меньшее - наиболее совпавшее  */
    override fun compareTo(other: MatchResult): Int {
        val comparator = compareBy<MatchResult> { it.getNotMatchWordCount() }
            .thenBy { it.flags.size }
            .thenBy { it.totalLevenshtein }
        return comparator.compare(this, other)
    }


}
