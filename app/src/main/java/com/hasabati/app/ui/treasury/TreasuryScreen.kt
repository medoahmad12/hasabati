package com.hasabati.app.ui.treasury

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hasabati.app.data.db.entities.Currency
import com.hasabati.app.data.db.entities.Transaction
import com.hasabati.app.data.db.entities.TransactionType
import com.hasabati.app.ui.common.*
import com.hasabati.app.ui.theme.*

@Composable
fun TreasuryScreen() {
    val vm = hasabatiViewModel { TreasuryViewModel(it) }
    val state by vm.uiState.collectAsState()
    var showTransferDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTransferDialog = true },
                containerColor = PurpleAccent,
                icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                text = { Text("تحويل Sham Cash إلى نقد") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
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
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showTransferDialog) {
        TransferDialog(
            onDismiss = { showTransferDialog = false },
            onConfirm = { amount, currency ->
                vm.transferToCash(amount, currency)
                showTransferDialog = false
            }
        )
    }
}

@Composable
private fun TransferDialog(onDismiss: () -> Unit, onConfirm: (Double, Currency) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currency.USD) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحويل من Sham Cash إلى نقد") },
        text = {
            Column {
                Text("هذا يسحب المبلغ من رصيد Sham Cash ويضيفه إلى النقد بنفس العملة.", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("المبلغ") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.padding(top = 8.dp)) {
                    Currency.values().forEach { c ->
                        FilterChip(selected = currency == c, onClick = { currency = c }, label = { Text(c.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@TextButton
                onConfirm(amt, currency)
            }) { Text("تحويل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun TreasuryTxRow(tx: Transaction) {
    val positive = tx.type == TransactionType.CUSTOMER_PAYMENT || tx.type == TransactionType.CAPITAL_ADDITION ||
        tx.type == TransactionType.CAPITAL_INITIAL || tx.type == TransactionType.TRANSFER_IN
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
