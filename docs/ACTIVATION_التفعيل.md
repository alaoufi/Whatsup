# نظام التفعيل (Ed25519 — بلا إنترنت)

الملف: `app/src/main/java/com/example/whatsappreminder/util/LicenseManager.kt`
المكتبة: `net.i2p.crypto:eddsa:0.3.0` (تحقّق التوقيع فقط).

## الفكرة
- التطبيق يحمل **المفتاح العامّ فقط**. لا يستطيع توليد أكواد.
- المولّد (تطبيق المالك) يحمل **البذرة السرّية** ويوقّع بها الأكواد.
- كل كود مربوط برقم الجهاز؛ كود جهازٍ لا يعمل على غيره.

## الثوابت (يجب أن تطابق المولّد)
```
PREFIX  = "UNI3"
PUB_B64 = "W5Kc9hRB7lb9xSh/VqdR4T8GT6VaDznEwYQgXZpLZz0="
Base32  = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
```

## رقم الجهاز
`10` بايت عشوائية تُولَّد مرّة وتُخزَّن في تخزين خاصّ (`app_guard`)، تُرمَّز Base32 → `16` حرفاً.
تُعرض للمستخدم بشرطات كل 4 أحرف. **يتغيّر مع كل إعادة تثبيت/مسح بيانات.**

## بنية الكود
```
duration = عدد الأيام (0 = دائم)            // بايتان Big-Endian
sig      = Ed25519_sign("PREFIX|deviceId|duration", البذرة)   // 64 بايت
packet   = [duration>>8, duration&255, ...sig]                 // 66 بايت
code     = Base32(packet)                                       // ~106 حرفاً
```

## التحقّق (داخل التطبيق)
```
pkt = Base32Decode(normalize(code))          // normalize: أحرف كبيرة + حذف ما ليس [A-Z0-9]
if len(pkt) != 66: رفض
duration = (pkt[0]<<8) | pkt[1]
sig      = pkt[2..66)
msg      = "PREFIX|normalize(deviceId)|duration"
صالح     = Ed25519_verify(msg, sig, PUB_B64)
```

## حارس الساعة (للأكواد المؤقّتة)
`effNow = max(now, lastSeen)` يُخزَّن عند كل فتح — إرجاع ساعة الجهاز للوراء لا يمدّد المدّة.

## واجهة LicenseManager
- `deviceId(context)` / `deviceIdPretty(context)`
- `isActive(context): Boolean`
- `activate(context, code): Boolean` — يقبل كود الجهاز، أو كوداً عالمياً (device = `UNIVERSAL`).
- `recoverWithSeed(context, seedHex)` — استرجاع المالك ببذرته (ضغطة مطوّلة على القفل).
- `verifyCode(code, deviceId): Int?` — تحقّق نقيّ (للاختبار).

## تغيير المفتاح لمولّد آخر
1. ولّد زوج مفاتيح في مولّدك واحصل على المفتاح العامّ (Base64).
2. ضع القيمة في `PUB_B64`، واضبط `PREFIX` على بادئة مولّدك.
3. أعد البناء. **لا تضع البذرة السرّية في التطبيق إطلاقاً.**

## الكود العالمي (اختياري)
التطبيق يقبل أيضاً كوداً مُوقّعاً على الجهاز الثابت `UNIVERSAL` (device=UNIVERSAL, days=0)،
فيعمل على أي جهاز — مفيد للاختبار أو للتوزيع المبسّط.
