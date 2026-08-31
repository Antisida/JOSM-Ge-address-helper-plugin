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

data class NumData(
    val type: StreetType,
    val mainNum: Int,
    val secNum: Int? // null для MAIN типа
)

fun parseData(address: String): NumData? {
    val text = address.trim()

    // Определяем тип улицы по концу строки
    val type = when {
        text.endsWith("შესახვევი") -> StreetType.LANE
        text.endsWith("ჩიხი") -> StreetType.DEAD_END
        text.endsWith("ქუჩა") -> StreetType.MAIN
        else -> return null // Неизвестный формат
    }

    // Извлекаем римскую цифру (для LANE и DEAD_END)
    var secNum: Int? = null
    if (type != StreetType.MAIN) {
        secNum = getSecondaryNum(text) ?: return null
    }

    // Извлекаем основной номер (mainNum)
    var mainNum: Int
    if (type == StreetType.MAIN) {
        // Паттерн для MAIN: ищем число и "-ლი", "-ე" или "მე-"
        val mainRegex = """(?U)(?:მე-(\d+)|(\d+)(?:-ლი|-ე))""".toRegex()
        val match = mainRegex.find(text)
        if (match == null) return null
        mainNum = match.groupValues[1].ifEmpty { match.groupValues[2] }.toInt()
    } else {
        // Для LANE и DEAD основной номер стоит перед "ქუჩის"
        val laneRegex = """(?U)(?:მე-(\d+)|(\d+)(?:-ლი|-ე))\s*ქუჩის""".toRegex()
        val match = laneRegex.find(text)
        if (match == null) return null
        mainNum = match.groupValues[1].ifEmpty { match.groupValues[2] }.toInt()
    }

    return NumData(type, mainNum, secNum)
}

private fun getSecondaryNum(text: String): Int? {
    // Ищем римское число перед ключевым словом типа (შესახვევი / ჩიხი)
    val romanRegex = """(?U)([IVXL]+)\s*(?:შესახვევი|ჩიხი)""".toRegex()
    val match = romanRegex.find(text)
    return match?.groupValues?.get(1)
        ?.let(::romanToInt)
        ?.takeIf { it > 0 }
}

private fun romanToInt(roman: String): Int {
    val values = mapOf(
        'I' to 1, 'V' to 5, 'X' to 10, 'L' to 50
//        ,'C' to 100, 'D' to 500, 'M' to 1000
    )

    var result = 0
    var prevValue = 0

    // Проходим справа налево для корректной обработки вычитания (IV, IX)
    for (i in roman.length - 1 downTo 0) {
        val char = roman[i]
        val value = values[char] ?: return -1 // Ошибка формата

        if (value < prevValue) {
            result -= value
        } else {
            result += value
        }
        prevValue = value
    }
    return result
}