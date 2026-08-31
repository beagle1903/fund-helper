package com.burha.fundhelper.data

import com.burha.fundhelper.domain.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppEventLevel { Info, Error }

enum class AppEventKind {
    TefasCatalogOk,
    TefasCatalogError,
    TefasPricesOk,
    TefasPricesError,
    TefasRefreshSkipped,
    FollowAdded,
    FollowRemoved,
    FollowAll,
    FollowsCleared,
    BackupWriteFailed,
    BackupRestored,
    BackupReadFailed,
    SearchOk,
    SearchFailed,
}

data class AppEvent(
    val atMillis: Long,
    val level: AppEventLevel,
    val kind: AppEventKind,
    val detail: String? = null,
    val count: Int? = null,
    val durationMs: Long? = null,
)

@Singleton
class AppEventLog @Inject constructor(
    private val clock: Clock,
) {
    private val _events = MutableStateFlow<List<AppEvent>>(emptyList())

    fun observe(): Flow<List<AppEvent>> = _events.asStateFlow()

    @Synchronized
    fun append(
        level: AppEventLevel,
        kind: AppEventKind,
        detail: String? = null,
        count: Int? = null,
        durationMs: Long? = null,
    ) {
        val event = AppEvent(
            atMillis = clock.nowMillis(),
            level = level,
            kind = kind,
            detail = detail,
            count = count,
            durationMs = durationMs,
        )
        _events.value = (listOf(event) + _events.value).take(MAX)
    }

    @Synchronized
    fun clear() {
        _events.value = emptyList()
    }

    private companion object {
        const val MAX = 100
    }
}
