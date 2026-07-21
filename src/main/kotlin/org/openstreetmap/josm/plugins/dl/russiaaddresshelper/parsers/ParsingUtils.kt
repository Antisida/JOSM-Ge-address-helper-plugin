package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers

import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagCreator.STATUSES_SET
import org.openstreetmap.josm.tools.Logging

data class ParseContext(
    var str: String,
    val flags: List<ParsingFlags> = listOf()
)

/**
 * Применяет трансформацию к строке. Если строка изменилась, добавляет переданный флаг в список.
 */
inline fun ParseContext.modifyAndFlag(
    flag: ParsingFlags,
    transform: (String) -> String
): ParseContext {
    val (text, _) = this
    val newText = transform(text)
    return if (newText != text) {
        // Если текст изменился, возвращаем новую пару с добавленным флагом
        this.copy(str = newText, flags = this.flags + flag)
    } else {
        // Если ничего не поменялось, прокидываем текущее состояние дальше
        this
    }
}

/**
 * Позволяет динамически добавлять несколько разных флагов изнутри логики трансформации.
 */
private inline fun ParseContext.modifyAndFlag(
    transform: (String, MutableList<ParsingFlags>) -> String
): ParseContext {
    // 1. Создаем пустой локальный список для сбора флагов на этом шаге
    val localFlags = mutableListOf<ParsingFlags>()

    // 2. Вызываем переданную логику.
    // В нее уходит текущая строка и наш пустой список, в который вы можете делать .add()
    val newText = transform(this.str, localFlags)

    // 3. Проверяем результат
    return if (newText != this.str) {
        // Если текст реально изменился, клеим накопленные localFlags к основным флагам
        this.copy(str = newText, flags = this.flags + localFlags)
    } else {
        // Если текст не менялся, локальные флаги просто отбрасываются, возвращаем старый контекст
        this
    }
}

/** Расширяет сокращения в названиях грузинских улиц.
 * ParsingFlags.HAS_ABBREVIATIONS
 */
fun ParseContext.expandAbbreviations(vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        availableFlags.getOrNull(0)?.let { addedFlags.add(it) }
        s.replace("""(?Ui)\bქ\.\s*""".toRegex(), "ქუჩა ")
            .replace("""(?Ui)\bგამზ\.\s*""".toRegex(), "გამზირი ")
            .replace("""(?Ui)\bშეს\.\s*""".toRegex(), "შესახვევი ")
    }

/**
 * Удаляет из строки круглые скобки и всё, что находится внутри них.
 * Также очищает лишние двойные пробелы, которые могли образоваться после удаления.
 * ParsingFlags.PARENTHESES_REMOVED
 */
fun ParseContext.removeParenthesesContent(vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        availableFlags.getOrNull(0)?.let { addedFlags.add(it) }
        // Регулярное выражение находит ( и ) и всё между ними (жадный поиск исключая закрывающую скобку)
        val regex = """(?U)\([^)]*\)""".toRegex()

        s.replace(regex, "")
            .replace("""(?U)\s+""".toRegex(), " ") // Схлопываем разбежавшиеся пробелы в один
            .trim()                            // Убираем пробелы по краям, если скобки были в начале/конце
    }

/**
 * Заменяет две запятые, между которыми есть только пробелы (или нет ничего), на одну запятую.
 * Например: ", ," -> ", " или ",," -> ","
 * ParsingFlags.COMMAS_COLLAPSED
 */
fun ParseContext.collapseDuplicateCommas(vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        availableFlags.getOrNull(0)?.let { addedFlags.add(it) }
        // Регулярное выражение ищет: запятую, затем ноль или более пробелов, затем вторую запятую
        val regex = """(?U),\s*,""".toRegex()

        // Заменяем найденное комбо на одну запятую с последующим пробелом для красоты
        s.replace(regex, ", ")
            .replace("""(?U)\s+""".toRegex(), " ") // Схлопываем случайные двойные пробелы
            .trim()
    }

/**
 * удаляет ს после латинского номера
 * ParsingFlags.ROMAN_NUMERAL_CLEANED
 */
fun ParseContext.cleanRomanNumerals(vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        availableFlags.getOrNull(0)?.let { addedFlags.add(it) }
        val regex = """(?U)\b(I|II|III|IV|V|VI|VII|VIII|IX|X)ს\b""".toRegex()
        s.replace(regex, "$1")
    }

fun ParseContext.trim(vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        s.trim()
    }

/** Переносит статусную часть в конец, добавляет ს к названию если его там нет */
fun ParseContext.moveStatusToBack(vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        //логика выполняется если содержится только один статус (исключаем 1 переулок 1 улицы)
        if (STATUSES_SET.count { s.contains(it) } > 1) return@modifyAndFlag s

        Logging.info("4: $s")

        val keywordsPattern = STATUSES_SET.joinToString("|") { Regex.escape(it) }
        Logging.info("5: $keywordsPattern")
        // Регулярное выражение ищет статус в начале строки
        val regex = """(?U)^($keywordsPattern)\b\s*(.*)""".toRegex(RegexOption.IGNORE_CASE)

        regex.replace(s) { matchResult ->
            availableFlags.getOrNull(0)?.let { addedFlags.add(it) }
            val keyword = matchResult.groupValues[1]
            val restOfString = matchResult.groupValues[2]
            Logging.info("6: $keyword")
            Logging.info("7: $restOfString")

            if (restOfString.isNotEmpty()) {
                //к римским цифрам не добавляем "ს"
                if (restOfString in listOf("I", "V", "X") || restOfString.endsWith("ს")) {
                    "$restOfString $keyword"
                } else {
                    availableFlags.getOrNull(1)?.let { addedFlags.add(it) }
                    val inGenitive = toGeorgianGenitive(restOfString, true)
                    "${inGenitive} $keyword"
                }
            } else keyword
        }
    }

