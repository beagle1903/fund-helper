package com.burha.fundhelper.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burha.fundhelper.R
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.ui.FundRowCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.fetch_error)
    val retryText = stringResource(R.string.retry)

    LaunchedEffect(state.showError) {
        if (state.showError) {
            val result = snackbarHostState.showSnackbar(errorText, retryText)
            viewModel.consumeError()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.submit(refetchCatalog = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.submit() }),
            )
            Button(
                onClick = { viewModel.submit() },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.search_action))
            }
            when {
                state.emptyQueryHint -> Text(
                    stringResource(R.string.search_empty_query),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                )
                state.noResults -> Text(
                    stringResource(R.string.search_no_results),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                )
                else -> LazyColumn(Modifier.padding(top = 8.dp)) {
                    items(state.matches, key = { it.code }) { fund ->
                        val followed = fund.code in state.followedCodes
                        SearchRow(
                            fund = fund,
                            followed = followed,
                            onOpen = onOpen,
                            onToggle = { viewModel.toggleFollow(fund.code, followed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(
    fund: FundSnapshot,
    followed: Boolean,
    onOpen: (String) -> Unit,
    onToggle: () -> Unit,
) {
    FundRowCard(
        code = fund.code,
        name = fund.name,
        onClick = { onOpen(fund.code) },
        trailing = {
            IconButton(onClick = onToggle) {
                if (followed) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(R.string.unfollow),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        Icons.Outlined.StarBorder,
                        contentDescription = stringResource(R.string.follow),
                    )
                }
            }
        },
    )
}
