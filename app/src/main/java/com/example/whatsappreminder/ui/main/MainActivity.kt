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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DismissValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import com.example.whatsappreminder.util.composeMessage
import dagger.hilt.android.AndroidEntryPoint

/**
 * الشاشة الرئيسية: تعرض قائمة التذكيرات وتتيح إضافة/تصدير/استيراد التذكيرات.
 * ترث FragmentActivity لدعم قفل البصمة (BiometricPrompt).
 */
@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // عرض شاشة البداية الرسمية قبل تحميل الواجهة
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            WhatsAppReminderTheme {
                // بوابة التفعيل: تُطلب حتى إدخال كود صحيح (مربوط بالجهاز، Ed25519)
                var activated by remember {
                    mutableStateOf(
                        com.example.whatsappreminder.util.LicenseManager.isActive(this)
                    )
                }
                if (!activated) {
                    ActivationScreen(
                        deviceCode = com.example.whatsappreminder.util.LicenseManager
                            .deviceIdPretty(this),
                        onValidate = { entered ->
                            val ok = com.example.whatsappreminder.util.LicenseManager
                                .activate(this, entered)
                            if (ok) activated = true
                            ok
                        },
                        onRecoverSeed = { seedHex ->
                            val ok = com.example.whatsappreminder.util.LicenseManager
                                .recoverWithSeed(this, seedHex)
                            if (ok) activated = true
                            ok
                        }
                    )
                    return@WhatsAppReminderTheme
                }

                // بوابة قفل البصمة: تُعرض الواجهة بعد نجاح التحقق (إن كان القفل مفعّلاً)
                var unlocked by remember {
                    mutableStateOf(!SettingsPreferences(this).isBiometricLock())
                }
                if (!unlocked) {
                    LockScreen(onRequestUnlock = { authenticate { unlocked = true } })
                    // محاولة تلقائية عند الدخول
                    LaunchedEffect(Unit) { authenticate { unlocked = true } }
                } else {
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

    /** إطلاق نافذة التحقق بالبصمة/قفل الجهاز */
    private fun authenticate(onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("فتح تذكيرات واتساب")
            .setSubtitle("استخدم بصمتك أو قفل الجهاز")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        runCatching { prompt.authenticate(info) }
    }
}

/** شاشة التفعيل: تُعرض حتى إدخال كود صحيح خاص بالجهاز (Ed25519) */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivationScreen(
    deviceCode: String,
    onValidate: (String) -> Boolean,
    onRecoverSeed: (String) -> Boolean = { false }
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    // استرجاع المالك: يظهر بالضغط المطوّل على الأيقونة
    var showRecover by remember { mutableStateOf(false) }
    var seed by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showRecover = true }
                    ),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text("تفعيل التطبيق", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "أرسل رمز جهازك للمطوّر لتحصل على كود التفعيل الخاص بهذا الجهاز.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            // رمز الجهاز + نسخ
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "رمز جهازك",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            deviceCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(deviceCode))
                        Toast.makeText(context, "تم نسخ رمز الجهاز", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it; error = false },
                label = { Text("كود التفعيل") },
                isError = error,
                supportingText = if (error) {
                    { Text("كود غير صحيح لهذا الجهاز") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!onValidate(code)) {
                        error = true
                        Toast.makeText(context, "كود غير صحيح", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("تفعيل", fontWeight = FontWeight.Bold)
            }

            // استرجاع المالك بالبذرة السرّية (يظهر بالضغط المطوّل على القفل)
            if (showRecover) {
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = seed,
                    onValueChange = { seed = it },
                    label = { Text("بذرة المالك (64 hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (onRecoverSeed(seed)) {
                            Toast.makeText(context, "تم التفعيل الدائم", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "بذرة غير صحيحة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("استرجاع المالك")
                }
            }
        }
    }
}

/** شاشة القفل: تظهر حتى نجاح التحقق */
@Composable
private fun LockScreen(onRequestUnlock: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text("التطبيق مقفل", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestUnlock) { Text("فتح بالبصمة") }
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
    // البحث وفلترة التصنيف
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    // إظهار حوار رمز الدولة / حول التطبيق / الإحصائيات
    var showCountryDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    // حالات المفاتيح (قفل بالبصمة / إخفاء المعاينة)
    var lockEnabled by remember { mutableStateOf(SettingsPreferences(context).isBiometricLock()) }
    var hidePreview by remember { mutableStateOf(SettingsPreferences(context).isHidePreview()) }

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
            // كتابة الملف على خيط خلفي لتفادي أي تجميد
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)
                        ?.use { it.write(json.toByteArray()) }
                }
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
            // قراءة الملف على خيط خلفي ثم الاستيراد
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (text != null) viewModel.importFromJson(text)
            }
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
                        onStats = { showStats = true },
                        onClearData = { showClearConfirm = true },
                        onToggleTheme = {
                            com.example.whatsappreminder.ui.theme.ThemeState.setMode(
                                context,
                                (com.example.whatsappreminder.ui.theme.ThemeState.mode + 1) % 3
                            )
                        },
                        lockEnabled = lockEnabled,
                        onToggleLock = {
                            val newValue = !lockEnabled
                            SettingsPreferences(context).setBiometricLock(newValue)
                            lockEnabled = newValue
                            Toast.makeText(
                                context,
                                if (newValue) "سيُطلب فتح القفل عند الدخول التالي"
                                else "تم إيقاف قفل البصمة",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        hidePreview = hidePreview,
                        onTogglePreview = {
                            val newValue = !hidePreview
                            SettingsPreferences(context).setHidePreview(newValue)
                            hidePreview = newValue
                        },
                        onRestoreAuto = { viewModel.restoreAutoBackup() },
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
                // حقل البحث
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("ابحث في التذكيرات") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                // شرائح فلترة التصنيف
                CategoryFilterRow(selected = categoryFilter, onSelect = { categoryFilter = it })

                // تطبيق البحث والفلترة
                val filtered = uiState.reminders.filter { r ->
                    val q = searchQuery.trim()
                    val matchesQuery = q.isBlank() ||
                        r.contactName.contains(q, ignoreCase = true) ||
                        r.phoneNumber.contains(q) ||
                        r.message.contains(q, ignoreCase = true)
                    val matchesCategory = categoryFilter.isBlank() || r.category == categoryFilter
                    matchesQuery && matchesCategory
                }
                val active = filtered.filter {
                    it.status == ReminderStatus.SCHEDULED ||
                        it.status == ReminderStatus.PENDING_NETWORK ||
                        it.status == ReminderStatus.NOTIFIED
                }
                val done = filtered.filter {
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
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "لا نتائج مطابقة",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                    if (active.isNotEmpty()) {
                        item(key = "h_active") { SectionHeader("قادمة (${active.size})") }
                        items(active, key = { it.id }) { reminder ->
                            SwipeableReminderCard(
                                reminder = reminder,
                                modifier = Modifier.animateItemPlacement(),
                                onClick = { onItemClick(reminder) },
                                onDelete = { pendingDelete = reminder },
                                onSend = {
                                    WhatsAppOpener.openChat(
                                        context, reminder.phoneNumber, reminder.composeMessage()
                                    )
                                    viewModel.markOpened(reminder)
                                }
                            )
                        }
                    }
                    if (done.isNotEmpty()) {
                        item(key = "h_done") { SectionHeader("منتهية (${done.size})") }
                        items(done, key = { it.id }) { reminder ->
                            SwipeableReminderCard(
                                reminder = reminder,
                                modifier = Modifier.animateItemPlacement(),
                                onClick = { onItemClick(reminder) },
                                onDelete = { pendingDelete = reminder },
                                onSend = {
                                    WhatsAppOpener.openChat(
                                        context, reminder.phoneNumber, reminder.composeMessage()
                                    )
                                    viewModel.markOpened(reminder)
                                }
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
                WhatsAppOpener.openChat(context, r.phoneNumber, r.composeMessage())
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

    // حوار الإحصائيات
    if (showStats) {
        StatsDialog(reminders = uiState.reminders, onDismiss = { showStats = false })
    }

    // تأكيد المسح الشامل
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAllData()
                }) {
                    Text("مسح الكل", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("إلغاء") }
            },
            title = { Text("مسح جميع البيانات") },
            text = {
                Text(
                    "سيُحذف كل شيء نهائياً: التذكيرات، الإعدادات، القوالب، " +
                        "والنسخ الاحتياطية الداخلية، مع إلغاء كل المنبّهات. " +
                        "لا يمكن التراجع."
                )
            }
        )
    }
}

/** حوار إحصائيات الاستخدام */
@Composable
private fun StatsDialog(reminders: List<Reminder>, onDismiss: () -> Unit) {
    val now = System.currentTimeMillis()
    val weekAgo = now - 7L * 24 * 60 * 60 * 1000
    val monthAgo = now - 30L * 24 * 60 * 60 * 1000

    // "أُرسلت" = فُتح واتساب لها (OPENED)
    val sent = reminders.filter { it.status == ReminderStatus.OPENED }
    val sentWeek = sent.count { (it.notifiedAt ?: it.scheduledTime) >= weekAgo }
    val sentMonth = sent.count { (it.notifiedAt ?: it.scheduledTime) >= monthAgo }
    val upcoming = reminders.count {
        it.status == ReminderStatus.SCHEDULED && it.scheduledTime > now
    }
    val expired = reminders.count { it.status == ReminderStatus.EXPIRED }
    // أكثر جهة تواصلاً
    val topContact = sent
        .groupBy { it.contactName.ifBlank { it.phoneNumber } }
        .maxByOrNull { it.value.size }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("حسناً") } },
        title = { Text("الإحصائيات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow("رسائل أُرسلت هذا الأسبوع", "$sentWeek")
                StatRow("رسائل أُرسلت هذا الشهر", "$sentMonth")
                StatRow("إجمالي الرسائل المُرسلة", "${sent.size}")
                StatRow("تذكيرات قادمة", "$upcoming")
                StatRow("مواعيد فائتة", "$expired")
                topContact?.let {
                    StatRow("الأكثر تواصلاً", "${it.key} (${it.value.size})")
                }
            }
        }
    )
}

