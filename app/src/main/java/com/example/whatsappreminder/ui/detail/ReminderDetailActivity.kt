package com.example.whatsappreminder.ui.detail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.ui.theme.WhatsAppReminderTheme
import com.example.whatsappreminder.ui.toVisual
import com.example.whatsappreminder.util.DateFormatter
import com.example.whatsappreminder.util.NotificationHelper
import com.example.whatsappreminder.util.WhatsAppOpener
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * شاشة تفاصيل التذكير: عرض كامل + أزرار الإجراءات.
 * تُفتح من القائمة أو من الإشعار (عبر EXTRA_REMINDER_ID).
 */
@AndroidEntryPoint
class ReminderDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)

        setContent {
            WhatsAppReminderTheme {
                ReminderDetailScreen(
                    reminderId = reminderId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminderId: Long,
    onBack: () -> Unit,
    viewModel: ReminderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // تحميل التذكير مرة واحدة
    LaunchedEffect(reminderId) {
        if (reminderId != -1L) viewModel.load(reminderId)
    }

    // إغلاق الشاشة بعد الحذف
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل التذكير") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        val reminder = state.reminder
        if (reminder == null) {
            // أثناء التحميل أو إذا لم يُعثر على التذكير
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("جارٍ التحميل...")
            }
        } else {
            DetailContent(
                modifier = Modifier.padding(padding),
                reminder = reminder,
                onOpenWhatsApp = {
                    // تحديث الحالة إلى OPENED ثم فتح واتساب عبر Intent رسمي فقط
                    viewModel.markAsOpened()
                    WhatsAppOpener.openChat(
                        context = context,
                        phoneNumber = reminder.phoneNumber,
                        message = reminder.message
                    )
                },
                onCancel = { viewModel.cancelReminder() },
                onReschedule = {
                    showDateTimePicker(context) { newTime ->
                        viewModel.reschedule(newTime)
                    }
                },
                onDelete = { viewModel.deleteReminder() }
            )
        }
    }
}

@Composable
private fun DetailContent(
    modifier: Modifier = Modifier,
    reminder: Reminder,
    onOpenWhatsApp: () -> Unit,
    onCancel: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit
) {
    val visual = reminder.status.toVisual()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // بطاقة الحالة
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = visual.color.copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "الحالة: ${visual.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = visual.color,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // الحقول التفصيلية
        DetailField(label = "اسم جهة الاتصال", value = reminder.contactName)
        DetailField(label = "رقم الهاتف", value = reminder.phoneNumber)
        DetailField(label = "الرسالة", value = reminder.message)
        DetailField(
            label = "الموعد",
            value = DateFormatter.formatFull(reminder.scheduledTime)
        )
        if (reminder.notes.isNotBlank()) {
            DetailField(label = "ملاحظات", value = reminder.notes)
        }
        reminder.notifiedAt?.let {
            DetailField(label = "وقت التنبيه", value = DateFormatter.formatFull(it))
        }

        Spacer(Modifier.height(8.dp))

        // زر فتح واتساب والإرسال (المستخدم هو من يضغط الإرسال داخل واتساب)
        Button(
            onClick = onOpenWhatsApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("فتح واتساب وإرسال", style = MaterialTheme.typography.titleMedium)
        }

        // زر الإلغاء يظهر فقط إن كان التذكير مجدولاً
        if (reminder.status == ReminderStatus.SCHEDULED ||
            reminder.status == ReminderStatus.PENDING_NETWORK
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("إلغاء التذكير")
            }
        }

        // زر إعادة الجدولة يظهر إن فات الموعد أو تعذّرت الشبكة
        if (reminder.status == ReminderStatus.EXPIRED ||
            reminder.status == ReminderStatus.PENDING_NETWORK ||
            reminder.status == ReminderStatus.CANCELLED
        ) {
            OutlinedButton(
                onClick = onReschedule,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("إعادة الجدولة")
            }
        }

        // زر الحذف
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("حذف")
        }
    }
}

/** عنصر عرض حقل (عنوان + قيمة) داخل بطاقة */
@Composable
private fun DetailField(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * منتقي تاريخ ووقت لإعادة الجدولة.
 */
private fun showDateTimePicker(context: Context, onSelected: (Long) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSelected(selected.timeInMillis)
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
            ).show()
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = now.timeInMillis
    }.show()
}
