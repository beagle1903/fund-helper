package com.burha.fundhelper.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burha.fundhelper.data.AppEvent
import com.burha.fundhelper.data.AppEventLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val events: List<AppEvent> = emptyList(),
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val events: AppEventLog,
) : ViewModel() {
    private val _state = MutableStateFlow(LogsUiState())
    val state: StateFlow<LogsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            events.observe().collect { rows ->
                _state.value = LogsUiState(events = rows)
            }
        }
    }

    fun clear() {
        events.clear()
    }
}
