package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools

object Transliterator {

    private val charMap: Map<Char, String> = mapOf(
        'ა' to "a", 'ბ' to "b", 'გ' to "g", 'დ' to "d", 'ე' to "e",
        'ვ' to "v", 'ზ' to "z", 'თ' to "t", 'ი' to "i", 'კ' to "k",
        'ლ' to "l", 'მ' to "m", 'ნ' to "n", 'ო' to "o", 'პ' to "p",
        'ჟ' to "zh", 'რ' to "r", 'ს' to "s", 'ტ' to "t", 'უ' to "u",
        'ფ' to "p", 'ქ' to "k", 'ღ' to "gh", 'ყ' to "q", 'შ' to "sh",
        'ჩ' to "ch", 'ც' to "ts", 'ძ' to "dz", 'წ' to "ts", 'ჭ' to "ch",
        'ხ' to "kh", 'ჯ' to "j", 'ჰ' to "h"
    )

    fun transliterate(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return buildString(text.length) {
            for (ch in text) {
                append(charMap[ch] ?: ch)
            }
        }
    }
}
