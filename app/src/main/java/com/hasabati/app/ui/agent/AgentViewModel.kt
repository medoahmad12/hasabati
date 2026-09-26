package com.hasabati.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasabati.app.data.db.entities.Currency
import com.hasabati.app.data.db.entities.PaymentMethod
import com.hasabati.app.data.db.entities.Transaction
import com.hasabati.app.data.repository.HasabatiRepository
import com.hasabati.app.domain.FinanceEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AgentViewModel(private val repository: HasabatiRepository) : ViewModel() {
    data class UiState(
        val totalOwedUsd: Double = 0.0,
        val paidUsd: Double = 0.0,
        val remainingUsd: Double = 0.0,
        val payments: List<Transaction> = emptyList()
    )

    val uiState: StateFlow<UiState> = combine(
        repository.observeOrders(),
        repository.observeTransactions()
    ) { orders, txs ->
        val totalOwed = orders.filter { it.status.name != "CANCELLED" }.sumOf { it.actualCostUsd ?: it.expectedCostUsd }
        val paid = txs.filter { it.type.name == "AGENT_PAYMENT" }.sumOf { it.usdEquivalent }
        UiState(
            totalOwedUsd = totalOwed,
            paidUsd = paid,
            remainingUsd = FinanceEngine.agentPayableUsd(orders, txs),
            payments = txs.filter { it.type.name == "AGENT_PAYMENT" }.sortedByDescending { it.createdAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun payAgent(amount: Double, currency: Currency, method: PaymentMethod, rate: Double?, note: String) {
        viewModelScope.launch { repository.payAgent(amount, currency, method, rate, note) }
    }
}
