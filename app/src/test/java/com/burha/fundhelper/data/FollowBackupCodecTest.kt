package com.burha.fundhelper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowBackupCodecTest {

    @Test
    fun encode_roundtrips_normalized_codes() {
        val json = FollowBackupCodec.encode(listOf("AAL", "AAK"))
        assertEquals(listOf("AAK", "AAL"), FollowBackupCodec.decode(json))
        assertTrue(json.contains("\"version\":1"))
    }

    @Test
    fun encode_trims_dedupes_and_drops_blanks() {
        val json = FollowBackupCodec.encode(listOf(" aak ", "AAK", "", "  "))
        assertEquals(listOf("AAK"), FollowBackupCodec.decode(json))
    }

    @Test
    fun decode_empty_or_malformed_returns_empty() {
        assertTrue(FollowBackupCodec.decode("").isEmpty())
        assertTrue(FollowBackupCodec.decode("{not json").isEmpty())
        assertTrue(FollowBackupCodec.decode("{\"version\":1}").isEmpty())
        assertTrue(FollowBackupCodec.decode("{\"version\":1,\"codes\":[\"\",null]}").isEmpty())
    }

    @Test
    fun decode_reads_codes_even_if_version_unknown() {
        val json = "{\"version\":9,\"codes\":[\"XYZ\"]}"
        assertEquals(listOf("XYZ"), FollowBackupCodec.decode(json))
    }
}
