package com.example.whatsappreminder.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.ui.theme.StatusCancelled
import com.example.whatsappreminder.ui.theme.StatusExpired
import com.example.whatsappreminder.ui.theme.StatusNotified
import com.example.whatsappreminder.ui.theme.StatusOpened
import com.example.whatsappreminder.ui.theme.StatusPendingNetwork
import com.example.whatsappreminder.ui.theme.StatusScheduled

/**
 * يجمع العناصر المرئية لكل حالة تذكير: الأيقونة، اللون، والنص العربي.
 * يُستخدم في الشاشة الرئيسية وشاشة التفاصيل لتوحيد العرض.
 */
data class StatusVisual(
    val icon: ImageVector,
    val color: Color,
    val label: String
)

fun ReminderStatus.toVisual(): StatusVisual = when (this) {
    ReminderStatus.SCHEDULED -> StatusVisual(
        icon = Icons.Filled.Schedule,
        color = StatusScheduled,
        label = "مجدول"
    )
    ReminderStatus.PENDING_NETWORK -> StatusVisual(
        icon = Icons.Filled.SignalWifiOff,
        color = StatusPendingNetwork,
        label = "بانتظار الشبكة"
    )
    ReminderStatus.NOTIFIED -> StatusVisual(
        icon = Icons.Filled.Notifications,
        color = StatusNotified,
        label = "تم التنبيه"
    )
    ReminderStatus.OPENED -> StatusVisual(
        icon = Icons.Filled.CheckCircle,
        color = StatusOpened,
        label = "تم الفتح"
    )
    ReminderStatus.CANCELLED -> StatusVisual(
        icon = Icons.Filled.Cancel,
        color = StatusCancelled,
        label = "ملغى"
    )
    ReminderStatus.EXPIRED -> StatusVisual(
        icon = Icons.Filled.WarningAmber,
        color = StatusExpired,
        label = "فات الموعد"
    )
}
