package com.hasabati.app.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hasabati.app.data.db.entities.Currency
import com.hasabati.app.data.db.entities.ExpenseCategories
import com.hasabati.app.data.db.entities.PaymentMethod
import com.hasabati.app.ui.common.*
import com.hasabati.app.ui.theme.*

@Composable
fun ExpensesScreen() {
    val vm = hasabatiViewModel { ExpensesViewModel(it) }
    val expenses by vm.expenses.collectAsState()
    val total by vm.totalUsd.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = WarningAmber) {
                Icon(Icons.Filled.Add, contentDescription = "مصروف جديد")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("المصاريف", style = MaterialTheme.typography.headlineMedium) }
            item { StatCard("إجمالي المصاريف", Formatters.usd(total), "🧾", WarningAmber, Modifier.fillMaxWidth()) }
            item { SectionTitle("السجل") }
            if (expenses.isEmpty()) {
                item { EmptyState("لا توجد مصاريف بعد") }
            } else {
                items(expenses) { tx ->
                    Card(shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(tx.expenseCategory ?: "مصروف", fontWeight = FontWeight.SemiBold)
                                Text(Formatters.dateTime(tx.createdAt), color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                                if (tx.note.isNotBlank()) Text(tx.note, color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                "-${if (tx.currency == Currency.USD) Formatters.usd(tx.amount) else Formatters.syp(tx.amount)}",
                                color = DangerRed, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }

    if (showDialog) {
        AddExpenseDialog(onDismiss = { showDialog = false }, onSave = { amount, currency, method, rate, category, note ->
            vm.addExpense(amount, currency, method, rate, category, note)
            showDialog = false
        })
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (Double, Currency, PaymentMethod, Double?, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currency.USD) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var rate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategories.defaults.first()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مصروف جديد") },
        text = {
            Column {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("المبلغ") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExpenseCategories.defaults) { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Currency.values().forEach { c ->
                        FilterChip(selected = currency == c, onClick = { currency = c }, label = { Text(c.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    PaymentMethod.values().forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                if (currency == Currency.SYP) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("سعر الصرف") }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("ملاحظة") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@TextButton
                onSave(amt, currency, method, if (currency == Currency.SYP) rate.toDoubleOrNull() else null, category, note)
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
