package com.burha.fundhelper.fakes

import com.burha.fundhelper.domain.Clock

class FakeClock(var now: Long = 1_000_000L) : Clock {
    override fun nowMillis(): Long = now
}
