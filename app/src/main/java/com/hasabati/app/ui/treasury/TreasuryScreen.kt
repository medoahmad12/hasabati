package com.hasabati.app.ui.treasury

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hasabati.app.data.db.entities.Currency
import com.hasabati.app.data.db.entities.PaymentMethod
import com.hasabati.app.data.db.entities.Transaction
import com.hasabati.app.data.db.entities.TransactionType
import com.hasabati.app.ui.common.*
import com.hasabati.app.ui.theme.*

@Composable
fun TreasuryScreen() {
    val vm = hasabatiViewModel { TreasuryViewModel(it) }
    val state by vm.uiState.collectAsState()
    var showTransferDialog by remember { mutableStateOf(false) }
    var showConvertDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("الخزينة", style = MaterialTheme.typography.headlineMedium) }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(330.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { StatCard("النقد بالدولار", Formatters.usd(state.snapshot.cashUsd), "💵", SuccessGreen) }
                item { StatCard("النقد بالليرة", Formatters.syp(state.snapshot.cashSyp), "💴", WarningAmber) }
                item { StatCard("النقد بالريال", Formatters.sar(state.snapshot.cashSar), "💰", Color(0xFF2E86DE)) }
                item { StatCard("Sham Cash دولار", Formatters.usd(state.snapshot.shamCashUsd), "📱", PurpleAccent) }
                item { StatCard("Sham Cash ليرة", Formatters.syp(state.snapshot.shamCashSyp), "📱", PurpleAccentLight) }
                item { StatCard("Sham Cash ريال", Formatters.sar(state.snapshot.shamCashSar), "📱", Color(0xFF0FA3B1)) }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showTransferDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent)
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sham Cash ↔ نقد")
                }
                Button(
                    onClick = { showConvertDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تحويل عملات")
                }
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

    if (showTransferDialog) {
        WalletTransferDialog(
            onDismiss = { showTransferDialog = false },
            onConfirm = { amount, currency ->
                vm.transferToCash(amount, currency)
                showTransferDialog = false
            }
        )
    }
    if (showConvertDialog) {
        CurrencyConvertDialog(
            onDismiss = { showConvertDialog = false },
            onConfirm = { amountOut, currencyOut, methodOut, amountIn, currencyIn, methodIn ->
                vm.convertCurrency(amountOut, currencyOut, methodOut, amountIn, currencyIn, methodIn)
                showConvertDialog = false
            }
        )
    }
}

@Composable
private fun WalletTransferDialog(onDismiss: () -> Unit, onConfirm: (Double, Currency) -> Unit) {
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

/**
 * تحويل عملة إلى عملة أخرى: تُدخل المستخدمة المبلغ الذي "خرج" (مثلاً 1,500,000 ليرة) والمبلغ
 * الذي "دخل" فعلياً بدل ذلك (مثلاً 100 دولار) — بدون أي سعر صرف مجرّد قد يلخبط الاتجاه.
 */
@Composable
private fun CurrencyConvertDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Currency, PaymentMethod, Double, Currency, PaymentMethod) -> Unit
) {
    var amountOut by remember { mutableStateOf("") }
    var currencyOut by remember { mutableStateOf(Currency.SYP) }
    var methodOut by remember { mutableStateOf(PaymentMethod.CASH) }

    var amountIn by remember { mutableStateOf("") }
    var currencyIn by remember { mutableStateOf(Currency.USD) }
    var methodIn by remember { mutableStateOf(PaymentMethod.CASH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحويل بين العملات") },
        text = {
            Column {
                Text("من (المبلغ الذي أخرجتِه)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = amountOut, onValueChange = { amountOut = it }, label = { Text("المبلغ") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.padding(top = 6.dp)) {
                    Currency.values().forEach { c ->
                        FilterChip(selected = currencyOut == c, onClick = { currencyOut = c }, label = { Text(c.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                Row(Modifier.padding(top = 6.dp)) {
                    PaymentMethod.values().forEach { m ->
                        FilterChip(selected = methodOut == m, onClick = { methodOut = m }, label = { Text(m.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))
                Divider()
                Spacer(Modifier.height(14.dp))

                Text("إلى (المبلغ الذي استلمتِه فعلياً)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = amountIn, onValueChange = { amountIn = it }, label = { Text("المبلغ") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.padding(top = 6.dp)) {
                    Currency.values().forEach { c ->
                        FilterChip(selected = currencyIn == c, onClick = { currencyIn = c }, label = { Text(c.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                Row(Modifier.padding(top = 6.dp)) {
                    PaymentMethod.values().forEach { m ->
                        FilterChip(selected = methodIn == m, onClick = { methodIn = m }, label = { Text(m.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val out = amountOut.toDoubleOrNull() ?: return@TextButton
                val inn = amountIn.toDoubleOrNull() ?: return@TextButton
                onConfirm(out, currencyOut, methodOut, inn, currencyIn, methodIn)
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
                "${if (positive) "+" else "-"}${Formatters.amount(tx.amount, tx.currency)}",
                color = color, fontWeight = FontWeight.Bold
            )
        }
    }
}
