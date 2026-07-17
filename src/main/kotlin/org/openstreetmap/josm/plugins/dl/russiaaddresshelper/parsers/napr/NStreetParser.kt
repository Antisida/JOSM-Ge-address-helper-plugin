package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr

import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.StreetType
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.StreetTypes
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsedStreet
import org.openstreetmap.josm.tools.Logging
import kotlin.math.abs
import kotlin.math.min

class NStreetParser /*: IParser<ParsedStreet>*/ {
    private val streetTypes: StreetTypes = StreetTypes.byYml("/references/street_types.yml")
    val statuses = setOf(
        "ქუჩა", //улица
        "გამზირი", //проспект
        "ბულვარი", //Бульвар
        "ჩიხი", //тупик
        "შესახვევი", //переулок
        "გასასვლელი", //съезд
        "აღმართი", //склон, подъем, спуск
        "ხევი" //овраг
    )

    //todo надо анализировать стрку: если есть и улица (ქუჩა) и  (переулок (შესახვევი) или тупик (ჩიხი)) то приводить к
    // эталону как здесь https://openstreetmap.ge/numbered-streets/
    // სოფელი მახო , ქუჩა მე-9 - тут в вообще первели


    fun parse(sourceString: String): N_ParsedStreet {
        return ParseContext(sourceString)
            .log("StreetParser START")
            .removeParenthesesContent(ParsingFlags.PARENTHESES_REMOVED)
            .log("After removeParenthesesContent")
            .expandAbbreviations(ParsingFlags.HAS_ABBREVIATIONS)
            .log("After expandAbbreviations")
            .cleanRomanNumerals(ParsingFlags.ROMAN_NUMERAL_CLEANED)
            .log("After cleanRomanNumerals")
            .trim()
            .moveStatusToBack(ParsingFlags.STATUS_MOVED, ParsingFlags.GENITIVE_APPLIED)
            .log("After moveStatusToBack")
            .normalizeIdioms(ParsingFlags.IDIOMS_NORMALIZED)
            .log("After normalizeIdioms")
            .collapseDuplicateCommas(ParsingFlags.COMMAS_COLLAPSED)
            .log("After collapseDuplicateCommas")
            .trim()
            .let {
                N_ParsedStreet(
                    sourceString,
                    it.str,
                    it.flags.toMutableList(),
                    it.str.isNotBlank()
                )
            }
    }



    /*    *//*override*//* fun parse(rawNaprStreet: String, editDataSet: DataSet): N_ParsedStreet {
        //тэги, откуда будут собираться возможные имена
        val altNames: List<String> = listOf("alt_name", "old_name", "short_name")
        // Оставляем дороги у которых есть название
        // улицы, собранные отношениями, в которых на вэях нет тэгов??

        val primitives = editDataSet.allNonDeletedCompletePrimitives()
            .filter { p ->
                p.hasKey("highway")
                        && p.hasKey("name")
                        && p.type == OsmPrimitiveType.WAY
            }

        //формируем мапу <название вэя (в том числе из альтернативных тэгов) ->> <название из name, список объектов>>
        val primitiveNames: MutableMap<String, Pair<String, List<OsmPrimitive>>> = mutableMapOf()
        val primitivesByName = primitives.groupBy({ it.name }, { it })
        primitivesByName.forEach { (name, primitivesList) ->
            primitiveNames.putIfAbsent(name, Pair(name, primitivesList))
            primitivesList.forEach { primitive ->
                altNames.map {
                    if (primitive.hasKey(it)) {
                        val altName = primitive.get(it)
                        primitiveNames.putIfAbsent(
                            altName,
                            Pair(primitive.name, primitivesList.filter { pr -> pr.hasTag(it, altName) })
                        )
                    }
                }
            }
        }
        val normalizeStreetName = normalize(rawNaprStreet)
        val mostRelevantOsmName: Pair<String, List<OsmPrimitive>>? =
            getMostRelevantOsm(normalizeStreetName, primitiveNames)
        val flags = emptyList<ParsingFlags>().toMutableList()
        if (normalizeStreetName != rawNaprStreet) {
            flags.add(ParsingFlags.STREET_NAME_FUZZY_MATCH)
        }
        return N_ParsedStreet(
            rawNaprStreet,
            mostRelevantOsmName?.first ?: "",
            normalizeStreetName,
            mostRelevantOsmName?.second ?: emptyList(),
            flags
        )
        //return identify(rowNaprStreet, streetTypes, primitiveNames)
    }*/

