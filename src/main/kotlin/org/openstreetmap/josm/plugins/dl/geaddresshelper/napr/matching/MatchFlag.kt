package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.matching

enum class MatchFlag {
    ABBR, // сокращение
    ONLY_ONE_DIFF_WORD_CONTAINS_DIGIT, // разница в одно слово и оно содержит числа
}