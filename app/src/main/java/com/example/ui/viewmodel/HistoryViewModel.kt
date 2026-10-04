package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ScanHistoryRepository
import com.example.domain.model.BarcodeType
import com.example.domain.model.ParsedBarcodeResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistoryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Starred"),
    URL("Links"),
    WIFI("Wi-Fi"),
    CONTACT("Contacts"),
    TEXT("Text"),
    PRODUCT("Products")
}

data class HistoryUiState(
    val searchQuery: String = "",
    val activeFilter: HistoryFilter = HistoryFilter.ALL,
    val selectedItemForDetail: ParsedBarcodeResult? = null,
    val showClearConfirmDialog: Boolean = false,
    val infoMessage: String? = null
)

class HistoryViewModel(
    private val repository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: StateFlow<List<ParsedBarcodeResult>> = combine(
        _uiState
    ) { stateArray ->
        stateArray[0] as HistoryUiState
    }.flatMapLatest { state ->
        val query = state.searchQuery.trim()
        val filter = state.activeFilter

        if (query.isNotEmpty()) {
            repository.searchHistory(query)
        } else {
            when (filter) {
                HistoryFilter.ALL -> repository.allHistory
                HistoryFilter.FAVORITES -> repository.favoriteHistory
                HistoryFilter.URL -> repository.getHistoryByType(BarcodeType.URL)
                HistoryFilter.WIFI -> repository.getHistoryByType(BarcodeType.WIFI)
                HistoryFilter.CONTACT -> repository.getHistoryByType(BarcodeType.CONTACT)
                HistoryFilter.TEXT -> repository.getHistoryByType(BarcodeType.TEXT)
                HistoryFilter.PRODUCT -> repository.getHistoryByType(BarcodeType.PRODUCT)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilter(filter: HistoryFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    fun openDetail(item: ParsedBarcodeResult) {
        _uiState.value = _uiState.value.copy(selectedItemForDetail = item)
    }

    fun closeDetail() {
        _uiState.value = _uiState.value.copy(selectedItemForDetail = null)
    }

    fun toggleFavorite(item: ParsedBarcodeResult) {
        viewModelScope.launch {
            val newFav = !item.isFavorite
            repository.toggleFavorite(item.id, newFav)
            if (_uiState.value.selectedItemForDetail?.id == item.id) {
                _uiState.value = _uiState.value.copy(
                    selectedItemForDetail = _uiState.value.selectedItemForDetail?.copy(isFavorite = newFav)
                )
            }
        }
    }

    fun deleteItem(item: ParsedBarcodeResult) {
        viewModelScope.launch {
            repository.deleteById(item.id)
            if (_uiState.value.selectedItemForDetail?.id == item.id) {
                _uiState.value = _uiState.value.copy(selectedItemForDetail = null)
            }
            _uiState.value = _uiState.value.copy(infoMessage = "Item removed from history")
        }
    }

    fun showClearDialog() {
        _uiState.value = _uiState.value.copy(showClearConfirmDialog = true)
    }

    fun dismissClearDialog() {
        _uiState.value = _uiState.value.copy(showClearConfirmDialog = false)
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _uiState.value = _uiState.value.copy(
                showClearConfirmDialog = false,
                selectedItemForDetail = null,
                infoMessage = "All history cleared"
            )
        }
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    fun generateExportText(items: List<ParsedBarcodeResult>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("Fast QR Scan History\n")
        sb.append("Exported: ${dateFormat.format(Date())}\n")
        sb.append("Total items: ${items.size}\n\n")

        items.forEachIndexed { index, item ->
            sb.append("${index + 1}. [${item.type.displayName}] ${item.title}\n")
            sb.append("   Content: ${item.rawValue}\n")
            sb.append("   Date: ${dateFormat.format(Date(item.timestamp))}\n\n")
        }
        return sb.toString()
    }

    companion object {
        fun provideFactory(repository: ScanHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }
    }
}
