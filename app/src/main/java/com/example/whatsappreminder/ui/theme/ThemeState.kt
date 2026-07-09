package com.example.whatsappreminder.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.whatsappreminder.util.SettingsPreferences

/**
 * حالة وضع السمة على مستوى التطبيق (0=تلقائي، 1=فاتح، 2=داكن).
 * تُقرأ من التفضيلات عند أول استخدام، وتغييرها يعيد تركيب الواجهة فوراً.
 */
object ThemeState {
    var mode by mutableStateOf(0)
        private set

    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            mode = SettingsPreferences(context).getThemeMode()
            initialized = true
        }
    }

    fun setMode(context: Context, newMode: Int) {
        mode = newMode.coerceIn(0, 2)
        SettingsPreferences(context).setThemeMode(mode)
    }

    fun label(): String = when (mode) {
        1 -> "فاتح"
        2 -> "داكن"
        else -> "تلقائي"
    }
}
