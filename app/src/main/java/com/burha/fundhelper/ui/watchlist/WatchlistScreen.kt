package com.burha.fundhelper.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burha.fundhelper.R
import com.burha.fundhelper.data.WatchlistRow
import com.burha.fundhelper.ui.FundRowCard
import com.burha.fundhelper.ui.ReturnPercentText
import com.burha.fundhelper.ui.formatFetchedAt
import com.burha.fundhelper.ui.formatNumber
import com.burha.fundhelper.ui.periodLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onSearch: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.fetch_error)
    val retryText = stringResource(R.string.retry)

    LaunchedEffect(Unit) {
        viewModel.refresh(force = false)
    }
    LaunchedEffect(state.showError) {
        if (state.showError) {
            val result = snackbarHostState.showSnackbar(
                message = errorText,
                actionLabel = retryText,
            )
            viewModel.consumeError()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.refresh(force = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.watchlist_title)) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(force = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.rows.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.watchlist_empty_title))
                    Button(onClick = onSearch, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.watchlist_empty_cta))
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.code }) { row ->
                        WatchlistRowItem(
                            row = row,
                            onOpen = onOpen,
                            onUnfollow = { viewModel.unfollow(row.code) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistRowItem(
    row: WatchlistRow,
    onOpen: (String) -> Unit,
    onUnfollow: () -> Unit,
) {
    val dash = stringResource(R.string.price_missing)
    val period = periodLabel(row.headlinePeriod)
    FundRowCard(
        code = row.code,
        name = row.name ?: dash,
        onClick = { onOpen(row.code) },
        supporting = {
            Text(
                row.price?.let(::formatNumber) ?: dash,
                style = MaterialTheme.typography.bodyLarge,
            )
            row.fetchedAt?.let {
                Text(
                    formatFetchedAt(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReturnPercentText(
                    value = row.headlineReturn,
                    leading = period,
                )
                IconButton(onClick = onUnfollow) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(R.string.unfollow),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}
