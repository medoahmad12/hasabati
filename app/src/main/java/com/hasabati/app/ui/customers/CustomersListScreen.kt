package com.hasabati.app.ui.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hasabati.app.ui.common.EmptyState
import com.hasabati.app.ui.common.Formatters
import com.hasabati.app.ui.common.hasabatiViewModel
import com.hasabati.app.ui.theme.BorderGray
import com.hasabati.app.ui.theme.DangerRed
import com.hasabati.app.ui.theme.PurpleAccent
import com.hasabati.app.ui.theme.SuccessGreen
import com.hasabati.app.ui.theme.TextSecondaryGray

@Composable
fun CustomersListScreen(onOpenCustomer: (Long) -> Unit) {
    val vm = hasabatiViewModel { CustomersListViewModel(it) }
    val rows by vm.rows.collectAsState()
    val query by vm.query.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = PurpleAccent) {
                Icon(Icons.Filled.Add, contentDescription = "عميلة جديدة")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Text("العملاء", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.setQuery(it) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("ابحث بالاسم أو رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            if (rows.isEmpty()) {
                EmptyState("لا يوجد عملاء بعد")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(rows) { row ->
                        Card(
                            onClick = { onOpenCustomer(row.customer.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(row.customer.name, fontWeight = FontWeight.Bold)
                                    Text("${row.orderCount} طلبات", color = TextSecondaryGray)
                                }
                                if (row.customer.phone.isNotBlank()) {
                                    Text(row.customer.phone, color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column { Text("إجمالي", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium); Text(Formatters.usd(row.totalSalesUsd), fontWeight = FontWeight.SemiBold) }
                                    Column { Text("مدفوع", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium); Text(Formatters.usd(row.paidUsd), fontWeight = FontWeight.SemiBold, color = SuccessGreen) }
                                    Column {
                                        Text("متبقي", color = TextSecondaryGray, style = MaterialTheme.typography.bodyMedium)
                                        Text(Formatters.usd(row.remainingUsd), fontWeight = FontWeight.SemiBold, color = if (row.remainingUsd > 0.009) DangerRed else SuccessGreen)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(70.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(onDismiss = { showAddDialog = false }, onSave = { name, phone ->
            vm.addCustomer(name, phone, "") { showAddDialog = false }
        })
    }
}

@Composable
private fun AddCustomerDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عميلة جديدة") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), phone.trim()) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
