package com.hasabati.app.ui.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hasabati.app.ui.common.*
import com.hasabati.app.ui.customers.SimplePaymentDialog
import com.hasabati.app.ui.theme.*

@Composable
fun AgentScreen() {
    val vm = hasabatiViewModel { AgentViewModel(it) }
    val state by vm.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showDialog = true }, containerColor = PurpleAccent) {
                Text("دفع للوكيلة")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("حساب الوكيلة", style = MaterialTheme.typography.headlineMedium) }
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = NavySurface)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي مستحق للوكيلة", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f))
                            Text(Formatters.usd(state.totalOwedUsd), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المدفوع", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f))
                            Text(Formatters.usd(state.paidUsd), color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        Divider(Modifier.padding(vertical = 10.dp), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المتبقي", color = androidx.compose.ui.graphics.Color.White)
                            Text(Formatters.usd(state.remainingUsd), color = if (state.remainingUsd > 0.009) DangerRed else SuccessGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { SectionTitle("سجل الدفعات للوكيلة") }
            if (state.payments.isEmpty()) {
                item { EmptyState("لا توجد دفعات بعد") }
            } else {
                items(state.payments) { tx ->
                    Card(shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(Formatters.dateTime(tx.createdAt), fontWeight = FontWeight.SemiBold)
                                Text(tx.method.arabicLabel, color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                                if (tx.note.isNotBlank()) Text(tx.note, color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("-${Formatters.usd(tx.usdEquivalent)}", color = DangerRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }

    if (showDialog) {
        SimplePaymentDialog(
            title = "دفع للوكيلة",
            maxAmount = state.remainingUsd,
            onDismiss = { showDialog = false },
            onConfirm = { amount, currency, method, rate ->
                vm.payAgent(amount, currency, method, rate, "دفعة للوكيلة")
                showDialog = false
            }
        )
    }
}
