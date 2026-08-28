package org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator

enum class StreetType {
    MAIN, // улица
    LANE, // переулок
    DEAD_END // тупик
}

fun geName(mainNum: Int, secNum: Int, type: StreetType): String {
    val kaFull =
        when (type) {
            StreetType.MAIN -> if (mainNum == 1) "1-ლი ქუჩა" else if (mainNum < 21) "მე-$mainNum ქუჩა" else "$mainNum-ე ქუჩა"
            StreetType.LANE -> {
                val kaMainStr = if (mainNum == 1) "1-ლი ქუჩის" else if (mainNum < 21) "მე-$mainNum ქუჩის" else "$mainNum-ე ქუჩის"
                "$kaMainStr ${toRoman(secNum)} შესახვევი"
            }

            StreetType.DEAD_END -> {
                val kaMainStr = if (mainNum == 1) "1-ლი ქუჩის" else if (mainNum < 21) "მე-$mainNum ქუჩის" else "$mainNum-ე ქუჩის"
                "$kaMainStr ${toRoman(secNum)} ჩიხი"
            }
        }
    return kaFull
}

fun enName(mainNum: Int, secNum: Int, type: StreetType): String {
    val enMainSuffix = getEnglishOrdinalSuffix(mainNum)
    val enSecSuffix = getEnglishOrdinalSuffix(secNum)
    val enFull =
        when (type) {
            StreetType.MAIN -> "$mainNum$enMainSuffix Street"
            StreetType.LANE -> "$mainNum$enMainSuffix Street's $secNum$enSecSuffix Lane"
            StreetType.DEAD_END -> "$mainNum$enMainSuffix Street's $secNum$enSecSuffix Dead End"
        }
    return enFull
}

fun ruName(mainNum: Int, secNum: Int, type: StreetType): String {
    val ruMainStr = "$mainNum-я улица"
    val ruMainGenitive = "$mainNum-й улицы"
    val ruFull =
        when (type) {
            StreetType.MAIN -> ruMainStr
            StreetType.LANE -> "$secNum-й переулок $ruMainGenitive"
            StreetType.DEAD_END -> "$secNum-й тупик $ruMainGenitive"
        }
    return ruFull
}

private fun getEnglishOrdinalSuffix(n: Int): String {
    if (n % 100 in 11..13) return "th"
    return when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

private fun toRoman(number: Int): String {
    val romanNumerals =
        listOf(
//            1000 to "M",
//            900 to "CM",
//            500 to "D",
//            400 to "CD",
//            100 to "C",
//            90 to "XC",
            50 to "L",
            40 to "XL",
            10 to "X",
            9 to "IX",
            5 to "V",
            4 to "IV",
            1 to "I",
        )
    var n = number
    val result = StringBuilder()
    for ((value, numeral) in romanNumerals) {
        while (n >= value) {
            result.append(numeral)
            n -= value
        }
    }
    return result.toString()
}