fun toGeorgianGenitive(noun: String, isProperNoun: Boolean = false): String {
    if (noun.isBlank()) return noun

    val word = noun.trim()
    val lastChar = word.last()

    return when (lastChar) {
        // 1. Оканчивается на -ი (Самая большая группа + фамилии на -швили, -ани, -ури)
        'ი' -> {
            val stem = word.dropLast(1) // отрезаем 'ი'

            // Если это имя собственное (фамилия), корень никогда не усекается
            if (isProperNoun) {
                return stem + "ის" // ჯუღაშვილი -> ჯუღაშვილის, დადიანი -> დადიანის
            }

            // Для нарицательных применяем логику усечения (квеца)
            if (stem.length > 2) {
                val penultimate = stem[stem.length - 2]
                if (penultimate == 'ე' || penultimate == 'ა' || penultimate == 'ო') {
                    // Вырезаем гласную перед последней согласной
                    return stem.substring(0, stem.length - 2) + stem.last() + "ის"
                }
            }
            stem + "ის"
        }

        // 2. Оканчивается на -ა (Фамилии на -иа, -ава и личные имена)
        'ა' -> {
            if (isProperNoun) {
                // Имена и фамилии сохраняют 'ა' (ჟვანია -> ჟვანიას, შოთა -> შოთას)
                word + "ს"
            } else {
                // Нарицательные теряют 'ა' и получают 'ის' (დედა -> დედის)
                word.dropLast(1) + "ის"
            }
        }

        // 3. Оканчивается на -ე (Фамилии на -ძე и личные имена)
        'ე' -> {
            if (isProperNoun) {
                // Фамилии на -ძე усекаются (ჭავჭავაძე -> ჭავჭავაძის)
                if (word.endsWith("ძე")) {
                    word.dropLast(1) + "ის"
                } else {
                    // Личные имена и другие фамилии сохраняют 'ე' (ელენე -> ელენეს)
                    word + "ს"
                }
            } else {
                // Эвристика для нарицательных (خე -> ხის, კაფე -> კაფეს)
                if (word.length <= 4) {
                    word.dropLast(1) + "ის"
                } else {
                    word + "ს"
                }
            }
        }

        // 4. Оканчивается на -ო или -უ
        'ო', 'უ' -> {
            // Для имен собственных в современном языке чаще используется просто 'ს' (ლადო -> ლადოს)
            // Для нарицательных оставляем классическое 'ვის' (წყარო -> წყაროვის)
            if (isProperNoun) word + "ს" else word + "ვის"
        }

        // 5. На случай, если слово передано в виде основы
        else -> word + "ის"
    }
}

fun toGeorgianGenitive(noun: String): String {
    if (noun.isBlank()) return noun

    val word = noun.trim()
    val lastChar = word.last()

    return when (lastChar) {
        // 1. Именительный на -ი (Самая большая группа)
        'ი' -> {
            val stem = word.dropLast(1)
            // Реализация "усечения" (квеца) для основы на -ელ / -ალ / -ონ / -ინ
            // Например: მგელი -> მგლის, ქართველი -> ქართველის, ფანჯარა -> ფანჯრის
            if (stem.length > 2) {
                val penultimate = stem[stem.length - 2]
                if (penultimate == 'ე' || penultimate == 'ა' || penultimate == 'ო') {
                    // Вырезаем эту гласную перед последней согласной
                    return stem.substring(0, stem.length - 2) + stem.last() + "ის"
                }
            }
            stem + "ის"
        }

        // 2. Именительный на -ა
        'ა' -> word + "ს"

        // 3. Именительный на -ე
        'ე' -> {
            // Если это короткое исконное слово (მზე, ხე, მეფე) -> усекается 'ე' и добавляется 'ის'
            // Если длинное заимствование (კაფე, ატელიე) или имя собственное -> просто + 'ს'
            if (word.split(" ").last().length <= 4) { //сюда может прийти строка из нескольких слов
                word.dropLast(1) + "ის"
            } else {
                word + "ს"
            }
        }

        // 4. Именительный на -ო или -უ
        'ო', 'უ' -> word + "ვის"

        // На случай, если слово передано в виде основы без окончания падежа
        //  else -> word + "ის"
        else -> word
    }
}

//todo перенести в ямл
fun ParseContext.normalizeIdioms(vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        availableFlags.getOrNull(0)?.let { addedFlags.add(it) }
        s.replace("ვაჟა ფშაველა", "ვაჟა-ფშაველა")
            .replace("ზ. გამსახურდია", "ზვიად გამსახურდია")
            .replace("ც. დადიანი", "ცოტნე დადიანი")
            .replace("ნ. ბარათაშვილი", "ნიკოლოზ ბარათაშვილი")
        // .replace("პუშკინი", "ალექსანდრე პუშკინი")
    }

fun ParseContext.replace(oldVal: String, newVal: String, vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        availableFlags.getOrNull(0)?.let { addedFlags.add(it) }
        s.replace(oldVal, newVal)
    }

fun ParseContext.replace(regex: Regex, replacement: String, vararg availableFlags: ParsingFlags): ParseContext =
    modifyAndFlag { s, addedFlags ->
        availableFlags.getOrNull(0)?.let { addedFlags.add(it) }
        s.replace(regex, replacement)
    }


fun ParseContext.log(logPattern: String): ParseContext {
    Logging.info("$logPattern: $this")
    return this
}
