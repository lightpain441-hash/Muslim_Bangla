package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "muslim_bangla_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage { BANGLA, ARABIC, ENGLISH }
enum class QuranLayoutMode { STACKED, SIDE_BY_SIDE }
enum class PrayerCalcMethod(val titleBangla: String, val fajrAngle: Double, val ishaAngle: Double, val ishaMinutesAfterMaghrib: Int? = null) {
    MWL("মুসলিম ওয়ার্ল্ড লিগ (MWL)", 18.0, 17.0),
    ISLAMIC_FOUNDATION_BD("ইসলামিক ফাউন্ডেশন বাংলাদেশ", 18.0, 18.0),
    KARACHI("ইউনিভার্সিটি অব ইসলামিক সায়েন্সেস, করাচি", 18.0, 18.0),
    UMM_AL_QURA("উম্মুল কুরা (মক্কা)", 18.5, 0.0, ishaMinutesAfterMaghrib = 90)
}

data class UserPrefsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val nightReadingMode: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.BANGLA,
    val arabicFontSizeSp: Float = 28f,
    val banglaFontSizeSp: Float = 16f,
    val showTransliteration: Boolean = true,
    val quranLayoutMode: QuranLayoutMode = QuranLayoutMode.STACKED,
    val selectedReciterId: String = "ar.alafasy",
    val playbackSpeed: Float = 1.0f,
    val prayerCalcMethod: PrayerCalcMethod = PrayerCalcMethod.MWL,
    val adhanNotificationsEnabled: Boolean = true,
    val dailyAyahReminderEnabled: Boolean = true,
    val latitude: Double = 23.8103,
    val longitude: Double = 90.4125,
    val cityName: String = "ঢাকা, বাংলাদেশ",
    val localTasbihCount: Int = 0,
    val tasbihTarget: Int = 33,
    val localDisplayName: String = "মুসলিম ব্যবহারকারী"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NIGHT_READING = booleanPreferencesKey("night_reading")
        val LANGUAGE = stringPreferencesKey("app_language")
        val ARABIC_FONT = floatPreferencesKey("arabic_font_sp")
        val BANGLA_FONT = floatPreferencesKey("bangla_font_sp")
        val SHOW_TRANSLITERATION = booleanPreferencesKey("show_transliteration")
        val QURAN_LAYOUT = stringPreferencesKey("quran_layout")
        val RECITER_ID = stringPreferencesKey("reciter_id")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PRAYER_METHOD = stringPreferencesKey("prayer_method")
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val DAILY_REMINDER = booleanPreferencesKey("daily_reminder")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val CITY_NAME = stringPreferencesKey("city_name")
        val TASBIH_COUNT = intPreferencesKey("tasbih_count")
        val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        val LOCAL_DISPLAY_NAME = stringPreferencesKey("local_display_name")
    }

    val preferencesFlow: Flow<UserPrefsState> = context.dataStore.data.map { prefs ->
        UserPrefsState(
            themeMode = enumValues<ThemeMode>().firstOrNull { it.name == prefs[Keys.THEME_MODE] } ?: ThemeMode.SYSTEM,
            nightReadingMode = prefs[Keys.NIGHT_READING] ?: false,
            appLanguage = enumValues<AppLanguage>().firstOrNull { it.name == prefs[Keys.LANGUAGE] } ?: AppLanguage.BANGLA,
            arabicFontSizeSp = prefs[Keys.ARABIC_FONT] ?: 28f,
            banglaFontSizeSp = prefs[Keys.BANGLA_FONT] ?: 16f,
            showTransliteration = prefs[Keys.SHOW_TRANSLITERATION] ?: true,
            quranLayoutMode = enumValues<QuranLayoutMode>().firstOrNull { it.name == prefs[Keys.QURAN_LAYOUT] } ?: QuranLayoutMode.STACKED,
            selectedReciterId = prefs[Keys.RECITER_ID] ?: "ar.alafasy",
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            prayerCalcMethod = enumValues<PrayerCalcMethod>().firstOrNull { it.name == prefs[Keys.PRAYER_METHOD] } ?: PrayerCalcMethod.MWL,
            adhanNotificationsEnabled = prefs[Keys.ADHAN_ENABLED] ?: true,
            dailyAyahReminderEnabled = prefs[Keys.DAILY_REMINDER] ?: true,
            latitude = prefs[Keys.LATITUDE] ?: 23.8103,
            longitude = prefs[Keys.LONGITUDE] ?: 90.4125,
            cityName = prefs[Keys.CITY_NAME] ?: "ঢাকা, বাংলাদেশ",
            localTasbihCount = prefs[Keys.TASBIH_COUNT] ?: 0,
            tasbihTarget = prefs[Keys.TASBIH_TARGET] ?: 33,
            localDisplayName = prefs[Keys.LOCAL_DISPLAY_NAME] ?: "মুসলিম ব্যবহারকারী"
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setNightReadingMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NIGHT_READING] = enabled }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setFontSizes(arabicSp: Float, banglaSp: Float) {
        context.dataStore.edit {
            it[Keys.ARABIC_FONT] = arabicSp.coerceIn(20f, 44f)
            it[Keys.BANGLA_FONT] = banglaSp.coerceIn(13f, 26f)
        }
    }

    suspend fun setShowTransliteration(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TRANSLITERATION] = show }
    }

    suspend fun setQuranLayoutMode(mode: QuranLayoutMode) {
        context.dataStore.edit { it[Keys.QURAN_LAYOUT] = mode.name }
    }

    suspend fun setSelectedReciter(reciterId: String) {
        context.dataStore.edit { it[Keys.RECITER_ID] = reciterId }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun setPrayerMethod(method: PrayerCalcMethod) {
        context.dataStore.edit { it[Keys.PRAYER_METHOD] = method.name }
    }

    suspend fun setAdhanNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ADHAN_ENABLED] = enabled }
    }

    suspend fun setDailyReminder(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_REMINDER] = enabled }
    }

    suspend fun setLocation(lat: Double, lng: Double, city: String) {
        context.dataStore.edit {
            it[Keys.LATITUDE] = lat
            it[Keys.LONGITUDE] = lng
            it[Keys.CITY_NAME] = city
        }
    }

    suspend fun updateTasbih(count: Int, target: Int) {
        context.dataStore.edit {
            it[Keys.TASBIH_COUNT] = count
            it[Keys.TASBIH_TARGET] = target
        }
    }

    suspend fun setLocalDisplayName(name: String) {
        context.dataStore.edit { it[Keys.LOCAL_DISPLAY_NAME] = name }
    }
}
