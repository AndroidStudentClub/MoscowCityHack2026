package com.mikhailskiy.finni.data

import android.content.Context
import androidx.core.content.edit

data class AppSettings(
    val soundEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
)

class SettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences("finni_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        soundEnabled = preferences.getBoolean("sound_enabled", true),
        reducedMotion = preferences.getBoolean("reduced_motion", false),
    )

    fun setSoundEnabled(enabled: Boolean) {
        preferences.edit { putBoolean("sound_enabled", enabled) }
    }

    fun setReducedMotion(enabled: Boolean) {
        preferences.edit { putBoolean("reduced_motion", enabled) }
    }
}
