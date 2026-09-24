package com.hasabati.app.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hasabati.app.data.db.entities.Currency
import com.hasabati.app.data.db.entities.OrderStatus
import com.hasabati.app.data.db.entities.PaymentMethod
import com.hasabati.app.ui.common.*
import com.hasabati.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(orderId: Long, onBack: () -> Unit) {
    val vm = hasabatiViewModel { OrderDetailViewModel(it, orderId) }
    val state by vm.uiState.collectAsState()
    val order = state.order

    var showArrivalDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(order?.orderNumber ?: "الطلب") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (order == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(state.customer?.name ?: "—", style = MaterialTheme.typography.titleLarge)
                        Text(Formatters.date(order.createdAt), color = TextSecondaryGray)
                    }
                    StatusChip(order.status.arabicLabel, order.status.color())
                }
            }

            if (order.status != OrderStatus.CANCELLED && order.status != OrderStatus.DELIVERED) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (order.status == OrderStatus.SHIPPING) {
                            Button(onClick = { showArrivalDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)) {
                                Text("تأكيد الوصول")
                            }
                        } else {
                            val next = order.status.next()
                            if (next != null) {
                                Button(onClick = { vm.advanceStatus() }, colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)) {
                                    Text("نقل إلى: ${next.arabicLabel}")
                                }
                            }
                        }
                        OutlinedButton(onClick = { showCancelDialog = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)) {
                            Text("إلغاء الطلب")
                        }
                    }
                }
            }

            item { SectionTitle("المنتجات") }
            items(state.items) { item ->
                Card(shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.productName, fontWeight = FontWeight.SemiBold)
                        Text("الكمية: ${item.quantity}", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تكلفة متوقعة: ${Formatters.usd(item.expectedCostTotal)}", style = MaterialTheme.typography.bodyMedium)
                            if (item.actualCostTotal != null) {
                                Text("تكلفة فعلية: ${Formatters.usd(item.actualCostTotal!!)}", style = MaterialTheme.typography.bodyMedium, color = WarningAmber)
                            }
                            Text("بيع: ${Formatters.usd(item.saleTotal)}", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = NavySurface)) {
                    Column(Modifier.padding(14.dp)) {
                        SummaryRow("إجمالي البيع", Formatters.usd(order.saleTotalUsd), Color2.White)
                        SummaryRow("المدفوع", Formatters.usd(state.paidUsd), Color2.Green)
                        SummaryRow("المتبقي", Formatters.usd(state.remainingUsd), if (state.remainingUsd > 0.009) Color2.Red else Color2.Green)
                        if (order.actualCostUsd != null) {
                            SummaryRow("الربح الفعلي", Formatters.usd(order.saleTotalUsd - order.actualCostUsd), Color2.Green)
                        } else {
                            SummaryRow("الربح المتوقع", Formatters.usd(order.saleTotalUsd - order.expectedCostUsd), Color2.Green)
                        }
                    }
                }
            }

            if (order.status != OrderStatus.CANCELLED && state.remainingUsd > 0.009) {
                item {
                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) { Text("تحصيل دفعة") }
                }
            }

            if (order.status == OrderStatus.CANCELLED) {
                item {
                    val label = when (order.depositRefunded) {
                        true -> "تم رد العربون للعميلة"
                        false -> "لم يُرد العربون بعد"
                        null -> "لا يوجد عربون على هذا الطلب"
                    }
                    Text(label, color = TextSecondaryGray)
                }
            }

            item { SectionTitle("سجل العمليات على هذا الطلب") }
            if (state.transactions.isEmpty()) {
                item { EmptyState("لا توجد عمليات بعد") }
            } else {
                items(state.transactions) { tx ->
                    Card(shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(tx.type.arabicLabel, fontWeight = FontWeight.SemiBold)
                                Text(Formatters.dateTime(tx.createdAt), color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                if (tx.currency == Currency.USD) Formatters.usd(tx.amount) else Formatters.syp(tx.amount),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(50.dp)) }
        }
    }

    if (showArrivalDialog) {
        ArrivalDialog(items = state.items, onDismiss = { showArrivalDialog = false }, onConfirm = {
            vm.confirmArrival(it); showArrivalDialog = false
        })
    }
    if (showPaymentDialog) {
        PaymentDialog(maxAmount = state.remainingUsd, onDismiss = { showPaymentDialog = false }, onConfirm = { amount, currency, method, rate ->
            vm.collectPayment(amount, currency, method, rate); showPaymentDialog = false
        })
    }
    if (showCancelDialog) {
        CancelOrderDialog(hasPayments = state.paidUsd > 0.0, onDismiss = { showCancelDialog = false }, onConfirm = {
            vm.cancelOrder(it); showCancelDialog = false
        })
    }
}

private object Color2 {
    val White = androidx.compose.ui.graphics.Color.White
    val Green = androidx.compose.ui.graphics.Color(0xFF9FE8B5)
    val Red = androidx.compose.ui.graphics.Color(0xFFF3B6B6)
}

@Composable
private fun SummaryRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f))
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArrivalDialog(
    items: List<com.hasabati.app.data.db.entities.OrderItem>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Long, Double>) -> Unit
) {
    val costs = remember { mutableStateMapOf<Long, String>().apply { items.forEach { put(it.id, it.expectedUnitCostUsd.toString()) } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد الوصول — أدخل التكلفة الفعلية") },
        text = {
            Column {
                items.forEach { item ->
                    OutlinedTextField(
                        value = costs[item.id] ?: "",
                        onValueChange = { costs[item.id] = it },
                        label = { Text("${item.productName} (تكلفة الوحدة $)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(items.associate { it.id to (costs[it.id]?.toDoubleOrNull() ?: it.expectedUnitCostUsd) })
            }) { Text("تأكيد") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun PaymentDialog(
    maxAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Currency, PaymentMethod, Double?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currency.USD) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var rate by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحصيل دفعة") },
        text = {
            Column {
                Text("المتبقي: ${Formatters.usd(maxAmount)}", color = TextSecondaryGray)
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("المبلغ") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.padding(top = 8.dp)) {
                    Currency.values().forEach { c ->
                        FilterChip(selected = currency == c, onClick = { currency = c }, label = { Text(c.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                Row(Modifier.padding(top = 8.dp)) {
                    PaymentMethod.values().forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.arabicLabel) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                if (currency == Currency.SYP) {
                    OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("سعر الصرف") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@TextButton
                onConfirm(amt, currency, method, if (currency == Currency.SYP) rate.toDoubleOrNull() else null)
            }) { Text("تأكيد") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun CancelOrderDialog(hasPayments: Boolean, onDismiss: () -> Unit, onConfirm: (Boolean?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إلغاء الطلب") },
        text = {
            Text(if (hasPayments) "هذا الطلب عليه دفعات مسجلة. هل تم رد العربون للعميلة؟" else "هل أنت متأكدة من إلغاء هذا الطلب؟")
        },
        confirmButton = {
            if (hasPayments) {
                TextButton(onClick = { onConfirm(true) }) { Text("نعم، تم الرد") }
            } else {
                TextButton(onClick = { onConfirm(null) }) { Text("تأكيد الإلغاء") }
            }
        },
        dismissButton = {
            if (hasPayments) TextButton(onClick = { onConfirm(false) }) { Text("لا، لم يُرد بعد") }
            else TextButton(onClick = onDismiss) { Text("تراجع") }
        }
    )
}