/** صف إحصائية: عنوان + قيمة */
@Composable
private fun StatRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
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
    onStats: () -> Unit,
    onClearData: () -> Unit,
    onToggleTheme: () -> Unit,
    lockEnabled: Boolean,
    onToggleLock: () -> Unit,
    hidePreview: Boolean,
    onTogglePreview: () -> Unit,
    onRestoreAuto: () -> Unit,
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
            text = { Text("الإحصائيات") },
            leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
            onClick = { expanded = false; onStats() }
        )
        DropdownMenuItem(
            text = {
                Text("الوضع: ${com.example.whatsappreminder.ui.theme.ThemeState.label()}")
            },
            leadingIcon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
            onClick = { onToggleTheme() }
        )
        DropdownMenuItem(
            text = { Text(if (lockEnabled) "قفل البصمة: مفعّل" else "قفل البصمة: معطّل") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            onClick = { expanded = false; onToggleLock() }
        )
        DropdownMenuItem(
            text = { Text(if (hidePreview) "إخفاء نص الإشعار: نعم" else "إخفاء نص الإشعار: لا") },
            leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
            onClick = { expanded = false; onTogglePreview() }
        )
        DropdownMenuItem(
            text = { Text("استعادة النسخة التلقائية") },
            leadingIcon = { Icon(Icons.Filled.Restore, contentDescription = null) },
            onClick = { expanded = false; onRestoreAuto() }
        )
        DropdownMenuItem(
            text = { Text("حول التطبيق") },
            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            onClick = { expanded = false; onAbout() }
        )
        DropdownMenuItem(
            text = { Text("مسح جميع البيانات") },
            leadingIcon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = { expanded = false; onClearData() }
        )
    }
}

