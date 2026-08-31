package com.burha.fundhelper.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burha.fundhelper.R
import com.burha.fundhelper.data.AppEvent
import com.burha.fundhelper.data.AppEventKind
import com.burha.fundhelper.data.AppEventLevel
import com.burha.fundhelper.ui.formatLogTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::clear,
                        enabled = state.events.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.logs_clear))
                    }
                },
            )
        },
    ) { padding ->
        if (state.events.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.logs_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.logs_empty_body),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(
                    items = state.events,
                    key = { it.atMillis.toString() + it.kind.name + it.detail },
                ) { event ->
                    LogRow(event)
                }
            }
        }
    }
}

@Composable
private fun LogRow(event: AppEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            formatLogTime(event.atMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (event.level == AppEventLevel.Error) {
                        stringResource(R.string.logs_level_error)
                    } else {
                        stringResource(R.string.logs_level_info)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (event.level == AppEventLevel.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    logTitle(event.kind),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                logDetail(event),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun logTitle(kind: AppEventKind): String = stringResource(
    when (kind) {
        AppEventKind.TefasCatalogOk,
        AppEventKind.TefasCatalogError,
        -> R.string.logs_kind_tefas_catalog

        AppEventKind.TefasPricesOk,
        AppEventKind.TefasPricesError,
        -> R.string.logs_kind_tefas_prices

        AppEventKind.TefasRefreshSkipped -> R.string.logs_kind_tefas_refresh
        AppEventKind.FollowAdded,
        AppEventKind.FollowRemoved,
        AppEventKind.FollowAll,
        -> R.string.logs_kind_follow

        AppEventKind.FollowsCleared -> R.string.logs_kind_reset
        AppEventKind.BackupWriteFailed,
        AppEventKind.BackupRestored,
        AppEventKind.BackupReadFailed,
        -> R.string.logs_kind_backup

        AppEventKind.SearchOk,
        AppEventKind.SearchFailed,
        -> R.string.logs_kind_search
    },
)

@Composable
private fun logDetail(event: AppEvent): String = when (event.kind) {
    AppEventKind.TefasCatalogOk,
    AppEventKind.TefasPricesOk,
    -> stringResource(
        R.string.logs_ok_detail,
        String.format(
            Locale.forLanguageTag("tr-TR"),
            "%.1f sn",
            (event.durationMs ?: 0L) / 1000.0,
        ),
        event.count ?: 0,
    )

    AppEventKind.TefasRefreshSkipped -> stringResource(R.string.logs_refresh_skipped)
    AppEventKind.FollowAdded -> stringResource(R.string.logs_follow_added, event.detail.orEmpty())
    AppEventKind.FollowRemoved -> stringResource(R.string.logs_follow_removed, event.detail.orEmpty())
    AppEventKind.FollowAll -> if ((event.count ?: 0) == 0) {
        stringResource(R.string.logs_follow_all_none)
    } else {
        stringResource(R.string.logs_follow_all, event.detail.orEmpty())
    }

    AppEventKind.FollowsCleared -> stringResource(R.string.logs_follows_cleared)
    AppEventKind.BackupRestored -> stringResource(R.string.logs_backup_restored, event.count ?: 0)
    AppEventKind.SearchOk -> stringResource(
        R.string.logs_search_ok,
        event.detail.orEmpty(),
        event.count ?: 0,
    )

    AppEventKind.TefasCatalogError,
    AppEventKind.TefasPricesError,
    AppEventKind.BackupWriteFailed,
    AppEventKind.BackupReadFailed,
    AppEventKind.SearchFailed,
    -> event.detail ?: stringResource(R.string.price_missing)
}
