package org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.vocabulary

import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.vocabulary.StreetDictionary.streets
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.min

object StreetDictionary {
    private const val FILE_NAME = "street-list/georgian_streets.csv"

    val streets: Map<String, StreetTranslate> by lazy {
        val inputStream = javaClass.classLoader.getResourceAsStream(FILE_NAME)
            ?: throw IllegalArgumentException("File $FILE_NAME not found in classpath!")

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
            lines.drop(1) // Пропускаем заголовок
                .map { line ->
                    val tokens = line.split(",")
                    StreetTranslate(
                        name = tokens[0].trim(),
                        nameKa = tokens[1].trim(),
                        nameEn = tokens[2].trim(),
                        nameRu = tokens[3].trim()
                    )
                }
                .toSet()
                .associateBy { it.name } }
        }

//    fun getByName(name: String): StreetTranslate? {
//        return streets.get(name)
////        return usersList.find { it.name.equals(name, ignoreCase = true) }
//    }
//
//    fun isExists(name: String): Boolean {
//        return streets.get(name) != null
//    }

}