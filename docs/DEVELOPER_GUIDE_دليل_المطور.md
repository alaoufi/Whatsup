# دليل المطوّر الكامل — تطبيق «تذكيرات واتساب» (WhatsAppReminder)

تطبيق أندرويد يُذكّر المستخدم بإرسال رسائل واتساب في مواعيدها، ثم يفتح المحادثة
ليضغط المستخدم «إرسال» بنفسه. **تذكير فقط — لا إرسال تلقائي.**

---

## 0) القيد الأمني الأساسي (لا يُتجاوز)
- ❌ لا `AccessibilityService`، ولا قراءة شاشة، ولا ضغط أزرار واتساب تلقائياً.
- ✅ فتح واتساب حصراً عبر `Intent` (`whatsapp://send` مع بديل `wa.me`).
- الهدف: حماية حساب المستخدم من الحظر واحترام شروط واتساب.

---

## 1) المتطلبات
| الأداة | الإصدار |
|---|---|
| JDK | 17 أو أحدث (تم البناء بـ JDK 21) |
| Gradle | 8.x (المشروع يعمل بـ 8.14.3) |
| Android Gradle Plugin | 8.2.2 |
| Kotlin | 1.9.22 |
| Android SDK | compileSdk 34 / minSdk 29 / targetSdk 34 |

## 2) البناء السريع
```bash
# نسخة تصحيح (debug)
./gradlew :app:assembleDebug

# نسخة إصدار مصغّرة (release, R8) — الناتج ~5MB
./gradlew :app:assembleRelease
# الناتج: app/build/outputs/apk/release/app-release.apk

# تشغيل اختبارات الوحدة
./gradlew :app:testDebugUnitTest
```
> نسخة release موقّعة بمفتاح debug لتسهيل التثبيت المباشر (Sideload). للنشر على
> Google Play استبدل `signingConfig` بمفتاح إصدار خاصّ بك في `app/build.gradle.kts`.

---

## 3) المعمارية (Clean Architecture + MVVM)
```
domain/            ← منطق الأعمال المستقل (لا يعتمد على أندرويد)
  model/           ← Reminder, ReminderStatus, RecurrenceType
  repository/      ← ReminderRepository (واجهة)
  usecase/         ← Add / Update / Delete / Get / Export / Import / ClearData
data/              ← تنفيذ الطبقة
  local/database/  ← Room: Entity, DAO, AppDatabase (+ migrations)
  local/repository/← ReminderRepositoryImpl
  backup/          ← ReminderBackup (JSON export/import)
di/                ← Hilt: AppModule, WorkerModule
ui/                ← Jetpack Compose (Material 3)
  main/ add/ detail/ batch/ open/ theme/
receiver/          ← BootReceiver, ReminderAlarmReceiver, ReminderActionReceiver
util/              ← AlarmReminderScheduler, WhatsAppOpener, NotificationHelper,
                     PhoneNumberNormalizer, LicenseManager (التفعيل), ...
widget/            ← ReminderWidgetProvider (ودجت الشاشة الرئيسية)
```
تدفّق البيانات: `UI (Compose)` → `ViewModel (StateFlow)` → `UseCase` → `Repository` → `Room DAO (Flow)`.

---

## 4) الجدولة والتنبيه
- **AlarmManager** `setAlarmClock` (دقيق، مستثنى من Doze) — في `AlarmReminderScheduler`.
- عند الموعد يُطلق `ReminderAlarmReceiver` → يعرض إشعاراً عالي الأهمية بأزرار إجراء.
- `BootReceiver` يعيد جدولة كل التذكيرات المعلّقة بعد إعادة تشغيل الجهاز.
- فتح واتساب عبر `WhatsAppOpener` / `OpenWhatsAppActivity` (شفّافة).

---

## 5) نظام التفعيل (Ed25519 — بلا إنترنت) — `util/LicenseManager.kt`
راجع `docs/ACTIVATION_التفعيل.md` للتفاصيل الكاملة. باختصار:
- التطبيق يحمل **المفتاح العامّ فقط** (`PUB_B64`) والبادئة (`PREFIX = UNI3`).
- الكود = بايتان للمدّة (Big-Endian) + توقيع Ed25519 (64 بايت)، مرمّز Base32.
- الرسالة المُوقّعة: `PREFIX|deviceId|days`.
- **لتغيير المفتاح:** بدّل `PUB_B64` في `LicenseManager.kt` ليطابق مفتاح مولّدك،
  و`PREFIX` ليطابق بادئة مولّدك. لا تضع البذرة السرّية في التطبيق أبداً.

---

## 6) قاعدة البيانات
راجع `docs/DATABASE_قاعدة_البيانات.md` — الجداول والأعمدة والترحيلات (v1→v4) بالتفصيل.

---

## 7) الأذونات (AndroidManifest)
`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`,
`READ_CONTACTS`, `SYSTEM_ALERT_WINDOW` (الفتح التلقائي)، `USE_FULL_SCREEN_INTENT`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. تُطلب من المستخدم عند أول تشغيل.

---

## 8) الاختبارات
`app/src/test/java/.../` — 6 مجموعات اختبار (26 اختباراً):
`LicenseManagerTest` (يقبل أكواد مولّدك الحقيقية)، `PhoneNumberNormalizerTest`,
`RecurrenceTest`, `ReminderBackupTest`, `ReminderMappingTest`, `UseCaseTest`.
