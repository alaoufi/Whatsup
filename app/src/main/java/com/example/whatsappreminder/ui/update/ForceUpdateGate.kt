package com.example.whatsappreminder.ui.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.whatsappreminder.util.UpdateInfo
import com.example.whatsappreminder.util.UpdateService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

/**
 * بوّابة تحديث إلزاميّة تغلّف كامل التطبيق (يجب أن تكون الأعلى).
 *
 * - عند الإقلاع وعند عودة التطبيق للواجهة تفحص التحديث **إن وُجد إنترنت**.
 * - إن توفّرت نسخة أحدث ⇒ تعرض شاشة حاجبة لا يمكن تجاوزها حتى التحديث.
 * - إن تعذّر الفحص (لا إنترنت) ⇒ لا تحجب، ويعمل التطبيق عاديًّا.
 * - لا تمسّ بيانات المستخدم — التحديث يُثبَّت فوق القديم (مفتاح توقيع ثابت).
 */
@Composable
fun ForceUpdateGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    val scope = rememberCoroutineScope()

    // افحص عند الإقلاع وعند كل عودة للواجهة (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && update == null && isOnline(context)) {
                scope.launch {
                    runCatching { UpdateService.checkForUpdate() }
                        .getOrNull()
                        ?.let { update = it }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pending = update
    if (pending != null) {
        UpdateRequiredScreen(pending)
    } else {
        content()
    }
}

@Composable
private fun UpdateRequiredScreen(info: UpdateInfo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    // منع تجاوز الشاشة بزرّ الرجوع
    BackHandler(enabled = true) { /* محجوب عمداً */ }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "تحديث مطلوب",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "تتوفّر نسخة أحدث (${info.versionName}). حدّث الآن للمتابعة — لن تفقد بياناتك.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            if (downloading) {
                if (progress >= 0) {
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("جارٍ التنزيل… $progress%")
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("جارٍ التنزيل…")
                }
            } else {
                Button(
                    onClick = {
                        error = null
                        // تأكّد من صلاحية تثبيت المصادر غير المعروفة أولاً
                        if (!UpdateService.canInstallPackages(context)) {
                            UpdateService.openInstallSettings(context)
                            return@Button
                        }
                        downloading = true
                        scope.launch {
                            val ok = UpdateService.downloadAndInstall(
                                context, info.url
                            ) { p -> progress = p }
                            downloading = false
                            if (!ok) error = "تعذّر التنزيل. جرّب التنزيل عبر المتصفّح."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("تحديث الآن", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { UpdateService.openInBrowser(context, info.url) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تنزيل عبر المتصفّح")
                }
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** هل يوجد اتصال إنترنت فعّال؟ */
private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
