package com.example.whatsappreminder.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.ui.add.AddReminderActivity
import com.example.whatsappreminder.ui.detail.ReminderDetailActivity
import com.example.whatsappreminder.ui.theme.WhatsAppReminderTheme
import com.example.whatsappreminder.ui.toVisual
import com.example.whatsappreminder.util.DateFormatter
import com.example.whatsappreminder.util.NotificationHelper
import com.example.whatsappreminder.util.SettingsPreferences
import com.example.whatsappreminder.util.WhatsAppOpener
import dagger.hilt.android.AndroidEntryPoint

/**
 * الشاشة الرئيسية: تعرض قائمة التذكيرات وتتيح إضافة/تصدير/استيراد التذكيرات.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // عرض شاشة البداية الرسمية قبل تحميل الواجهة
        installSplashScreen()
        super.onCreate(savedInstanceState)

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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    // Snackbar للتراجع عن الحذف
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // التذكير المُنتظر تأكيد حذفه
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    // إظهار حوار رمز الدولة / حول التطبيق
    var showCountryDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    // شاشة الترحيب/الأذونات عند أول تشغيل
    val settingsPrefs = remember { SettingsPreferences(context) }
    var showOnboarding by remember { mutableStateOf(!settingsPrefs.isOnboardingDone()) }
    val runtimePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* استلمنا إجابة المستخدم */ }

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

    // مُطلق منتقي نغمة التنبيه (منتقي النظام الرسمي)
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            @Suppress("DEPRECATION")
            val picked: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            // حفظ النغمة وإعادة إنشاء القناة بها
            NotificationHelper(context).updateSound(picked)
            Toast.makeText(context, "تم تحديث نغمة التنبيه", Toast.LENGTH_SHORT).show()
        }
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
                        onAutoOpen = {
                            if (Settings.canDrawOverlays(context)) {
                                Toast.makeText(
                                    context, "الفتح التلقائي مُفعّل بالفعل", Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "فعّل «العرض فوق التطبيقات الأخرى» لهذا التطبيق",
                                    Toast.LENGTH_LONG
                                ).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                runCatching { context.startActivity(intent) }
                            }
                        },
                        onCountryCode = { showCountryDialog = true },
                        onAbout = { showAbout = true },
                        onExport = { viewModel.requestExport() },
                        onImport = { openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                        onPickSound = {
                            // بناء نية منتقي النغمات الرسمي من النظام
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_NOTIFICATION
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة التنبيه")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    NotificationHelper(context).currentSoundUri()
                                )
                            }
                            runCatching { ringtonePickerLauncher.launch(intent) }
                        }
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // شريط حالة الأذونات (يظهر فقط عند وجود إعداد ناقص)
            PermissionsBanner()

            if (uiState.reminders.isEmpty() && !uiState.isLoading) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                // تجميع: قادمة (نشطة) ومنتهية
                val active = uiState.reminders.filter {
                    it.status == ReminderStatus.SCHEDULED ||
                        it.status == ReminderStatus.PENDING_NETWORK ||
                        it.status == ReminderStatus.NOTIFIED
                }
                val done = uiState.reminders.filter {
                    it.status == ReminderStatus.OPENED ||
                        it.status == ReminderStatus.CANCELLED ||
                        it.status == ReminderStatus.EXPIRED
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (active.isNotEmpty()) {
                        item(key = "h_active") { SectionHeader("قادمة (${active.size})") }
                        items(active, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                modifier = Modifier.animateItemPlacement(),
                                onClick = { onItemClick(reminder) },
                                onDelete = { pendingDelete = reminder }
                            )
                        }
                    }
                    if (done.isNotEmpty()) {
                        item(key = "h_done") { SectionHeader("منتهية (${done.size})") }
                        items(done, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                modifier = Modifier.animateItemPlacement(),
                                onClick = { onItemClick(reminder) },
                                onDelete = { pendingDelete = reminder }
                            )
                        }
                    }
                }
            }
        }
    }

    // شاشة الترحيب لطلب كل الأذونات عند أول تشغيل
    if (showOnboarding) {
        OnboardingDialog(
            onGrantRuntime = {
                val perms = buildList {
                    add(Manifest.permission.READ_CONTACTS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                runtimePermLauncher.launch(perms)
            },
            onOpenOverlay = {
                if (!Settings.canDrawOverlays(context)) {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                } else {
                    Toast.makeText(context, "مُفعّل بالفعل", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenBattery = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            },
            onDone = {
                settingsPrefs.setOnboardingDone()
                showOnboarding = false
            }
        )
    }

    // فحص التذكيرات الفائتة: موعدها مضى وما زالت مجدولة (لم يُطلق تنبيهها)
    val missed = remember(uiState.reminders) {
        val now = System.currentTimeMillis()
        uiState.reminders.filter {
            it.scheduledTime <= now &&
                (it.status == ReminderStatus.SCHEDULED ||
                    it.status == ReminderStatus.PENDING_NETWORK)
        }
    }
    if (!showOnboarding && missed.isNotEmpty()) {
        MissedReminderDialog(
            reminder = missed.first(),
            onSend = { r ->
                WhatsAppOpener.openChat(context, r.phoneNumber, r.message)
                viewModel.markOpened(r)
            },
            onDismiss = { r -> viewModel.dismissMissed(r) }
        )
    }

    // تأكيد الحذف + إتاحة التراجع عبر Snackbar
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.deleteReminder(target)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "تم حذف التذكير",
                            actionLabel = "تراجع",
                            withDismissAction = true
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete(target)
                        }
                    }
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") }
            },
            title = { Text("حذف التذكير") },
            text = { Text("هل تريد حذف هذا التذكير؟") }
        )
    }

    // حوار رمز الدولة الافتراضي
    if (showCountryDialog) {
        CountryCodeDialog(
            current = settingsPrefs.getDefaultCountryCode(),
            onSave = { code ->
                settingsPrefs.setDefaultCountryCode(code)
                showCountryDialog = false
                Toast.makeText(context, "تم حفظ رمز الدولة", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCountryDialog = false }
        )
    }

    // حوار حول التطبيق
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

/** حوار "حول التطبيق" مع الاسم والإصدار ونبذة أمنية */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("حسناً") } },
        title = { Text("تذكيرات واتساب") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الإصدار $version", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "تطبيق يُذكّرك بإرسال رسائل واتساب في مواعيدها، ثم تفتح المحادثة " +
                        "والرسالة جاهزة لترسلها بنفسك.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "🔒 تذكير فقط — لا إرسال تلقائي، ولا أتمتة لواتساب. يحترم شروط " +
                        "الاستخدام ويحمي حسابك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/** شريط يظهر أعلى الرئيسية عند وجود إعداد أذونات ناقص، مع أزرار إصلاح سريعة */
@Composable
private fun PermissionsBanner() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // إعادة الفحص عند العودة للتطبيق من الإعدادات
    var tick by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifOk = remember(tick) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val overlayOk = remember(tick) { Settings.canDrawOverlays(context) }
    val batteryOk = remember(tick) {
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
    }

    if (notifOk && overlayOk && batteryOk) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "لضمان عمل التذكيرات بدقة، فعّل:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (!notifOk) {
                BannerRow("الإشعارات") {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                }
            }
            if (!overlayOk) {
                BannerRow("الفتح التلقائي (العرض فوق التطبيقات)") {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
            }
            if (!batteryOk) {
                BannerRow("عدم تقييد البطارية") {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
            }
        }
    }
}

