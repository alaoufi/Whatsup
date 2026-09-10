# نظام التحديث الذاتيّ (بلا Google Play)

آليّة تحديث تلقائيّة: عند كل دفع إلى `main` تبني GitHub Actions نسخة APK جديدة وتنشرها
في إصدار ثابت `latest`، فيكتشفها التطبيق ويعرض شاشة «تحديث مطلوب» تُنزّل وتثبّت التحديث
فوق النسخة القديمة **دون فقدان بيانات المستخدم**.

> هذه مكافئة أصليّة (Kotlin/Compose) لبرومبت آليّة Flutter — روعيت متغيّرات هذا المشروع.

## المتغيّرات (خاصة بهذا المشروع)
| المتغيّر | القيمة |
|---|---|
| `<owner>` | `alaoufi` |
| `<repo>` | `Whatsup` |
| `<AppName>` (اسم ملف APK) | `WhatsAppReminder` |
| `<applicationId>` | `com.example.whatsappreminder` |

## الأجزاء الأربعة
1. **البناء (GitHub Actions)** — `.github/workflows/build-apk.yml`
   - عند الدفع إلى `main`: `./gradlew :app:assembleRelease` مع `APP_BUILD_NUMBER = github.run_number`.
   - `versionName = 1.0.<run_number>`، `versionCode = <run_number>` (يُضبطان في `app/build.gradle.kts`).
   - ينشر إلى tag `latest`: ملف `WhatsAppReminder.apk` (رابط دائم) + `version.json`:
     ```json
     { "version": "1.0.<run_number>", "build": <run_number>,
       "url": "https://github.com/alaoufi/Whatsup/releases/download/latest/WhatsAppReminder.apk" }
     ```
2. **UpdateService** — `util/UpdateService.kt`
   - يقرأ `version.json` من إصدار `latest` (ومصادر احتياطية: raw.githubusercontent ثم jsDelivr) بمهلة.
   - يقارن رقم البناء + اسم النسخة دلاليًّا، ويعيد `UpdateInfo` أو `null`.
   - `downloadAndInstall`: ينزّل APK إلى `cacheDir/updates` مع تقدّم، ثم يفتح مثبّت النظام عبر `FileProvider`.
   - يتعامل مع صلاحية «تثبيت تطبيقات غير معروفة» (`canInstallPackages` / `openInstallSettings`).
3. **ForceUpdateGate** — `ui/update/ForceUpdateGate.kt`
   - يغلّف كامل التطبيق (الأعلى، فوق بوّابة التفعيل).
   - يفحص عند الإقلاع وعند عودة التطبيق للواجهة **إن وُجد إنترنت**؛ يعرض شاشة حاجبة عند توفّر نسخة أحدث.
   - زرّ «تحديث الآن» (تنزيل+تثبيت مع شريط تقدّم) + «تنزيل عبر المتصفّح». لا يحجب عند غياب الإنترنت.
4. **أندرويد** — المانيفست + FileProvider
   - أذونات: `INTERNET` (موجود) + `REQUEST_INSTALL_PACKAGES`.
   - `FileProvider` بسلطة `${applicationId}.fileprovider` ومساراته في `res/xml/file_paths.xml`.

## مفتاح التوقيع الثابت (الأهمّ)
- الإصدار موقّع بمفتاح ثابت مُضمَّن: `keystore/release.jks` (إعداده في `app/build.gradle.kts`).
- بدون ثبات المفتاح **لن تُثبَّت التحديثات فوق القديم** = فقدان بيانات. الآن كل بناء (محلّي أو CI) يوقّع بنفس المفتاح.
- ⚠️ **مرّة واحدة فقط:** النسخة الحالية على جهازك موقّعة بمفتاح debut قديم. لتفعيل التحديث التلقائي:
  **احذف التطبيق القديم ثم ثبّت النسخة الجديدة الموقّعة بالمفتاح الثابت.** بعدها كل التحديثات تُثبَّت تلقائياً فوق بعضها بلا حذف.

## بيانات المستخدم عند التحديث
- قاعدة البيانات تُرقّى تدريجيًّا عبر Room Migrations (راجع `DATABASE_قاعدة_البيانات.md`) بلا حذف جداول.
- التفعيل والإعدادات محفوظة في `SharedPreferences` وتبقى بعد التحديث (بشرط ثبات مفتاح التوقيع).

## ملاحظة أمنية حول المفتاح
`keystore/release.jks` وكلمته مضمّنان في المستودع لتبسيط البناء التلقائي (توزيع APK مباشر، لا Google Play).
إن أردت إخفاءهما: انقل الملف إلى أسرار GitHub (Base64) واقرأه في الـ workflow، واستبدل القيم في
`signingConfigs` بمتغيّرات بيئة (`KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`) — وهي مدعومة أصلاً في الكود.

## كيفية إطلاق تحديث جديد
ادفع أي تغيير إلى فرع `main` ← تبني Actions APK جديداً برقم أعلى ← أي جهاز يفتح التطبيق (مع إنترنت)
يرى «تحديث مطلوب» ويُحدّث. لا حاجة لأي خطوة يدوية.
