package com.burha.fundhelper.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burha.fundhelper.R
import com.burha.fundhelper.domain.ReturnKeys
import com.burha.fundhelper.ui.ReturnPercentText
import com.burha.fundhelper.ui.formatFetchedAt
import com.burha.fundhelper.ui.formatNumber
import com.burha.fundhelper.ui.periodLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.fetch_error)
    val retryText = stringResource(R.string.retry)
    val dash = stringResource(R.string.price_missing)

    LaunchedEffect(state.detail?.isFollowed) {
        if (state.detail?.isFollowed == true) {
            viewModel.refresh(force = false)
        }
    }
    LaunchedEffect(state.showError) {
        if (state.showError) {
            val result = snackbarHostState.showSnackbar(errorText, retryText)
            viewModel.consumeError()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.refresh(force = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            val detail = state.detail
            if (state.loaded && detail == null) {
                Column(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.detail_missing))
                    Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.back))
                    }
                }
            } else if (detail != null) {
                val snapshot = detail.snapshot
                val missing = stringResource(R.string.missing_field)
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(snapshot.code, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(snapshot.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                snapshot.price?.let { formatNumber(it) } ?: dash,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            snapshot.priceDate?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                formatFetchedAt(snapshot.fetchedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.detail_returns),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            ReturnKeys.DISPLAY_ORDER.forEach { key ->
                                val value = snapshot.returns[key] ?: return@forEach
                                ReturnPercentText(value, leading = periodLabel(key))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.detail_type),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(snapshot.fundType ?: missing)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.detail_risk),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(snapshot.risk ?: missing)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.detail_fees),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (snapshot.fees.isEmpty()) {
                        Text(missing)
                    } else {
                        snapshot.fees.forEach { Text("${it.label}: ${it.value}") }
                    }
                    Text(detail.explanation, modifier = Modifier.padding(top = 16.dp))
                    Text(
                        stringResource(R.string.disclaimer),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = viewModel::toggleFollow,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(
                            if (detail.isFollowed) stringResource(R.string.unfollow)
                            else stringResource(R.string.follow),
                        )
                    }
                }
            }
        }
    }
}
