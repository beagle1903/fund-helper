package com.burha.fundhelper.domain

fun <T> sortByHeadlineReturn(
    rows: List<T>,
    headlineReturn: (T) -> Double?,
    code: (T) -> String,
): List<T> = rows.sortedWith { a, b ->
    val aReturn = headlineReturn(a)
    val bReturn = headlineReturn(b)
    when {
        aReturn == null && bReturn == null -> code(a).compareTo(code(b))
        aReturn == null -> 1
        bReturn == null -> -1
        else -> {
            val byReturn = aReturn.compareTo(bReturn)
            if (byReturn != 0) byReturn else code(a).compareTo(code(b))
        }
    }
}
