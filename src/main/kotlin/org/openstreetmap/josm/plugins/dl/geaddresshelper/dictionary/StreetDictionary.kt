package org.openstreetmap.josm.plugins.dl.geaddresshelper.dictionary

import java.io.BufferedReader
import java.io.InputStreamReader

object StreetDictionary {
    private const val FILE_NAME = "street-list/georgian_streets.csv"

    val streets: Map<String, StreetTranslate> by lazy {
        val inputStream = javaClass.classLoader.getResourceAsStream(FILE_NAME)
            ?: throw IllegalArgumentException("File $FILE_NAME not found in classpath!")

        val dict: MutableMap<String, StreetTranslate> = HashMap()
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            .useLines { lines ->
                lines.drop(1) // заголовок
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
                    .forEach {
                        dict[it.name] = it
                        dict[it.nameKa] = it
                        dict[it.nameRu] = it
                        dict[it.nameEn] = it
                    }
            }
        dict
    }

    fun getFirstNotNullOrNull(name: String?, nameKa: String?, nameRu: String?, nameEn: String?): StreetTranslate? =
        sequenceOf(name, nameKa, nameRu, nameEn)
            .firstNotNullOfOrNull { n -> n?.let { streets[it] } }

}