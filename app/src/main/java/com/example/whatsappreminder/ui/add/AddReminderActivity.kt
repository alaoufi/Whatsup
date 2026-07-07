package com.example.whatsappreminder.ui.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Contacts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatsappreminder.ui.theme.WhatsAppReminderTheme
import com.example.whatsappreminder.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * شاشة إضافة تذكير جديد.
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

    // مُطلق اختيار جهة اتصال من دفتر الهاتف.
    // نستخدم ACTION_PICK فيمنح النظام صلاحية قراءة مؤقتة للسجل المختار
    // دون الحاجة لطلب صلاحية READ_CONTACTS دائمة.
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            val contact = queryPickedContact(context, uri)
            if (contact != null) {
                viewModel.onContactNameChange(contact.first)
                viewModel.onPhoneChange(contact.second)
            }
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
            // اسم جهة الاتصال
            OutlinedTextField(
                value = state.contactName,
                onValueChange = viewModel::onContactNameChange,
                label = { Text("اسم جهة الاتصال") },
                singleLine = true,
                isError = state.contactNameError != null,
                supportingText = { state.contactNameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            // رقم الهاتف مع تلميح الصيغة الدولية
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("رقم الهاتف") },
                placeholder = { Text("مثال: +9665xxxxxxxx") },
                supportingText = {
                    Text(state.phoneError ?: "أدخل الرقم مع رمز الدولة")
                },
                isError = state.phoneError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                // زر اختيار جهة اتصال من دفتر الهاتف
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_PICK).apply {
                            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                        }
                        runCatching { contactPickerLauncher.launch(intent) }
                    }) {
                        Icon(Icons.Filled.Contacts, contentDescription = "اختيار من جهات الاتصال")
                    }
                },
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

            // اختيار التاريخ والوقت
            OutlinedButton(
                onClick = { showDateTimePicker(context, viewModel) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Event, contentDescription = null)
                Spacer(Modifier.height(0.dp))
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
                    .height(52.dp),
                enabled = true
            ) {
                Text("حفظ التذكير", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * قراءة اسم ورقم جهة الاتصال المختارة من الـ Uri الناتج عن ACTION_PICK.
 * @return زوج (الاسم، الرقم) أو null إذا تعذّرت القراءة.
 */
private fun queryPickedContact(context: Context, uri: android.net.Uri): Pair<String, String>? {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    return runCatching {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0) ?: ""
                // إزالة المسافات من الرقم مع الإبقاء على رمز الدولة (+)
                val number = (cursor.getString(1) ?: "").replace(" ", "").replace("-", "")
                Pair(name, number)
            } else null
        }
    }.getOrNull()
}

/**
 * عرض منتقي التاريخ ثم الوقت بشكل متسلسل، وتمرير الناتج للـ ViewModel.
 */
private fun showDateTimePicker(
    context: android.content.Context,
    viewModel: AddReminderViewModel
) {
    val now = Calendar.getInstance()

    DatePickerDialog(
        context,
        { _, year, month, day ->
            // بعد اختيار التاريخ، اعرض منتقي الوقت
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
                    viewModel.onTimeSelected(selected.timeInMillis)
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
        // منع اختيار تاريخ في الماضي
        datePicker.minDate = now.timeInMillis
    }.show()
}
