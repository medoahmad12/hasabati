package com.hasabati.app.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hasabati.app.ui.common.EmptyState
import com.hasabati.app.ui.common.Formatters
import com.hasabati.app.ui.common.StatusChip
import com.hasabati.app.ui.common.color
import com.hasabati.app.ui.common.hasabatiViewModel
import com.hasabati.app.ui.theme.BorderGray
import com.hasabati.app.ui.theme.DangerRed
import com.hasabati.app.ui.theme.PurpleAccent
import com.hasabati.app.ui.theme.SuccessGreen
import com.hasabati.app.ui.theme.TextSecondaryGray

@Composable
fun OrdersListScreen(onOpenOrder: (Long) -> Unit, onNewOrder: () -> Unit) {
    val vm = hasabatiViewModel { OrdersListViewModel(it) }
    val rows by vm.rows.collectAsState()
    val filter by vm.filter.collectAsState()
    val query by vm.query.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewOrder, containerColor = PurpleAccent) {
                Icon(Icons.Filled.Add, contentDescription = "طلب جديد")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Text("الطلبات", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.setQuery(it) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("ابحث برقم الطلب أو اسم العميلة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            LazyRowFilters(filter) { vm.setFilter(it) }
            Spacer(Modifier.height(6.dp))
            if (rows.isEmpty()) {
                EmptyState("لا توجد طلبات مطابقة")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
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
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleAccent.copy(alpha = 0.16f))
            )
        }
    }
}

@Composable
private fun OrderRowCard(row: OrdersListViewModel.OrderRow, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
    ) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(row.order.orderNumber, fontWeight = FontWeight.Bold)
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
            Spacer(Modifier.height(4.dp))
            Text(Formatters.date(row.order.createdAt), color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
