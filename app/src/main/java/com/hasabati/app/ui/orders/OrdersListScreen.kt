package com.hasabati.app.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hasabati.app.ui.common.EmptyState
import com.hasabati.app.ui.common.Formatters
import com.hasabati.app.ui.common.SearchBarField
import com.hasabati.app.ui.common.StatusChip
import com.hasabati.app.ui.common.color
import com.hasabati.app.ui.common.hasabatiViewModel
import com.hasabati.app.ui.theme.AppShapes
import com.hasabati.app.ui.theme.BackgroundLight
import com.hasabati.app.ui.theme.BorderGray
import com.hasabati.app.ui.theme.DangerRed
import com.hasabati.app.ui.theme.PurpleAccent
import com.hasabati.app.ui.theme.SuccessGreen
import com.hasabati.app.ui.theme.TextPrimaryDark
import com.hasabati.app.ui.theme.TextSecondaryGray

@Composable
fun OrdersListScreen(onOpenOrder: (Long) -> Unit, onNewOrder: () -> Unit) {
    val vm = hasabatiViewModel { OrdersListViewModel(it) }
    val rows by vm.rows.collectAsState()
    val filter by vm.filter.collectAsState()
    val query by vm.query.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewOrder, containerColor = PurpleAccent, shape = AppShapes.large) {
                Icon(Icons.Filled.Add, contentDescription = "طلب جديد")
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Text("الطلبات", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Spacer(Modifier.height(12.dp))
                // شريط بحث موحّد من Components.kt بدل OutlinedTextField يدوي (نفس vm.setQuery تماماً)
                SearchBarField(
                    value = query,
                    onValueChange = { vm.setQuery(it) },
                    placeholder = "ابحثي باسم العميلة أو رقم الطلب..."
                )
            }
            LazyRowFilters(filter) { vm.setFilter(it) }
            Spacer(Modifier.height(10.dp))
            if (rows.isEmpty()) {
                EmptyState(
                    message = "لا توجد طلبات مطابقة",
                    icon = Icons.Filled.ShoppingBag,
                    actionLabel = "إضافة طلب",
                    onAction = onNewOrder
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rows) { row ->
                        OrderRowCard(row, onClick = { onOpenOrder(row.order.id) })
                    }
                    item { Spacer(Modifier.height(70.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LazyRowFilters(selected: OrdersListViewModel.Filter, onSelect: (OrdersListViewModel.Filter) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(OrdersListViewModel.Filter.entries) { f ->
            FilterChip(
                selected = f == selected,
                onClick = { onSelect(f) },
                label = { Text(f.label) },
                shape = AppShapes.pill,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PurpleAccent.copy(alpha = 0.16f),
                    selectedLabelColor = PurpleAccent
                )
            )
        }
    }
}

@Composable
private fun OrderRowCard(row: OrdersListViewModel.OrderRow, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
    ) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(row.order.orderNumber, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text(row.customerName, color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                }
                StatusChip(row.order.status.arabicLabel, row.order.status.color())
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("الإجمالي", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                    Text(Formatters.usd(row.order.saleTotalUsd), fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("المدفوع", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                    Text(Formatters.usd(row.paidUsd), fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                }
                Column {
                    Text("المتبقي", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Formatters.usd(row.remainingUsd),
                        fontWeight = FontWeight.SemiBold,
                        color = if (row.remainingUsd > 0.009) DangerRed else SuccessGreen
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(Formatters.date(row.order.createdAt), color = TextSecondaryGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
