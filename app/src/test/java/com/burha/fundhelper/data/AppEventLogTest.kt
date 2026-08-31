package com.burha.fundhelper.data

import com.burha.fundhelper.fakes.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppEventLogTest {

    @Test
    fun newest_first_stamps_clock_and_caps_at_100() = runTest {
        val clock = FakeClock(1_000L)
        val log = AppEventLog(clock)
        repeat(101) { i ->
            clock.now = 1_000L + i
            log.append(AppEventLevel.Info, AppEventKind.FollowAdded, detail = "C$i")
        }
        val events = log.observe().first()
        assertEquals(100, events.size)
        assertEquals("C100", events.first().detail)
        assertEquals("C1", events.last().detail)
        assertEquals(1_100L, events.first().atMillis)
        assertEquals(AppEventLevel.Info, events.first().level)
        assertEquals(AppEventKind.FollowAdded, events.first().kind)
    }

    @Test
    fun append_assigns_distinct_monotonic_ids_even_at_same_millis_and_after_cap() = runTest {
        val clock = FakeClock(5_000L)
        val log = AppEventLog(clock)
        log.append(AppEventLevel.Info, AppEventKind.FollowAdded, detail = "A")
        log.append(AppEventLevel.Info, AppEventKind.FollowAdded, detail = "B")
        val sameMillisPair = log.observe().first()
        assertEquals(2, sameMillisPair.size)
        assertTrue(sameMillisPair[0].id != sameMillisPair[1].id)
        assertTrue(sameMillisPair[0].id > sameMillisPair[1].id)

        repeat(100) { i ->
            log.append(AppEventLevel.Info, AppEventKind.FollowAdded, detail = "C$i")
        }
        val capped = log.observe().first()
        assertEquals(100, capped.size)
        val ids = capped.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.first() > ids.last())
    }

    @Test
    fun clear_empties_observers() = runTest {
        val log = AppEventLog(FakeClock())
        log.append(AppEventLevel.Error, AppEventKind.TefasPricesError, detail = "HTTP 403")
        log.clear()
        assertTrue(log.observe().first().isEmpty())
    }
}
