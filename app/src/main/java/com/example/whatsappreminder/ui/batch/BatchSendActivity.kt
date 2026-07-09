package com.example.whatsappreminder.ui.batch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.ui.theme.WhatsAppReminderTheme
import com.example.whatsappreminder.util.WhatsAppOpener
import com.example.whatsappreminder.util.composeMessage
import dagger.hilt.android.AndroidEntryPoint

/**
 * شاشة الإرسال الجماعي: تعرض مستلمي نفس الموعد ليرسل المستخدم لكل واحد
 * على حدة — يضغط "فتح واتساب"، يرسل، يرجع، ويتابع مع التالي (✓ لمن تم).
 */
@AndroidEntryPoint
class BatchSendActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IDS = "extra_batch_ids"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ids = intent.getLongArrayExtra(EXTRA_IDS)?.toList() ?: emptyList()
        setContent {
            WhatsAppReminderTheme {
                BatchSendScreen(ids = ids, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchSendScreen(
    ids: List<Long>,
    onBack: () -> Unit,
    viewModel: BatchSendViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()

    LaunchedEffect(ids) { viewModel.setIds(ids) }

    val sentCount = reminders.count { it.status == ReminderStatus.OPENED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إرسال جماعي ($sentCount/${reminders.size})") },
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
        ) {
            Text(
                text = "أرسل لكل مستلم ثم ارجع لهذه الشاشة للمتابعة مع التالي",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    BatchRow(
                        reminder = reminder,
                        onSend = {
                            // فتح واتساب لهذا المستلم ووسمه "تم"
                            WhatsAppOpener.openChat(
                                context, reminder.phoneNumber, reminder.composeMessage()
                            )
                            viewModel.markOpened(reminder.id)
                        }
                    )
                }
            }
        }
    }
}

/** صف مستلم واحد في قائمة الإرسال الجماعي */
@Composable
private fun BatchRow(reminder: Reminder, onSend: () -> Unit) {
    val sent = reminder.status == ReminderStatus.OPENED
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (sent)
                MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.contactName.ifBlank { reminder.phoneNumber },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            if (sent) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "تم الإرسال",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Button(onClick = onSend, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("فتح واتساب")
                }
            }
        }
    }
}
