package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.numberedstreet

enum class GenType {MAIN, LANE, DEAD_END}

fun geName(mainNum: Int, secNum: Int, type: GenType): String {
    val kaFull =
        when (type) {
            GenType.MAIN -> {
                if (mainNum == 1) "1-ლი ქუჩა" else if (mainNum < 21)"მე-$mainNum ქუჩა" else "$mainNum-ე ქუჩა"
            }
            GenType.LANE -> {
                val kaMainStr =
                    if (mainNum == 1) "1-ლი ქუჩის" else if (mainNum < 21) "მე-$mainNum ქუჩის" else "$mainNum-ე ქუჩის"
                "$kaMainStr ${toRoman(secNum)} შესახვევი"
            }
            GenType.DEAD_END -> {
                val kaMainStr =
                    if (mainNum == 1) "1-ლი ქუჩის" else if (mainNum < 21) "მე-$mainNum ქუჩის" else "$mainNum-ე ქუჩის"
                "$kaMainStr ${toRoman(secNum)} ჩიხი"
            }
        }
    return kaFull
}

fun enName(mainNum: Int, secNum: Int, type: GenType): String {
    val enMainSuffix = getEnglishOrdinalSuffix(mainNum)
    val enSecSuffix = getEnglishOrdinalSuffix(secNum)
    val enFull =
        when (type) {
            GenType.MAIN -> "$mainNum$enMainSuffix Street"
            GenType.LANE -> "$mainNum$enMainSuffix Street's $secNum$enSecSuffix Lane"
            GenType.DEAD_END -> "$mainNum$enMainSuffix Street's $secNum$enSecSuffix Dead End"
        }
    return enFull
}

fun ruName(mainNum: Int, secNum: Int, type: GenType): String {
    val ruMainStr = "$mainNum-я улица"
    val ruMainGenitive = "$mainNum-й улицы"
    val ruFull =
        when (type) {
            GenType.MAIN -> ruMainStr
            GenType.LANE -> "$secNum-й переулок $ruMainGenitive"
            GenType.DEAD_END -> "$secNum-й тупик $ruMainGenitive"
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