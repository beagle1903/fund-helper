package com.burha.fundhelper.domain

import java.util.Locale

fun foldForSearch(raw: String): String {
    val mapped = buildString(raw.length) {
        for (ch in raw) {
            append(
                when (ch) {
                    'ı', 'I', 'İ' -> 'i'
                    'ş', 'Ş' -> 's'
                    'ğ', 'Ğ' -> 'g'
                    'ü', 'Ü' -> 'u'
                    'ö', 'Ö' -> 'o'
                    'ç', 'Ç' -> 'c'
                    'â', 'Â' -> 'a'
                    'î', 'Î' -> 'i'
                    'û', 'Û' -> 'u'
                    else -> ch
                },
            )
        }
    }
    return mapped.lowercase(Locale.ROOT)
}
