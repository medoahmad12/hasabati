package com.hasabati.app.ui.treasury

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasabati.app.data.db.entities.Transaction
import com.hasabati.app.data.repository.HasabatiRepository
import com.hasabati.app.domain.FinanceEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TreasuryViewModel(repository: HasabatiRepository) : ViewModel() {
    data class UiState(
        val snapshot: FinanceEngine.TreasurySnapshot = FinanceEngine.TreasurySnapshot(),
        val transactions: List<Transaction> = emptyList()
    )

    val uiState: StateFlow<UiState> = repository.observeTransactions().map { txs ->
        UiState(FinanceEngine.treasury(txs), txs.sortedByDescending { it.createdAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())
}
