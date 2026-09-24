package com.hasabati.app.ui.common

import androidx.compose.ui.graphics.Color
import com.hasabati.app.data.db.entities.OrderStatus
import com.hasabati.app.ui.theme.DangerRed
import com.hasabati.app.ui.theme.PurpleAccent
import com.hasabati.app.ui.theme.SuccessGreen
import com.hasabati.app.ui.theme.TextSecondaryGray
import com.hasabati.app.ui.theme.WarningAmber

fun OrderStatus.color(): Color = when (this) {
    OrderStatus.CONFIRMED -> PurpleAccent
    OrderStatus.SHIPPING -> WarningAmber
    OrderStatus.ARRIVED -> Color(0xFF2E86DE)
    OrderStatus.READY_FOR_DELIVERY -> Color(0xFF0FA3B1)
    OrderStatus.DELIVERED -> SuccessGreen
    OrderStatus.CANCELLED -> DangerRed
}

fun OrderStatus.next(): OrderStatus? = when (this) {
    OrderStatus.CONFIRMED -> OrderStatus.SHIPPING
    OrderStatus.SHIPPING -> OrderStatus.ARRIVED
    OrderStatus.ARRIVED -> OrderStatus.READY_FOR_DELIVERY
    OrderStatus.READY_FOR_DELIVERY -> OrderStatus.DELIVERED
    OrderStatus.DELIVERED -> null
    OrderStatus.CANCELLED -> null
}
