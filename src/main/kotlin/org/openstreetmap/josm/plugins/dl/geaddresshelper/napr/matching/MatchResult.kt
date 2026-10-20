package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching

data class MatchResult(
    val fWordList: List<MutablePair<String, MatchStatus>>,
    val sWordList: List<MutablePair<String, MatchStatus>>,
    val flags: MutableList<MatchFlag>,
    val totalLevenshtein: Int
) : Comparable<MatchResult> {
    /** Логика такая, что нам интересны только строки, равные или большие по количеству слов на 1 */
    fun isMatched(): Boolean {
        //нам интересны только строки, равные или большие по количеству слов на 1
        if (fWordList.size - sWordList.size !in -1..1) return false
        // должно быть не не более 1 отличающегося слова
        if (getNotMatchWordCount() > 1) return false
        // Обязательно должно быть хотя бы 2 совпадения (статусная часть и еще что-нибудь)
        if (getMatchWordCount() < 2) return false
        if (flags.contains(MatchFlag.ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT)) return false
        return true
    }

    /** @return количество не совпавших слов */
    private fun getNotMatchWordCount(): Int {
        return listOf(
            fWordList.count { it.second == MatchStatus.NOT_MATCHED },
            sWordList.count { it.second == MatchStatus.NOT_MATCHED })
            .max()
    }

    /** @return количество не совпавших слов */
    private fun getMatchWordCount(): Int {
        return fWordList.count { it.second != MatchStatus.NOT_MATCHED }
    }

    /** Меньшее - наиболее совпавшее  */
    override fun compareTo(other: MatchResult): Int {
        val comparator = compareBy<MatchResult> { it.getNotMatchWordCount() }
            .thenBy { it.flags.size }
            .thenBy { it.totalLevenshtein }
        return comparator.compare(this, other)
    }


}
