package com.example.whatsappreminder.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatsappreminder.ui.theme.WhatsAppReminderTheme
import com.example.whatsappreminder.util.ContactResult
import com.example.whatsappreminder.util.ContactsSearch
import com.example.whatsappreminder.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.TimeZone

/**
 * شاشة إضافة تذكير جديد.
 * لا يوجد حقل اسم — الرقم يُختار عبر البحث في جهات الاتصال.
 */
@AndroidEntryPoint
class AddReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhatsAppReminderTheme {
                AddReminderScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onBack: () -> Unit,
    viewModel: AddReminderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // صلاحية قراءة جهات الاتصال
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (!granted) {
            Toast.makeText(
                context, "امنح صلاحية جهات الاتصال أو اكتب الرقم يدوياً", Toast.LENGTH_LONG
            ).show()
        }
    }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ContactResult>>(emptyList()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(query, hasContactsPermission) {
        results = if (hasContactsPermission && query.isNotBlank()) {
            ContactsSearch.search(context, query)
        } else emptyList()
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "تم حفظ التذكير", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تذكير جديد") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(), // يمنع اختفاء الحقول تحت لوحة المفاتيح
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // البحث في جهات الاتصال (اسم أو رقم)
            OutlinedTextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    if (q.isNotBlank() && !hasContactsPermission) {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
                label = { Text("ابحث عن جهة اتصال (اسم أو رقم)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // نتائج البحث
            if (results.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        results.forEachIndexed { index, contact ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onContactNameChange(contact.name)
                                        viewModel.onPhoneChange(contact.number)
                                        query = ""
                                        results = emptyList()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = contact.name.ifBlank { contact.number },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (contact.name.isNotBlank()) {
                                    Text(
                                        text = contact.number,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (index < results.lastIndex) Divider()
                        }
                    }
                }
            }

            // الرقم
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("الرقم") },
                placeholder = { Text("مثال: +9665xxxxxxxx") },
                supportingText = {
                    Text(
                        state.phoneError
                            ?: if (state.contactName.isNotBlank())
                                "جهة الاتصال: ${state.contactName}"
                            else "اختر من البحث أو اكتب الرقم مع رمز الدولة"
                    )
                },
                isError = state.phoneError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            // نص الرسالة
            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::onMessageChange,
                label = { Text("نص الرسالة") },
                isError = state.messageError != null,
                supportingText = {
                    Text(state.messageError ?: "عدد الأحرف: ${state.messageLength}")
                },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // اختيار التاريخ والوقت
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Event, contentDescription = null)
                Text(
                    text = state.scheduledTime?.let { DateFormatter.formatFull(it) }
                        ?: "  اختر التاريخ والوقت",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            state.timeError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // إعدادات هذا التذكير
            SettingRow(
                label = "صوت التنبيه",
                checked = state.soundEnabled,
                onCheckedChange = viewModel::onSoundEnabledChange
            )
            SettingRow(
                label = "يفتح واتساب مباشرة عند الضغط",
                checked = state.openWhatsAppDirectly,
                onCheckedChange = viewModel::onOpenDirectlyChange
            )

            // زر الحفظ
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("حفظ التذكير", style = MaterialTheme.typography.titleMedium)
            }

            // مساحة سفلية لضمان الوصول لكل الحقول فوق لوحة المفاتيح
            Spacer(Modifier.height(24.dp))
        }
    }

    // منتقي التاريخ (Material 3)
    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = pickedDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = dateState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("التالي") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            }
        ) { DatePicker(state = dateState) }
    }

    // منتقي الوقت عبر قوائم اختيار (ساعة / دقيقة / ص-م)
    if (showTimePicker) {
        TimeDropdownDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hour24, minute ->
                val base = pickedDateMillis ?: System.currentTimeMillis()
                val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = base
                }
                val local = Calendar.getInstance().apply {
                    set(
                        utc.get(Calendar.YEAR),
                        utc.get(Calendar.MONTH),
                        utc.get(Calendar.DAY_OF_MONTH),
                        hour24, minute, 0
                    )
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.onTimeSelected(local.timeInMillis)
                showTimePicker = false
            }
        )
    }
}

/** صف إعداد: نص + مفتاح تبديل */
@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * حوار اختيار الوقت عبر قوائم منسدلة (ساعة 1–12، دقيقة 00–59، ص/م).
 * الاختيار من قائمة وليس كتابة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropdownDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour24: Int, minute: Int) -> Unit
) {
    val now = remember { Calendar.getInstance() }
    val initHour24 = now.get(Calendar.HOUR_OF_DAY)
    var hour12 by remember {
        mutableStateOf((initHour24 % 12).let { if (it == 0) 12 else it })
    }
    var minute by remember { mutableStateOf(now.get(Calendar.MINUTE)) }
    var isPm by remember { mutableStateOf(initHour24 >= 12) }

    val hourOptions = remember { (1..12).map { it.toString() } }
    val minuteOptions = remember { (0..59).map { "%02d".format(it) } }
    val periodOptions = listOf("ص", "م")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // تحويل من نظام 12 إلى 24 ساعة
                val hour24 = when {
                    isPm && hour12 == 12 -> 12
                    isPm -> hour12 + 12
                    !isPm && hour12 == 12 -> 0
                    else -> hour12
                }
                onConfirm(hour24, minute)
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("اختر الوقت") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabeledDropdown(
                    label = "ساعة",
                    options = hourOptions,
                    selectedIndex = hour12 - 1,
                    onSelected = { hour12 = it + 1 },
                    modifier = Modifier.weight(1f)
                )
                LabeledDropdown(
                    label = "دقيقة",
                    options = minuteOptions,
                    selectedIndex = minute,
                    onSelected = { minute = it },
                    modifier = Modifier.weight(1f)
                )
                LabeledDropdown(
                    label = "ص/م",
                    options = periodOptions,
                    selectedIndex = if (isPm) 1 else 0,
                    onSelected = { isPm = it == 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

/** قائمة منسدلة معنونة للاختيار (وليس الكتابة) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