/** صف داخل شريط الأذونات: نص + زر تفعيل */
@Composable
private fun BannerRow(label: String, onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Button(onClick = onFix) { Text("تفعيل") }
    }
}

/** حوار إدخال رمز الدولة الافتراضي */
@Composable
private fun CountryCodeDialog(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(code) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("رمز الدولة الافتراضي") },
        text = {
            Column {
                Text(
                    "يُضاف تلقائياً للأرقام المحلية (مثل 05...) لتُفتح في واتساب. " +
                        "أدخل رمز دولتك بالأرقام فقط.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { v -> code = v.filter { it.isDigit() } },
                    label = { Text("رمز الدولة") },
                    placeholder = { Text("مثال: 966") },
                    singleLine = true
                )
            }
        }
    )
}

/**
 * حوار يُعرض عند وجود تذكير فائت (مضى موعده دون تنبيه، مثلاً كان الجهاز مغلقاً).
 * يسأل المستخدم إن كان يرغب بإرسال الرسالة الآن.
 */
@Composable
private fun MissedReminderDialog(
    reminder: Reminder,
    onSend: (Reminder) -> Unit,
    onDismiss: (Reminder) -> Unit
) {
    val title = reminder.contactName.ifBlank { reminder.phoneNumber }
    AlertDialog(
        onDismissRequest = { /* يُغلق فقط بأحد الزرين */ },
        confirmButton = {
            Button(onClick = { onSend(reminder) }) { Text("إرسال الآن") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(reminder) }) { Text("تجاهل") }
        },
        title = { Text("لديك تذكير فائت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("فات موعد إرسال رسالة إلى: $title")
                Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "الموعد: ${DateFormatter.formatFull(reminder.scheduledTime)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("هل ترغب بإرسالها الآن؟", style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

/** شاشة ترحيب تطلب كل الأذونات المطلوبة عند أول تشغيل */
@Composable
private fun OnboardingDialog(
    onGrantRuntime: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenBattery: () -> Unit,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* لا يُغلق إلا بزر تم */ },
        confirmButton = {
            TextButton(onClick = onDone) { Text("تم") }
        },
        title = { Text("الأذونات المطلوبة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "لكي يعمل التطبيق بدقة، امنح الأذونات التالية بالترتيب:",
                    style = MaterialTheme.typography.bodyMedium
                )
                PermissionRow(
                    title = "الإشعارات وجهات الاتصال",
                    desc = "لعرض التذكير والبحث عن الأرقام",
                    button = "منح",
                    onClick = onGrantRuntime
                )
                PermissionRow(
                    title = "الفتح التلقائي لواتساب",
                    desc = "العرض فوق التطبيقات الأخرى",
                    button = "فتح الإعدادات",
                    onClick = onOpenOverlay
                )
                PermissionRow(
                    title = "عدم تقييد البطارية",
                    desc = "لضمان دقة المواعيد على هواوي",
                    button = "فتح الإعدادات",
                    onClick = onOpenBattery
                )
            }
        }
    )
}

