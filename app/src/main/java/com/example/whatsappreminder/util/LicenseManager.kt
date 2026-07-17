package com.example.whatsappreminder.util

import android.content.Context
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * نظام تفعيل بلا إنترنت مربوط بالجهاز (Ed25519) — مطابق تماماً لآلية keygen.mjs:
 * - رقم جهاز ثابت (16 حرف Base32).
 * - الكود = بايتان للمدّة (Big-Endian) + توقيع Ed25519 (64 بايت)، مرمّز Base32.
 * - الرسالة الموقّعة: PREFIX|رقم_الجهاز|المدّة  (المدّة بالأيام، 0 = دائم).
 * - التطبيق يحمل المفتاح العامّ فقط ويتحقّق محلياً.
 * - استرجاع المالك: إدخال البذرة السرّية (64 hex) يفعّل دائماً على أي جهاز.
 */
object LicenseManager {

    // ⚠️ المفتاح العامّ الموحّد (Base64) لمولّد المالك — آمن للنشر، لا يولّد أكواداً.
    // مطابق للمفتاح المضمّن في مولّد المالك (نظام UNI3).
    private const val PUB_B64 = "W5Kc9hRB7lb9xSh/VqdR4T8GT6VaDznEwYQgXZpLZz0="

    // نفس بادئة المولّد الموحّد (نظام UNI3)
    private const val PREFIX = "UNI3"

    // «جهاز» ثابت للكود العالمي: كود واحد يعمل على أي جهاز
    // (يُولَّد بـ: node keygen.mjs code --seed <SEED> --device UNIVERSAL --days 0)
    private const val UNIV_DEVICE = "UNIVERSAL"

    private const val B32 = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val PREFS = "app_guard"
    private const val K_DEV = "guard_device"
    private const val K_LIC_DUR = "guard_dur"
    private const val K_LIC_AT = "guard_at"
    private const val K_LIC_SEEN = "guard_seen"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- ترميز/فكّ Base32 (مطابق لـ b32e/b32d في المولّد) ----------

    private fun b32Encode(bytes: ByteArray): String {
        var bits = 0; var v = 0; val o = StringBuilder()
        for (b in bytes) {
            v = (v shl 8) or (b.toInt() and 0xFF); bits += 8
            while (bits >= 5) { o.append(B32[(v ushr (bits - 5)) and 31]); bits -= 5 }
            v = v and ((1 shl bits) - 1)
        }
        if (bits > 0) o.append(B32[(v shl (5 - bits)) and 31])
        return o.toString()
    }

    private fun b32Decode(s: String): ByteArray {
        var bits = 0; var v = 0; val o = ArrayList<Byte>()
        for (c in s) {
            val k = B32.indexOf(c); if (k < 0) continue
            v = (v shl 5) or k; bits += 5
            if (bits >= 8) { o.add(((v ushr (bits - 8)) and 0xFF).toByte()); bits -= 8 }
            v = v and ((1 shl bits) - 1)
        }
        return o.toByteArray()
    }

    private fun norm(s: String): String =
        s.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    // ---------- Ed25519 ----------

