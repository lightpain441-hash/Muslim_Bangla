package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPrefsState
import com.example.notifications.AdhanScheduler
import com.example.ui.MuslimBanglaApp
import com.example.ui.theme.MuslimBanglaTheme
import com.example.widget.MuslimBanglaWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefsRepo = remember { UserPreferencesRepository(applicationContext) }
            val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPrefsState())

            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            LaunchedEffect(
                prefs.latitude,
                prefs.longitude,
                prefs.prayerCalcMethod,
                prefs.adhanNotificationsEnabled,
                prefs.dailyAyahReminderEnabled
            ) {
                AdhanScheduler.scheduleDailyPrayers(
                    context = applicationContext,
                    latitude = prefs.latitude,
                    longitude = prefs.longitude,
                    method = prefs.prayerCalcMethod,
                    adhanEnabled = prefs.adhanNotificationsEnabled,
                    dailyReminderEnabled = prefs.dailyAyahReminderEnabled
                )
                MuslimBanglaWidgetProvider.refreshAll(applicationContext)
            }

            MuslimBanglaTheme(darkTheme = darkTheme) {
                MuslimBanglaApp()
            }
        }
    }
}