/** صف إذن داخل شاشة الترحيب */
@Composable
private fun PermissionRow(
    title: String,
    desc: String,
    button: String,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onClick) { Text(button) }
    }
}

/** قائمة منسدلة (⋮) للنغمة والتصدير والاستيراد */
@Composable
private fun OverflowMenu(
    onAutoOpen: () -> Unit,
    onCountryCode: () -> Unit,
    onAbout: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onPickSound: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("تفعيل الفتح التلقائي") },
            leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null) },
            onClick = { expanded = false; onAutoOpen() }
        )
        DropdownMenuItem(
            text = { Text("رمز الدولة الافتراضي") },
            leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null) },
            onClick = { expanded = false; onCountryCode() }
        )
        DropdownMenuItem(
            text = { Text("نغمة التنبيه") },
            leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
            onClick = { expanded = false; onPickSound() }
        )
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
        DropdownMenuItem(
            text = { Text("حول التطبيق") },
            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            onClick = { expanded = false; onAbout() }
        )
    }
}

/** عنوان قسم داخل القائمة */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

/** شارة الحالة (Pill ملوّن مع أيقونة) */
@Composable
private fun StatusChip(reminder: Reminder) {
    val visual = reminder.status.toVisual()
    Surface(
        shape = RoundedCornerShape(50),
        color = visual.color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = visual.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = visual.color
            )
        }
    }
}

/** بطاقة عرض تذكير واحد في القائمة */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.contactName.ifBlank { reminder.phoneNumber },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(reminder)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = DateFormatter.formatRelative(reminder.scheduledTime),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
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
