package com.example.whatsappreminder.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.ui.add.AddReminderActivity
import com.example.whatsappreminder.ui.detail.ReminderDetailActivity
import com.example.whatsappreminder.ui.theme.WhatsAppReminderTheme
import com.example.whatsappreminder.ui.toVisual
import com.example.whatsappreminder.util.DateFormatter
import com.example.whatsappreminder.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint

/**
 * الشاشة الرئيسية: تعرض قائمة التذكيرات وتتيح إضافة/تصدير/استيراد التذكيرات.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // طلب صلاحية الإشعارات على Android 13+ عند أول تشغيل
        requestNotificationPermissionIfNeeded()

        setContent {
            WhatsAppReminderTheme {
                MainScreen(
                    onAddClick = {
                        startActivity(Intent(this, AddReminderActivity::class.java))
                    },
                    onItemClick = { reminder ->
                        val intent = Intent(this, ReminderDetailActivity::class.java).apply {
                            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminder.id)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    // مُسجّل طلب صلاحية الإشعارات
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* لا إجراء إضافي */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAddClick: () -> Unit,
    onItemClick: (Reminder) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    // مراقبة حالة الواجهة عبر StateFlow
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // نص التصدير المؤقت بانتظار اختيار المستخدم لمكان الحفظ
    var pendingExport by remember { mutableStateOf<String?>(null) }

    // مُطلق حفظ ملف النسخة الاحتياطية
    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExport
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
            Toast.makeText(context, "تم حفظ النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
        }
        pendingExport = null
    }

    // مُطلق اختيار ملف نسخة احتياطية للاستيراد
    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text != null) viewModel.importFromJson(text)
        }
    }

    // استقبال الأحداث لمرة واحدة (تصدير/رسائل)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.ExportReady -> {
                    pendingExport = event.json
                    createDocLauncher.launch("whatsapp_reminders_backup.json")
                }
                is MainEvent.Message -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تذكيرات واتساب") },
                actions = {
                    OverflowMenu(
                        onExport = { viewModel.requestExport() },
                        onImport = { openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة تذكير")
            }
        }
    ) { padding ->
        if (uiState.reminders.isEmpty() && !uiState.isLoading) {
            // حالة القائمة الفارغة
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onClick = { onItemClick(reminder) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                }
            }
        }
    }
}

/** قائمة منسدلة (⋮) لخيارات التصدير والاستيراد */
@Composable
private fun OverflowMenu(
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("تصدير نسخة احتياطية") },
            leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
            onClick = { expanded = false; onExport() }
        )
        DropdownMenuItem(
            text = { Text("استيراد نسخة احتياطية") },
            leadingIcon = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
            onClick = { expanded = false; onImport() }
        )
    }
}

/** بطاقة عرض تذكير واحد في القائمة */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val visual = reminder.status.toVisual()

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.contactName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                // مقتطف من الرسالة (سطر واحد)
                Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = DateFormatter.formatShort(reminder.scheduledTime),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                // شارة الحالة مع أيقونة ملونة
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = visual.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = visual.color
                    )
                }
            }
            // زر حذف سريع
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** واجهة تُعرض عند عدم وجود أي تذكيرات */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "لا توجد تذكيرات بعد",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "اضغط زر + لإضافة أول تذكير",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
