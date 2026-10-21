package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching

enum class MatchFlag {
    ABBR, // сокращение
    CONTAINS_DIGIT, // разница в одно слово и оно содержит числа
    ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT,
}