    private val curve = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)

    private fun ed25519Verify(msg: ByteArray, sig: ByteArray, pub: ByteArray): Boolean = try {
        val key = EdDSAPublicKey(EdDSAPublicKeySpec(pub, curve))
        val engine = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
        engine.initVerify(key)
        engine.update(msg)
        engine.verify(sig)
    } catch (e: Exception) {
        false
    }

    /**
     * التحقّق النقي من كود لجهاز معيّن (بلا سياق) بالمفتاح العامّ المضمّن.
     * @return عدد الأيام (0=دائم) إن كان صالحاً، أو null.
     */
    fun verifyCode(code: String, deviceId: String): Int? =
        verifyCodeWithPub(code, deviceId, PUB_B64)

    /**
     * نفس منطق [verifyCode] لكن بمفتاح عامّ مُمرَّر — للاختبار (إثبات تطابق
     * الخوارزمية مع المولّد دون الحاجة للبذرة السرّية).
     */
    internal fun verifyCodeWithPub(code: String, deviceId: String, pubB64: String): Int? {
        val pkt = b32Decode(norm(code))
        if (pkt.size != 66) return null
        val dur = ((pkt[0].toInt() and 0xFF) shl 8) or (pkt[1].toInt() and 0xFF)
        val sig = pkt.copyOfRange(2, 66)
        val msg = "$PREFIX|${norm(deviceId)}|$dur".toByteArray(Charsets.UTF_8)
        val pub = java.util.Base64.getDecoder().decode(pubB64)
        return if (ed25519Verify(msg, sig, pub)) dur else null
    }

    /** المفتاح العامّ المضمّن (للاختبار/العرض) */
    internal fun publicKeyB64(): String = PUB_B64

    // ---------- رقم الجهاز ----------

    /** رقم الجهاز الخام (16 حرف Base32)، يُنشأ مرة ويُخزَّن */
    fun deviceId(context: Context): String {
        val p = prefs(context)
        var r = p.getString(K_DEV, null)
        if (r == null || r.length < 16) {
            val b = ByteArray(10).also { SecureRandom().nextBytes(it) }
            r = b32Encode(b)
            p.edit().putString(K_DEV, r).apply()
        }
        return r
    }

    /** رقم الجهاز بصيغة مقروءة (شرطة كل 4 أحرف) */
    fun deviceIdPretty(context: Context): String =
        deviceId(context).chunked(4).joinToString("-")

    // ---------- الحالة والتفعيل ----------

    private fun disabled(): Boolean = PUB_B64.startsWith("REPLACE_")

    /** هل التطبيق مفعّل حالياً (وغير منتهٍ)؟ */
    fun isActive(context: Context): Boolean {
        if (disabled()) return true
        val p = prefs(context)
        if (!p.contains(K_LIC_DUR)) return false
        val dur = p.getInt(K_LIC_DUR, -1)
        if (dur < 0) return false
        val at = p.getLong(K_LIC_AT, 0L)
        val now = System.currentTimeMillis()
        // حارس الساعة: لا نسمح بإرجاع الوقت للوراء
        val eff = maxOf(now, p.getLong(K_LIC_SEEN, 0L))
        p.edit().putLong(K_LIC_SEEN, eff).apply()
        if (dur == 0) return true               // دائم
        return eff < at + dur.toLong() * 86_400_000L
    }

    /**
     * محاولة تفعيل بكود؛ يعيد true عند النجاح ويخزّن السجل.
     * يقبل نوعين من الأكواد:
     *  1) كود خاصّ بهذا الجهاز (مربوط برقم الجهاز).
     *  2) كود عالمي يعمل على أي جهاز (device = UNIVERSAL).
     */
    fun activate(context: Context, code: String): Boolean {
        val dur = verifyCode(code, deviceId(context))
            ?: verifyCode(code, UNIV_DEVICE)
            ?: return false
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putInt(K_LIC_DUR, dur)
            .putLong(K_LIC_AT, now)
            .putLong(K_LIC_SEEN, now)
            .apply()
        return true
    }

    /**
     * استرجاع المالك: إدخال البذرة السرّية (64 hex).
     * إن طابق مفتاحُها العامُّ مفتاحَ التطبيق ⇒ تفعيل دائم على هذا الجهاز.
     */
    fun recoverWithSeed(context: Context, seedHex: String): Boolean {
        val h = seedHex.trim().lowercase().filter { it in '0'..'9' || it in 'a'..'f' }
        if (h.length != 64) return false
        return try {
            val seed = ByteArray(32) { h.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
            val priv = EdDSAPrivateKey(EdDSAPrivateKeySpec(seed, curve))
            val derivedPub = java.util.Base64.getEncoder().encodeToString(priv.abyte)
            if (derivedPub == PUB_B64) {
                val now = System.currentTimeMillis()
                prefs(context).edit()
                    .putInt(K_LIC_DUR, 0)
                    .putLong(K_LIC_AT, now)
                    .putLong(K_LIC_SEEN, now)
                    .apply()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