    private fun getMostRelevantOsm(
        streetName: String,
        primitiveNames: MutableMap<String, Pair<String, List<OsmPrimitive>>>
    ): Pair<String, List<OsmPrimitive>>? {
        if (streetName.isBlank()) {
            Logging.info("streetName is blank")
            return null
        }

        val streetTypes: MutableList<String> = emptyList<String>().toMutableList()
        for (status in statuses) {
            if (streetName.contains(status)) {
                streetTypes.add(status)
            }
        }
        // Ищем соответствующий адресу примитив // улицы из слоя
        for (street in primitiveNames.keys) {
            //пропускаем осм имена которые заведомо не содержат определенных нами типов
            if (streetTypes.none { street.contains(it) }) {
                continue
            }
            if (street == streetName) {
                Logging.info("полное соответствие")
                return primitiveNames[street]
            }
        }
        val most = primitiveNames.keys.findBestAddress(streetName, 1)
        Logging.info("Most: $most (street: $streetName)")
        if (most != null) {
            Logging.info("Most primitives: ${primitiveNames[most]}")
            return primitiveNames[most]
        }
        return null
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val memo = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prevPrev = memo[0]
            memo[0] = i
            for (j in 1..s2.length) {
                val prev = memo[j]
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                memo[j] = min(min(memo[j] + 1, memo[j - 1] + 1), prevPrev + cost)
                prevPrev = prev
            }
        }
        return memo[s2.length]
    }

    // Функция поиска лучшего адреса
    fun Set<String>.findBestAddress(target: String, maxLevenshteinDistancePerWord: Int = 1): String? {
        if (this.isEmpty()) return null

        // Разбиваем входящий запрос на очищенные слова
        val targetWords = target.lowercase().split(Regex("\\s+|,|\\."))
            .filter { it.length > 1 } // Игнорируем предлоги/буквы типа "д", "к"
        if (targetWords.isEmpty()) return null

        return this
            .map { candidate ->
                val candidateWords = candidate.lowercase().split(Regex("\\s+|,|\\.")).filter { it.length > 1 }
                var matchedWords = 0

                // Считаем, сколько слов из запроса нашлось в кандидате
                for (tWord in targetWords) {
                    for (cWord in candidateWords) {
                        if (abs(tWord.length - cWord.length) <= maxLevenshteinDistancePerWord) {
                            if (levenshteinDistance(tWord, cWord) <= maxLevenshteinDistancePerWord) {
                                matchedWords++
                                break
                            }
                        }
                    }
                }
                // Сохраняем кандидата и долю совпавших слов (от 0.0 до 1.0)
                candidate to (matchedWords.toDouble() / targetWords.size)
            }
            // Оставляем только те строки, где совпало хотя бы 70% слов из запроса
            .filter { it.second >= 0.5 }
            // Сортируем: сначала те, где совпало больше слов
            .maxByOrNull { it.second }?.first
    }

    /*  //todo тут надо возвращать еще флаги если были какие-то преобразования
      fun normalize(rawNaprStreet: String): String {
          var street = rawNaprStreet
              .log("Начало")
              .log("После удаления скобок")
              .cleanGeorgianRomanNumerals()
              .log("После преобразования латинских цифр")
              .trim()
          //если только одно совпадение (исключаем 1 переулок 1 улицы)
          if (statuses.count { street.contains(it) } == 1) {
              street = moveToBack(street, statuses)
          }
          val result = street
              .log("После перемещения статуса в конец")
              .normalizeIdioms()
              .log("После идиом")
              .trim()
          return result
      }*/

    fun String.log(index: String): String {
        Logging.info("$index: $this")
        return this
    }

