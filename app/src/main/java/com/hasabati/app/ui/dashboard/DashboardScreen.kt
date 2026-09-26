package com.hasabati.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasabati.app.data.db.entities.TransactionType
import com.hasabati.app.ui.common.*
import com.hasabati.app.ui.theme.*

@Composable
fun DashboardScreen(
    onNewOrder: () -> Unit,
    onCollectPayment: () -> Unit,
    onAddExpense: () -> Unit,
    onPayAgent: () -> Unit,
    onOpenTreasury: () -> Unit,
    onOpenAgent: () -> Unit,
    onOpenCustomersWithBalance: () -> Unit,
    onOpenArrivedOrders: () -> Unit
) {
    val vm = hasabatiViewModel { DashboardViewModel(it) }
    val state by vm.uiState.collectAsState()
    val snap = state.snapshot

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("حساباتي", style = MaterialTheme.typography.headlineMedium, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            Text("نظرة سريعة على وضعك المالي اليوم", color = TextSecondaryGray)
        }

        item {
            GradientHeroCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("الوضع المالي", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("💼", fontSize = 22.sp)
                }
                Spacer(Modifier.height(16.dp))
                val cashTotal = (snap?.treasury?.cashUsd ?: 0.0) + (snap?.treasury?.shamCashUsd ?: 0.0)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("معي", Formatters.usd(cashTotal), Color.White)
                    MiniStat("لي", Formatters.usd(snap?.customerReceivablesUsd ?: 0.0), Color(0xFFB9F5D8))
                    MiniStat("عليّ", Formatters.usd(snap?.agentPayableUsd ?: 0.0), Color(0xFFFFD1DC))
                }

                val cashSypTotal = (snap?.treasury?.cashSyp ?: 0.0) + (snap?.treasury?.shamCashSyp ?: 0.0)
                val cashSarTotal = (snap?.treasury?.cashSar ?: 0.0) + (snap?.treasury?.shamCashSar ?: 0.0)
                if (cashSypTotal > 0.5 || cashSarTotal > 0.5) {
                    Spacer(Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (cashSypTotal > 0.5) MiniStat("معي بالليرة", Formatters.syp(cashSypTotal), Color.White.copy(alpha = 0.9f))
                        if (cashSarTotal > 0.5) MiniStat("معي بالريال", Formatters.sar(cashSarTotal), Color.White.copy(alpha = 0.9f))
                    }
                }
            }
        }

        item {
            SectionTitle("الإجراءات السريعة")
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { QuickActionButton("طلب جديد", Icons.Filled.AddShoppingCart, PurpleAccent, onNewOrder) }
                item { QuickActionButton("تحصيل دفعة", Icons.Filled.Payments, SuccessGreen, onCollectPayment) }
                item { QuickActionButton("إضافة مصروف", Icons.Filled.Receipt, WarningAmber, onAddExpense) }
                item { QuickActionButton("دفع للوكيلة", Icons.Filled.LocalShipping, Color(0xFF2E86DE), onPayAgent) }
            }
        }

        item { SectionTitle("الأرصدة") }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(450.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { StatCard("النقد بالدولار", Formatters.usd(snap?.treasury?.cashUsd ?: 0.0), "💵", SuccessGreen, Modifier.clickable2(onOpenTreasury)) }
                item { StatCard("النقد بالليرة", Formatters.syp(snap?.treasury?.cashSyp ?: 0.0), "💴", WarningAmber, Modifier.clickable2(onOpenTreasury)) }
                item { StatCard("النقد بالريال", Formatters.sar(snap?.treasury?.cashSar ?: 0.0), "💰", Color(0xFF2E86DE), Modifier.clickable2(onOpenTreasury)) }
                item { StatCard("Sham Cash دولار", Formatters.usd(snap?.treasury?.shamCashUsd ?: 0.0), "📱", PurpleAccent, Modifier.clickable2(onOpenTreasury)) }
                item { StatCard("Sham Cash ليرة", Formatters.syp(snap?.treasury?.shamCashSyp ?: 0.0), "📱", PurpleAccentLight, Modifier.clickable2(onOpenTreasury)) }
                item { StatCard("Sham Cash ريال", Formatters.sar(snap?.treasury?.shamCashSar ?: 0.0), "📱", Color(0xFF0FA3B1), Modifier.clickable2(onOpenTreasury)) }
                item { StatCard("مستحقات العملاء", Formatters.usd(snap?.customerReceivablesUsd ?: 0.0), "👥", Color(0xFF2E86DE), Modifier.clickable2(onOpenCustomersWithBalance)) }
                item { StatCard("المستحق للوكيلة", Formatters.usd(snap?.agentPayableUsd ?: 0.0), "🏢", DangerRed, Modifier.clickable2(onOpenAgent)) }
                item { StatCard("طلبات وصلت ولم تُسلّم", (snap?.arrivedNotDeliveredCount ?: 0).toString(), "📦", Color(0xFF0FA3B1), Modifier.clickable2(onOpenArrivedOrders)) }
                item { StatCard("الربح المحقق", Formatters.usd(snap?.realizedProfitUsd ?: 0.0), "💰", SuccessGreen) }
            }
        }

        if ((snap?.arrivedNotDeliveredCount ?: 0) > 0 || (snap?.customersWithBalanceCount ?: 0) > 0 || (snap?.agentPayableUsd ?: 0.0) > 0.5) {
            item { SectionTitle("يحتاج متابعة") }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)) {
                    Column(Modifier.padding(14.dp)) {
                        if ((snap?.arrivedNotDeliveredCount ?: 0) > 0) AttentionRow("📦 لديك ${snap?.arrivedNotDeliveredCount} طلبات وصلت ولم تُسلَّم بعد")
                        if ((snap?.shippingCount ?: 0) > 0) AttentionRow("🚚 لديك ${snap?.shippingCount} طلبات قيد الشحن")
                        if ((snap?.customersWithBalanceCount ?: 0) > 0) AttentionRow("👥 لديك ${snap?.customersWithBalanceCount} عميلات عليهن مبالغ")
                        if ((snap?.agentPayableUsd ?: 0.0) > 0.5) AttentionRow("🏢 المستحق للوكيلة ${Formatters.usd(snap?.agentPayableUsd ?: 0.0)}")
                    }
                }
            }
        }

        item { SectionTitle("الحركات الأخيرة") }
        if (state.recentTransactions.isEmpty()) {
            item { EmptyState("لا توجد حركات بعد") }
        } else {
            items(state.recentTransactions) { tx ->
                RecentTxRow(tx)
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = color)
            Spacer(Modifier.height(6.dp))
            Text(label, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AttentionRow(text: String) {
    Text(text, modifier = Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun RecentTxRow(tx: com.hasabati.app.data.db.entities.Transaction) {
    val isPositive = tx.type == TransactionType.CUSTOMER_PAYMENT || tx.type == TransactionType.CAPITAL_ADDITION ||
        tx.type == TransactionType.CAPITAL_INITIAL || tx.type == TransactionType.TRANSFER_IN
    val color = if (isPositive) SuccessGreen else DangerRed
    val sign = if (isPositive) "+" else "-"
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(tx.type.arabicLabel, fontWeight = FontWeight.SemiBold)
                Text(Formatters.dateTime(tx.createdAt), style = MaterialTheme.typography.bodyMedium, color = TextSecondaryGray)
                if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodyMedium, color = TextSecondaryGray)
            }
            Text(
                "$sign${Formatters.amount(tx.amount, tx.currency)}",
                color = color, fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun Modifier.clickable2(onClick: () -> Unit): Modifier =
    this.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