/** صف شرائح فلترة التصنيف (أفقي قابل للتمرير) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected.isBlank(),
            onClick = { onSelect("") },
            label = { Text("الكل") }
        )
        com.example.whatsappreminder.ui.Categories.ALL
            .filter { it.name.isNotBlank() }
            .forEach { cat ->
                FilterChip(
                    selected = selected == cat.name,
                    onClick = { onSelect(cat.name) },
                    label = { Text(cat.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(cat.color)
                        )
                    }
                )
            }
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

/**
 * بطاقة قابلة للسحب: سحب باتجاه = فتح واتساب للإرسال، والاتجاه الآخر = حذف.
 * لا يُثبَّت السحب بصرياً — يعود للوضع الطبيعي بعد إطلاق الإجراء.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableReminderCard(
    reminder: Reminder,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            when (value) {
                DismissValue.DismissedToEnd -> { onSend(); false }
                DismissValue.DismissedToStart -> { onDelete(); false }
                else -> false
            }
        }
    )
    SwipeToDismiss(
        state = dismissState,
        modifier = modifier,
        background = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "إرسال",
                    tint = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissContent = {
            ReminderCard(reminder = reminder, onClick = onClick, onDelete = onDelete)
        }
    )
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
                    // نقطة لون التصنيف (إن وُجد)
                    com.example.whatsappreminder.ui.Categories.colorFor(reminder.category)?.let { c ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(c)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
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
                    if (reminder.recurrence != com.example.whatsappreminder.domain.model.RecurrenceType.NONE) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = com.example.whatsappreminder.util.Recurrence.label(reminder.recurrence),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