//    /** удаляет ს после латинского номера */
//    fun String.cleanGeorgianRomanNumerals(): String {
//        val regex = """(?U)\b(I|II|III|IV|V|VI|VII|VIII|IX|X)ს\b""".toRegex()
//        return this.replace(regex, "$1")
//    }

    /** Переносит статусную часть в конец, добавляет ს к названию если его там нет */
    fun moveToBack(input: String, movables: Set<String>): String {
        if (movables.isEmpty()) return input
        Logging.info("4: $input")
        val keywordsPattern = movables.joinToString("|") { Regex.escape(it) }
        Logging.info("5: $keywordsPattern")
        // Регулярное выражение ищет любое из слов в начале строки
        val regex = """(?U)^($keywordsPattern)\b\s*(.*)""".toRegex(RegexOption.IGNORE_CASE)

        return regex.replace(input) { matchResult ->
            val keyword = matchResult.groupValues[1]
            val restOfString = matchResult.groupValues[2]
            Logging.info("6: $keyword")
            Logging.info("7: $restOfString")

            if (restOfString.isNotEmpty()) {
                //к римским цифрам не добавляем "ს"
                if (restOfString in listOf("I", "V", "X") || restOfString.endsWith("ს")) "$restOfString $keyword"
                else "${restOfString}ს $keyword"
            } else keyword
        }
    }

    //todo перенести в ямл
    fun String.normalizeIdioms(): String {
        return this
            .replace("ვაჟა ფშაველა", "ვაჟა-ფშაველა")
            .replace("ზ. გამსახურდია", "ზვიად გამსახურდია")
            .replace("ც. დადიანი", "ცოტნე დადიანი")
            .replace("ნ. ბარათაშვილი", "ნიკოლოზ ბარათაშვილი")
            .replace("პუშკინი", "ალექსანდრე პუშკინი")
    }

    /* private fun identify(
         sourceAddress: String,
         streetTypes: StreetTypes,
         osmObjectNames: Map<String, Pair<String, List<OsmPrimitive>>>
     ): ParsedStreet {

         var s = sourceAddress
             .log("Начало")
             .removeParenthesesContent()
             .log("После удаления скобок")
             .cleanGeorgianRomanNumerals()
             .log("После преобразования латинских цифр")
             .trim()
         if (statuses.count { s.contains(it) } == 1) {
             s = moveToBack(s, setOf("ქუჩა", "გამზირი", "ბულვარი", "ჩიხი", "შესახვევი", "გასასვლელი", "აღმართი"))
         }
         val egrnStreetName = s.log("После перемещения статуса в конец")
             .normalizeIdioms()
             .log("После идиом")
             .trim()

         //todo не понятно зачем нужен streetType, заполнять если он действительно нужен
         //можно на основании этого определять названия где улица и переулок

         var streetType: StreetType? = null
 //        var egrnStreetName = ""
         val flags: MutableList<ParsingFlags> = mutableListOf()
         // Извлекаем название улицы
 //        for (type in streetTypes.types) {
 //            egrnStreetName = extractStreetName(type.egrn.asRegExpList(), sourceAddress)
 //
 //            if (egrnStreetName != "") {
 //                streetType = type
 //
 //                break
 //            }
 //        }

 //        if (streetType != null && streetType.name == "улица") {
 //            for (type in streetTypes.types) {
 //                val internalEgrnStreetName = extractStreetName(type.egrn.asRegExpList(), egrnStreetName)
 //                if (internalEgrnStreetName != "") {
 //                    streetType = type
 //                    Logging.info("EGRN-PLUGIN Street parser found double typed name in $sourceAddress, found secondary ${streetType.name}")
 //                    egrnStreetName = internalEgrnStreetName
 //                    break
 //                }
 //            }
 //        }

         if (egrnStreetName == "") {
             Logging.warn("EGRN-PLUGIN Cannot extract street name from EGRN address $sourceAddress")
             flags.add(ParsingFlags.CANNOT_EXTRACT_STREET_NAME)
             return ParsedStreet("", "", null, listOf(), flags)
         }

         val JWSsimilarity = JaroWinklerSimilarity()

 //        val filteredEGRNStreetName = egrnStreetName.replace('ё', 'е').replace('Ё', 'Е')

         var maxSimilarity = 0.0
         var mostSimilar = ""
         // Ищем соответствующий адресу примитив

         for (street in osmObjectNames.keys) {

             //пропускаем осм имена которые заведомо не содержат определенного нами типа
             //todo тут нужны статусы
 //            if (!street.contains(streetType!!.name, true)) {
 //                continue
 //            }

 //            val filteredOsmStreetName = extractStreetName(streetType.osm.asRegExpList(), street)
 //                .replace('ё', 'е').replace('Ё', 'Е')
 //
 //            if (filteredOsmStreetName == "") {
 //                Logging.info("EGRN-PLUGIN Cannot get openStreetMap name for $street, type ${streetType.name}")
 //                continue
 //            }
             val osmStreetName = street
             if (egrnStreetName.lowercase() == osmStreetName.lowercase()) {
                 if (street != osmObjectNames[street]?.first) {//fixme не очень понятно
                     flags.add(ParsingFlags.MATCHED_STREET_BY_SECONDARY_TAGS)
                 }
                 return ParsedStreet(
                     osmObjectNames[street]!!.first,
                     egrnStreetName,
                     streetType,
                     osmObjectNames[street]!!.second,
                     flags
                 )
             } else {
                 //todo подумать как это заменить
 //                val numberedSimilarity =
 //                    matchedNumberedStreet(egrnStreetName, osmStreetName, streetType.name)
 //                if (numberedSimilarity == 1.0) {
 //                    Logging.info("EGRN-PLUGIN Matched OSM street name by numerics parsing $egrnStreetName -> $street")
 //                    flags.add(ParsingFlags.STREET_HAS_NUMBERED_NAME)
 //                    return ParsedStreet(
 //                        osmObjectNames[street]!!.first,
 //                        egrnStreetName,
 //                        streetType,
 //                        osmObjectNames[street]!!.second,
 //                        flags
 //                    )
 //                }
 //todo подумать как это заменить
 //                if (matchedWithoutInitials(egrnStreetName, osmStreetName)) {
 //                    flags.add(ParsingFlags.STREET_NAME_INITIALS_MATCH)
 //                    Logging.warn("EGRN-PLUGIN Matched OSM street name without initials $egrnStreetName -> $street")
 //                    return ParsedStreet(
 //                        osmObjectNames[street]!!.first,
 //                        egrnStreetName,
 //                        streetType,
 //                        osmObjectNames[street]!!.second,
 //                        flags
 //                    )
 //                }

 //                val similarity = if (numberedSimilarity != 0.0) {
 //                    numberedSimilarity
 //                } else {
 //                    JWSsimilarity.apply(egrnStreetName, osmStreetName)
 //                }
                 val similarity = JWSsimilarity.apply(egrnStreetName, osmStreetName)
                 if (similarity > maxSimilarity) {
                     maxSimilarity = similarity
                     mostSimilar = street
                 }
             }
         }
         //TO DO low priority - вынести в настройки вкл.выкл нечеткого матчинга + значение схожести?
         if (mostSimilar.isNotBlank() && maxSimilarity > 0.9) {
             Logging.warn("EGRN-PLUGIN Exact street match for $egrnStreetName not found, use most similar: $mostSimilar with distance $maxSimilarity")
             flags.add(ParsingFlags.STREET_NAME_FUZZY_MATCH)
             return ParsedStreet(
                 osmObjectNames[mostSimilar]!!.first,
                 egrnStreetName,
 //                streetType!!,
                 null,
                 osmObjectNames[mostSimilar]!!.second,
                 flags
             )
         }
         flags.add(ParsingFlags.CANNOT_FIND_STREET_OBJECT_IN_OSM)
 //        return ParsedStreet("", egrnStreetName, streetType!!, listOf(), flags)
         return ParsedStreet("", egrnStreetName, streetType, listOf(), flags)
     }*/

    //пытаемся поматчить улицы убирая из них инициалы, префикс "им" и имена
    private fun matchedWithoutInitials(EGRNStreetName: String, osmStreetName: String): Boolean {
        val initialsRegexp = Regex("""[А-Я](\.\s?|\s)""")
        val namedByRegex = Regex("""им(\.|\s+)|имени\s+""")

        val divider = Regex("""\s+""")
        if (initialsRegexp.find(EGRNStreetName) != null || initialsRegexp.find(osmStreetName) != null || namedByRegex.find(
                EGRNStreetName
            ) != null || namedByRegex.find(osmStreetName) != null
        ) {
            val EGRNStreetWithoutInitials = EGRNStreetName.replace(namedByRegex, "").replace(initialsRegexp, "")
            if (EGRNStreetWithoutInitials == osmStreetName) return true
            if (osmStreetName.split(divider).size > 1) {
                val filteredOsmNameSurnameOnly = osmStreetName.split(divider).last()
                if (EGRNStreetWithoutInitials == filteredOsmNameSurnameOnly) return true
            }
            val OSMStreetNameWithoutInitials = osmStreetName.replace(namedByRegex, "").replace(initialsRegexp, "")
            if (EGRNStreetWithoutInitials == OSMStreetNameWithoutInitials || EGRNStreetName == OSMStreetNameWithoutInitials) return true
            if (EGRNStreetName.split(divider).size > 1) {
                val filteredEGRNNameSurnameOnly = EGRNStreetName.split(divider).last()
                if (filteredEGRNNameSurnameOnly == OSMStreetNameWithoutInitials) return true
            }
        }
        return false
    }

    private fun matchedNumberedStreet(
        egrnStreetName: String,
        osmStreetName: String,
        streetTypePrefix: String
    ): Double {
        val osmNumericsRegexp = Regex("""(?<streetNumber>\d{1,2})(\s|-)(й|ий|ый|ой|я|ая|ья|е|ое|ье)""")
        val egrnNumericsRegexp = Regex("""(?<streetNumber>\d{1,2})(\s|-)*(й|ий|ый|ой|я|ая|ья|е|ое|ье)""")
        //точно ли подходит этот регексп для ЕГРН нумерованных улиц? "2 улица Строителей" не сматчится, как и "2 Строительная улица"
        val egrnNumericsMatch = egrnNumericsRegexp.find(egrnStreetName) ?: return 0.0
        val egrnStreetNumber = egrnNumericsMatch.groups["streetNumber"] ?: return 0.0
        val osmNumericsMatch = osmNumericsRegexp.find(osmStreetName) ?: return 0.0
        val osmStreetNumber = osmNumericsMatch.groups["streetNumber"] ?: return 0.0
        if (egrnStreetNumber.value != osmStreetNumber.value) {
            return 0.1
        }
        val filteredEgrnName =
            egrnStreetName.replace(egrnNumericsRegexp, "").replace(streetTypePrefix, "").trim().lowercase()
        val filteredOsmName =
            osmStreetName.replace(osmNumericsRegexp, "").replace(streetTypePrefix, "").trim().lowercase()

        if (filteredEgrnName == filteredOsmName) {
            return 1.0
        }
        val JWSsimilarity = JaroWinklerSimilarity()
        return JWSsimilarity.apply(filteredEgrnName, filteredOsmName)
    }

    private fun extractStreetName(regExList: Collection<Regex>, address: String): String {
        if (address == "") {
            return ""
        }

        for (pattern in regExList) {
            val match = pattern.find(address)

            if (match != null) {
                val streetName = match.groups["street"]!!.value
                //костыли, потому что эта функция обрабатывает и ОСМ имена и ЕГРН.
                //возможно стоит добавить в ОСМ паттерн тоже номер улицы и убрать это условие?
                if (pattern.toString().contains("streetNumber")) {
                    val numericPrefixMatch = match.groups["streetNumber"]
                    if (numericPrefixMatch != null) {
                        val numericPrefix = numericPrefixMatch.value
                        var filteredStreetName = streetName.replace(numericPrefix, "").trim()
                        filteredStreetName = "$numericPrefix$filteredStreetName"
                        return filteredStreetName
                    }
                }
                return streetName
            }
        }

        return ""
    }

    fun String.removeParenthesesContent(): String {
        // Регулярное выражение находит ( и ) и всё между ними (жадный поиск исключая закрывающую скобку)
        val regex = """(?U)\([^)]*\)""".toRegex()

        return this.replace(regex, "")
            .replace("""(?U)\s+""".toRegex(), " ") // Схлопываем разбежавшиеся пробелы в один
            .trim()                            // Убираем пробелы по краям, если скобки были в начале/конце
    }

    /** Ищет букву N, если перед ней (через любые пробелы) НЕТ запятой и вставляет запятую. */
    fun String.insertMissingComma(): String {
        return this.replace(Regex("([^,\\s])\\s*N"), "$1, N")
    }

    /** Расширяет сокращения в названиях грузинских улиц. */
    fun String.expandAbbreviations(): String {
        return this.replace("ქ. ", "ქუჩა ")
            .replace("გამზ. ", "გამზირი ")
            .replace("შეს. ", "შესახვევი ")
    }

}