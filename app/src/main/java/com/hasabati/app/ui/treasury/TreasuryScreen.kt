package com.hasabati.app.ui.treasury

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hasabati.app.data.db.entities.Currency
import com.hasabati.app.data.db.entities.Transaction
import com.hasabati.app.ui.common.*
import com.hasabati.app.ui.theme.*

@Composable
fun TreasuryScreen() {
    val vm = hasabatiViewModel { TreasuryViewModel(it) }
    val state by vm.uiState.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("الخزينة", style = MaterialTheme.typography.headlineMedium) }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { StatCard("النقد بالدولار", Formatters.usd(state.snapshot.cashUsd), "💵", SuccessGreen) }
                item { StatCard("النقد بالليرة", Formatters.syp(state.snapshot.cashSyp), "💴", WarningAmber) }
                item { StatCard("Sham Cash دولار", Formatters.usd(state.snapshot.shamCashUsd), "📱", PurpleAccent) }
                item { StatCard("Sham Cash ليرة", Formatters.syp(state.snapshot.shamCashSyp), "📱", PurpleAccentLight) }
            }
        }
        item { SectionTitle("سجل الحركات") }
        if (state.transactions.isEmpty()) {
            item { EmptyState("لا توجد حركات بعد") }
        } else {
            items(state.transactions) { tx -> TreasuryTxRow(tx) }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun TreasuryTxRow(tx: Transaction) {
    val positive = tx.type.name in listOf("CUSTOMER_PAYMENT", "CAPITAL_ADDITION", "CAPITAL_INITIAL")
    val color = if (positive) SuccessGreen else DangerRed
    Card(shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(tx.type.arabicLabel, fontWeight = FontWeight.SemiBold)
                Text("${tx.method.arabicLabel} • ${Formatters.dateTime(tx.createdAt)}", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "${if (positive) "+" else "-"}${if (tx.currency == Currency.USD) Formatters.usd(tx.amount) else Formatters.syp(tx.amount)}",
                color = color, fontWeight = FontWeight.Bold
            )
        }
    }
}
