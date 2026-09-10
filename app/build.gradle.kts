// ملف الـ Gradle الخاص بوحدة التطبيق (app module)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// رقم البناء يأتي من GitHub Actions (github.run_number)؛ محلياً = 1
val appBuildNumber = (System.getenv("APP_BUILD_NUMBER") ?: "1").toInt()

// مفتاح التوقيع الثابت (ضروري لآليّة التحديث الذاتيّ: يُثبَّت التحديث فوق القديم)
val releaseKeystore = rootProject.file("keystore/release.jks")

android {
    namespace = "com.example.whatsappreminder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.whatsappreminder"
        minSdk = 29          // الحد الأدنى: Android 10
        targetSdk = 34       // الهدف: Android 14
        // versionCode = رقم البناء، versionName = 1.0.<رقم البناء> — يُضبطان تلقائياً في CI
        versionCode = appBuildNumber
        versionName = "1.0.$appBuildNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // مفتاح إصدار ثابت مُضمَّن — نفس التوقيع في كل بناء (محلي أو CI)
        // فتُثبَّت التحديثات فوق النسخة القديمة دون فقدان بيانات المستخدم.
        create("release") {
            if (releaseKeystore.exists()) {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "whatsapp2026"
                keyAlias = System.getenv("KEY_ALIAS") ?: "whatsappreminder"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "whatsapp2026"
            }
        }
    }

    buildTypes {
        release {
            // تفعيل تصغير الكود (R8) وإزالة الموارد غير المستخدمة لتقليل حجم APK
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // استخدم مفتاح الإصدار الثابت إن وُجد، وإلا مفتاح debug (للبناء المحلي بلا keystore)
            signingConfig = if (releaseKeystore.exists())
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // استخدام Java 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // مطلوب لقراءة رقم النسخة الحالي (VERSION_CODE/NAME) في نظام التحديث
        buildConfig = true
    }

    composeOptions {
        // نسخة مترجم Compose المتوافقة مع Kotlin 1.9.22
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // مكتبات Android الأساسية
    implementation("androidx.core:core-ktx:1.12.0")
    // شاشة البداية الرسمية (Splash Screen API)
    implementation("androidx.core:core-splashscreen:1.0.1")
    // قفل التطبيق بالبصمة
    implementation("androidx.biometric:biometric:1.1.0")
    // تحقق توقيع Ed25519 لنظام التفعيل (بلا إنترنت)
    implementation("net.i2p.crypto:eddsa:0.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose (عبر BOM لضمان توافق الإصدارات)
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // أيقونات Material الموسعة
    implementation("androidx.compose.material:material-icons-extended")
    // مكوّنات Material (مطلوبة لسمة XML: Theme.Material3.DayNight)
    implementation("com.google.android.material:material:1.11.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Hilt لحقن التبعيات
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Room قاعدة البيانات المحلية
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager لجدولة المهام + امتداد Hilt
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // أدوات التطوير والمعاينة
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // اختبارات الوحدة
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    // نسخة حقيقية من org.json لاختبار منطق النسخ الاحتياطي على الـ JVM
    testImplementation("org.json:json:20231013")
}
