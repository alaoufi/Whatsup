package com.example.whatsappreminder.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.whatsappreminder.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * معلومات التحديث المقروءة من version.json.
 * @property versionName اسم النسخة (مثل 1.0.75)
 * @property build رقم البناء (عدد صحيح)
 * @property url رابط تنزيل APK المباشر
 */
data class UpdateInfo(
    val versionName: String,
    val build: Int,
    val url: String
)

/**
 * خدمة التحديث الذاتيّ (بلا Google Play) — مكافئ UpdateService في الأصل Flutter.
 *
 * تقرأ version.json من إصدار GitHub الثابت "latest" (مع مصادر احتياطية)، تقارن
 * النسخة، وتنزّل وتثبّت APK عبر مثبّت النظام. لا تمسّ بيانات المستخدم — التحديث
 * يُثبَّت فوق القديم لأن مفتاح التوقيع ثابت.
 */
object UpdateService {

    // ===== المتغيّرات الخاصة بهذا المشروع =====
    private const val OWNER = "alaoufi"
    private const val REPO = "Whatsup"
    private const val APP_NAME = "WhatsAppReminder"   // اسم ملف APK في الإصدار

    /** المصادر بالترتيب: إصدار latest ثم raw.githubusercontent ثم jsDelivr */
    private val VERSION_JSON_SOURCES = listOf(
        "https://github.com/$OWNER/$REPO/releases/download/latest/version.json",
        "https://raw.githubusercontent.com/$OWNER/$REPO/main/version.json",
        "https://cdn.jsdelivr.net/gh/$OWNER/$REPO@main/version.json"
    )

    private const val TIMEOUT_MS = 8000

    /**
     * يفحص إن توفّر تحديث أحدث من النسخة الحالية.
     * @return [UpdateInfo] إن توفّرت نسخة أحدث، أو null (لا تحديث / تعذّر الاتصال).
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val info = fetchVersionInfo() ?: return@withContext null
        // مقارنة رقم البناء (APK واحد بلا split-per-abi ⇒ رقم البناء موثوق)،
        // مع مقارنة اسم النسخة دلاليًّا كطبقة أمان إضافية.
        val newer = info.build > BuildConfig.VERSION_CODE ||
            isVersionNewer(info.versionName, BuildConfig.VERSION_NAME)
        if (newer) info else null
    }

    /** يجرّب كل المصادر بالتتابع حتى ينجح أحدها */
    private fun fetchVersionInfo(): UpdateInfo? {
        for (src in VERSION_JSON_SOURCES) {
            val text = runCatching { httpGet(src) }.getOrNull()
            if (text != null) {
                val parsed = runCatching {
                    val o = JSONObject(text)
                    UpdateInfo(
                        versionName = o.optString("version", "0"),
                        build = o.optInt("build", 0),
                        url = o.optString("url", "")
                    )
                }.getOrNull()
                if (parsed != null && parsed.url.isNotBlank()) return parsed
            }
        }
        return null
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * مقارنة دلاليّة لاسمَي النسخة: "1.0.75" أحدث من "1.0.72".
     * تتجاهل ما ليس أرقاماً وتقارن جزءاً جزءاً.
     */
    fun isVersionNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val c = current.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val n = maxOf(r.size, c.size)
        for (i in 0 until n) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    /**
     * ينزّل APK إلى مجلد مؤقّت مع تقدّم، ثم يشغّل مثبّت النظام.
     * @param onProgress نسبة مئوية 0..100 (‑1 إذا الحجم غير معروف).
     * @return true إذا بدأ التثبيت، false عند الفشل.
     */
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val apk = runCatching { downloadApk(context, url, onProgress) }.getOrNull()
            ?: return@withContext false
        withContext(Dispatchers.Main) { installApk(context, apk) }
    }

    private fun downloadApk(context: Context, url: String, onProgress: (Int) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        // نظّف أي تنزيلات سابقة لتوفير المساحة
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "$APP_NAME-update.apk")

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = 30000
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val total = conn.contentLength
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(8 * 1024)
                    var read: Int
                    var downloaded = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        downloaded += read
                        if (total > 0) {
                            onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                        } else {
                            onProgress(-1)
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return out
    }

    /** يشغّل مثبّت النظام على ملف APK عبر FileProvider */
    private fun installApk(context: Context, apk: File): Boolean = runCatching {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /** هل يُسمح للتطبيق بتثبيت تطبيقات من مصادر غير معروفة؟ */
    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.packageManager.canRequestPackageInstalls()
        else true

    /** يفتح إعداد «السماح بتثبيت تطبيقات غير معروفة» لهذا التطبيق */
    fun openInstallSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /** فتح رابط التنزيل في المتصفّح (خيار احتياطي) */
    fun openInBrowser(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
