package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers


class Matcher(private val first: String, private val second: String) : Comparable<Matcher> {
    //    private var _firstLength: Int? = null           // количество букв 1 аргумента
//    private var _secondLength: Int? = null      // количество букв 2 аргумента
//    private var _firstWordCount: Int? = null       // количество слов 1 аргумента
//    private var _secondWordCount: Int? = null       // количество слов 2 аргумента
//    private var _wordCountNotMatch: Int? = null   // количество не совпавших слов
//    private var _wordExactMatch: Int? = null   // количество совпавших слов
//    private var _wordFuzzyMatch: Int? = null   // количество не четко совпавших слов
//    private var _totalLevenshtein: Int? = null
//    private var _flags: MutableList<MatchFlag> = mutableListOf()
//
    private var details: MatchDetails? = null

    /** Логика такая, что нам интересны только строки, равные или большие по количеству слов на 1 */
    fun isMatched(): Boolean {
        requireNotNull(details) { "ParsedAddress cannot be null while creating building's tags" }
        //нам интересны только строки, равные или большие по количеству слов на 1
        if (details!!.firstWordCount - details!!.secondWordCount !in 0..1) return false
        // Обязательно должно быть хотя бы 2 совпадения (статусная часть и еще что-нибудь)
        if (details!!.wordExactMatch + details!!.wordFuzzyMatch < 2) return false
        if (details!!.flags.contains(MatchFlag.ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT)) return false
        return true
    }


    fun match(): Matcher {
        val cleanedFirst: String = cleanString(first)
        val cleanedSecond: String = cleanString(second)

        val firstWordList: List<String> = cleanedFirst.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() } // Кандидат
        val secondWordList: List<String> = cleanedSecond.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }    // Запрос

        val usedInFirst = BooleanArray(firstWordList.size)   //индексы слов в larger, для которых нашлось совпадение
        val usedInSecond = BooleanArray(secondWordList.size) //индексы слов в larger, для которых нашлось совпадение
        var exactCount = 0 //счетчик точных совпадений
        var fuzzyCount = 0 //счетчик неточных совпадений
        var totalLev = 0

        // Сопоставляем каждое слово из запроса (smaller) со словом из кандидата (larger)
        for (sWord in secondWordList) {
            // 1. Ищем точное совпадение
            val idx: Int = getIdx(sWord, firstWordList, usedInFirst)
            if (idx != -1) {
                usedInFirst[idx] = true
                usedInSecond[secondWordList.indexOf(sWord)] = true
                exactCount++
            } else {
                // 2. Если точного нет, ищем с опечаткой в 1 символ
                val maxLev = 2
                val idxl: Int = getIdx(sWord, firstWordList, usedInFirst, maxLev)
                if (idxl != -1) {
                    fuzzyCount++
                    totalLev += maxLev //fixme тут верно пока значение 1
                    usedInFirst[idxl] = true
                    usedInSecond[secondWordList.indexOf(sWord)] = true
                }
            }
        }
        val flags: MutableList<MatchFlag> = mutableListOf()
        val wordCountMatch = usedInSecond.count { it }

        val wordCountNotMatch =
            if (firstWordList.size > secondWordList.size) firstWordList.size - wordCountMatch
            else secondWordList.size - wordCountMatch

        // 3. количество слов одинаковое, только одно слово не совпало - проверяем не сокращение ли это
        // не используется. потому что избыточно: если larger.size - exactCount == 1 считаем что совпало
        /*        if (larger.size == smaller.size) {
                    if (larger.size - exactCount == 1) {
                        val idxSmallerNotFound = usedInSmoller.indexOf(false) //индекс не найденное слово в коротком
                        val idxLargerNotFound = usedInLarger.indexOf(false)
                        if (idxSmallerNotFound != -1 && idxLargerNotFound != -1) {
                            val smallerNotFound = smaller[idxSmallerNotFound]
                            val largerNotFound = larger[idxLargerNotFound]
                            if (largerNotFound.startsWith(smallerNotFound)
                                || smallerNotFound.startsWith(largerNotFound)
                            ) {
                                flags.add(MatchFlag.ABBR)
                            }
                        }
                    }
                }*/

        // 4. Если кандидат длиннее на 1 слово, проверяем лишнее слово на "запрещенность" (не должно содержать цифры) случай с В Ленина и Ленина
        if (firstWordList.size > secondWordList.size) {
            val extraIdx = usedInFirst.indexOf(false)
            if (extraIdx != -1) {
                val extraWord = firstWordList[extraIdx]
                if (containDigit(extraWord)) {
                    flags.add(MatchFlag.ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT)
                }
            }
        }
        details = MatchDetails(
            cleanedFirst.length,
            cleanedSecond.length,
            firstWordList.size,
            secondWordList.size,
            wordCountNotMatch,
            exactCount,
            fuzzyCount,
            totalLev,
            flags
        )
        return this
    }

    private fun cleanString(text: String): String {
        return text.filter { it.isLetterOrDigit() || it.isWhitespace() }
    }

    private fun getIdx(string: String, list: List<String>, usedInLarger: BooleanArray): Int {
        for (j in list.indices) {
            if (!usedInLarger[j] && string == list[j]) {
                return j
            }
        }
        return -1
    }

    private fun getIdx(string: String, list: List<String>, usedInLarger: BooleanArray, maxLev: Int): Int {
        for (j in list.indices) {
            if (!usedInLarger[j]) {
                val lev = levenshtein(string, list[j])
                if (lev <= maxLev) { // эту проверку надо вынести из этого метода наружу
                    // Запрещаем опечатки в цифрах/числах
                    if (containDigit(string) || containDigit(list[j])) {
                        break
                    }
                    return j
                }
            }
        }
        return -1
    }

    val romanDigits = setOf('I', 'V', 'X', 'L'/*, 'C', 'D', 'M'*/)

    private fun containDigit(word: String): Boolean {
        val clean = word.uppercase()//.trim('.', ',', '-', ' ')
        if (clean.any { it.isDigit() }) return true

        return clean.any { it in romanDigits }
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

    fun isFirstLongerThanSecond(): Boolean {
        requireNotNull(details) { "ParsedAddress cannot be null while creating building's tags" }
        return details!!.firstLength > details!!.secondLength
    }

    /** Меньшее - наиболее совпавшее  */
    override fun compareTo(other: Matcher): Int {
        val comparator = compareBy<Matcher> { it.details }
        return comparator.compare(this, other)
    }

}