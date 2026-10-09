package org.openstreetmap.josm.plugins.dl.geaddresshelper.dictionary

import java.io.BufferedReader
import java.io.InputStreamReader

object StreetDictionary {
    private const val FILE_NAME = "street-list/georgian_streets.csv"

    /**
     * Ключ - названия на грузинском, русском, английском.
     * Значение - StreetTranslate.
     */
    val streets: Map<String, StreetTranslation> by lazy {
        val inputStream = javaClass.classLoader.getResourceAsStream(FILE_NAME)
            ?: throw IllegalArgumentException("File $FILE_NAME not found in classpath!")

        val dict: MutableMap<String, StreetTranslation> = HashMap()
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            .useLines { lines ->
                lines.drop(1) // заголовок
                    .map { line ->
                        val tokens = line.split(",")
                        StreetTranslation(
                            nameKa = tokens[0].trim(),
                            nameEn = tokens[1].trim(),
                            nameRu = tokens[2].trim()
                        )
                    }
                    .toSet()
                    .forEach {
                        dict[it.nameKa] = it
                        dict[it.nameRu] = it
                        dict[it.nameEn] = it
                    }
            }
        dict
    }

    fun getFirstNotNullOrNull(name: String?, nameKa: String?, nameRu: String?, nameEn: String?): StreetTranslation? =
        sequenceOf(name, nameKa, nameRu, nameEn)
            .firstNotNullOfOrNull { n -> n?.let { streets[it] } }

    fun getByNameOrNull(name: String): StreetTranslation? = streets[name]

}