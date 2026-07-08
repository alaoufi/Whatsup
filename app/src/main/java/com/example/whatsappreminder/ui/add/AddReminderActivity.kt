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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    // حالة صلاحية قراءة جهات الاتصال
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
                context,
                "امنح صلاحية جهات الاتصال أو اكتب الرقم يدوياً",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // حقل البحث ونتائجه
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ContactResult>>(emptyList()) }

    // حالة عرض منتقيات التاريخ والوقت
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }

    // تحديث النتائج عند تغيّر نص البحث أو منح الصلاحية
    LaunchedEffect(query, hasContactsPermission) {
        results = if (hasContactsPermission && query.isNotBlank()) {
            ContactsSearch.search(context, query)
        } else {
            emptyList()
        }
    }

    // عند الحفظ الناجح، أغلق الشاشة
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // حقل البحث في جهات الاتصال (اسم أو رقم)
            OutlinedTextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    // طلب الصلاحية عند أول استخدام إن لم تُمنح بعد
                    if (q.isNotBlank() && !hasContactsPermission) {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
                label = { Text("ابحث عن جهة اتصال (اسم أو رقم)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // نتائج البحث — اضغط لاختيار الرقم
            if (results.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        results.forEachIndexed { index, contact ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // ملء الرقم والاسم (الاسم يُحفظ للعرض فقط)
                                        viewModel.onContactNameChange(contact.name)
                                        viewModel.onPhoneChange(contact.number)
                                        query = ""
                                        results = emptyList()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = contact.name.ifBlank { contact.number },
                                    style = MaterialTheme.typography.titleMedium
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

            // رقم الهاتف (يُملأ من البحث أو يُكتب يدوياً)
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
                            else "اختر من البحث بالأعلى أو اكتب الرقم مع رمز الدولة"
                    )
                },
                isError = state.phoneError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            // نص الرسالة (متعدد الأسطر + عداد أحرف)
            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::onMessageChange,
                label = { Text("نص الرسالة") },
                isError = state.messageError != null,
                supportingText = {
                    // عرض الخطأ إن وُجد، وإلا عرض عدّاد الأحرف
                    Text(state.messageError ?: "عدد الأحرف: ${state.messageLength}")
                },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            // ملاحظات اختيارية
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("ملاحظات (اختياري)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // اختيار التاريخ والوقت (يبدأ بالتاريخ ثم إدخال الوقت بالأرقام)
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

            Spacer(Modifier.height(8.dp))

            // زر الحفظ
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("حفظ التذكير", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // --- منتقي التاريخ (Material 3، يدعم الإدخال بالأرقام) ---
    if (showDatePicker) {
        val today = System.currentTimeMillis()
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = pickedDateMillis ?: today
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = dateState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true // ننتقل لإدخال الوقت
                }) { Text("التالي") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    // --- منتقي الوقت بالأرقام (TimeInput = حقول HH:MM واضحة) ---
    if (showTimePicker) {
        val nowCal = Calendar.getInstance()
        val timeState = rememberTimePickerState(
            initialHour = nowCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = nowCal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // دمج التاريخ المختار مع الوقت (بمعالجة صحيحة للمنطقة الزمنية)
                    val baseMillis = pickedDateMillis ?: System.currentTimeMillis()
                    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        .apply { timeInMillis = baseMillis }
                    val local = Calendar.getInstance().apply {
                        set(
                            utc.get(Calendar.YEAR),
                            utc.get(Calendar.MONTH),
                            utc.get(Calendar.DAY_OF_MONTH),
                            timeState.hour,
                            timeState.minute,
                            0
                        )
                        set(Calendar.MILLISECOND, 0)
                    }
                    viewModel.onTimeSelected(local.timeInMillis)
                    showTimePicker = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("إلغاء") }
            },
            title = { Text("أدخل الوقت") },
            text = { TimeInput(state = timeState) }
        )
    }
}

