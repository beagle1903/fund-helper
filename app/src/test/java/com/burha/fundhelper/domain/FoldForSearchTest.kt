package com.burha.fundhelper.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FoldForSearchTest {

    @Test
    fun i_family_folds_to_i() {
        assertEquals("i", foldForSearch("i"))
        assertEquals("i", foldForSearch("ı"))
        assertEquals("i", foldForSearch("I"))
        assertEquals("i", foldForSearch("İ"))
    }

    @Test
    fun turkish_consonants_and_vowels_fold() {
        assertEquals("s", foldForSearch("ş"))
        assertEquals("s", foldForSearch("Ş"))
        assertEquals("g", foldForSearch("ğ"))
        assertEquals("g", foldForSearch("Ğ"))
        assertEquals("u", foldForSearch("ü"))
        assertEquals("u", foldForSearch("Ü"))
        assertEquals("o", foldForSearch("ö"))
        assertEquals("o", foldForSearch("Ö"))
        assertEquals("c", foldForSearch("ç"))
        assertEquals("c", foldForSearch("Ç"))
    }

    @Test
    fun circumflex_strips_to_base_vowel() {
        assertEquals("a", foldForSearch("â"))
        assertEquals("a", foldForSearch("Â"))
        assertEquals("i", foldForSearch("î"))
        assertEquals("i", foldForSearch("Î"))
        assertEquals("u", foldForSearch("û"))
        assertEquals("u", foldForSearch("Û"))
        assertEquals("kar", foldForSearch("kâr"))
        assertEquals(foldForSearch("kar"), foldForSearch("kâr"))
    }

    @Test
    fun words_fold_both_directions() {
        assertEquals("yatirim", foldForSearch("Yatırım"))
        assertEquals(foldForSearch("yatirim"), foldForSearch("Yatırım"))
        assertEquals("degisken", foldForSearch("Değişken"))
        assertEquals(foldForSearch("degisken"), foldForSearch("DEĞİŞKEN"))
        assertEquals("aak", foldForSearch("AAK"))
    }
}
