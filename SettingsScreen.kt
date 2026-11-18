package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppLanguage
import com.example.data.local.PrayerCalcMethod
import com.example.data.local.ThemeMode
import com.example.notifications.AdhanScheduler
import com.example.ui.viewmodels.HomeSettingsViewModel
import com.example.widget.MuslimBanglaWidgetProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: HomeSettingsViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val hadithBooks by viewModel.hadithBooks.collectAsStateWithLifecycle()
    val offlineAudios by viewModel.offlineAudios.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isBulkDownloading by viewModel.isBulkDownloading.collectAsStateWithLifecycle()

    var localNameInput by remember(prefs.localDisplayName) { mutableStateOf(prefs.localDisplayName) }
    val snackbarHostState = remember { SnackbarHostState() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setAdhanEnabled(true)
            AdhanScheduler.scheduleDailyPrayers(
                context, prefs.latitude, prefs.longitude,
                prefs.prayerCalcMethod, true, prefs.dailyAyahReminderEnabled
            )
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("সেটিংস ও অফলাইন স্টোরেজ", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "পেছনে যান")
                }
            }
        )
        SnackbarHost(hostState = snackbarHostState)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Local Profile Card (No Sign-In Required)
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("স্থানীয় প্রোফাইল (লগইন ছাড়াই)", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "আপনার বুকমার্ক, আয়াত নোট ও ম্যাট্রিমোনিয়াল তথ্য ডিভাইসের Room ডাটাবেসে অফলাইনে সংরক্ষিত থাকে।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = localNameInput,
                            onValueChange = { localNameInput = it },
                            label = { Text("আপনার নাম (ঐচ্ছিক)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = { viewModel.updateLocalDisplayName(localNameInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_local_name_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("নাম সংরক্ষণ করুন")
                        }
                    }
                }

                // Theme & Language Card
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("থিম, ভাষা ও পাঠ সেটিংস", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("অ্যাপ থিম", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                ThemeMode.SYSTEM to "সিস্টেম",
                                ThemeMode.LIGHT to "লাইট মোড",
                                ThemeMode.DARK to "ডার্ক মোড"
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = prefs.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("কুরআন নাইট রিডিং মোড")
                            Switch(
                                checked = prefs.nightReadingMode,
                                onCheckedChange = { viewModel.setNightReading(it) }
                            )
                        }

                        Text("প্রাথমিক ভাষা", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AppLanguage.BANGLA to "বাংলা",
                                AppLanguage.ARABIC to "العربية",
                                AppLanguage.ENGLISH to "English"
                            ).forEach { (lang, label) ->
                                FilterChip(
                                    selected = prefs.appLanguage == lang,
                                    onClick = { viewModel.setLanguage(lang) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Text("আরবি ফন্ট সাইজ: ${prefs.arabicFontSizeSp.toInt()} sp")
                        Slider(
                            value = prefs.arabicFontSizeSp,
                            onValueChange = { viewModel.setFontSizes(it, prefs.banglaFontSizeSp) },
                            valueRange = 20f..44f
                        )

                        Text("বাংলা ফন্ট সাইজ: ${prefs.banglaFontSizeSp.toInt()} sp")
                        Slider(
                            value = prefs.banglaFontSizeSp,
                            onValueChange = { viewModel.setFontSizes(prefs.arabicFontSizeSp, it) },
                            valueRange = 13f..26f
                        )
                    }
                }

                // Location Settings Card (Division -> District -> Upazila + GPS)
                BangladeshLocationSelectorCard(
                    currentCityName = prefs.cityName,
                    currentLat = prefs.latitude,
                    currentLng = prefs.longitude,
                    prayerCalcMethod = prefs.prayerCalcMethod,
                    adhanNotificationsEnabled = prefs.adhanNotificationsEnabled,
                    dailyAyahReminderEnabled = prefs.dailyAyahReminderEnabled,
                    testTagPrefix = "settings",
                    onGpsStatusMessage = { msg -> viewModel.postStatus(msg) },
                    onLocationSelected = { lat, lng, label ->
                        viewModel.setLocation(lat, lng, label)
                    }
                )

                // Prayer Calculation & Notifications Card
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("নামাজের গণনা ও আজান নোটিফিকেশন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("গণনা পদ্ধতি (ডিফল্ট: মুসলিম ওয়ার্ল্ড লিগ • স্ট্যান্ডার্ড আসর)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrayerCalcMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = prefs.prayerCalcMethod == method,
                                    onClick = {
                                        viewModel.setPrayerMethod(method)
                                        AdhanScheduler.scheduleDailyPrayers(
                                            context, prefs.latitude, prefs.longitude, method,
                                            prefs.adhanNotificationsEnabled, prefs.dailyAyahReminderEnabled
                                        )
                                        MuslimBanglaWidgetProvider.refreshAll(context)
                                    },
                                    label = { Text(method.titleBangla) }
                                )
                            }
                        }

                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("পাঁচ ওয়াক্ত আজান নোটিফিকেশন")
                            Switch(
                                checked = prefs.adhanNotificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setAdhanEnabled(enabled)
                                        AdhanScheduler.scheduleDailyPrayers(
                                            context, prefs.latitude, prefs.longitude, prefs.prayerCalcMethod,
                                            enabled, prefs.dailyAyahReminderEnabled
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("settings_adhan_switch")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("দৈনিক আয়াত ও হাদিস রিমাইন্ডার")
                            Switch(
                                checked = prefs.dailyAyahReminderEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setDailyReminder(enabled)
                                    AdhanScheduler.scheduleDailyPrayers(
                                        context, prefs.latitude, prefs.longitude, prefs.prayerCalcMethod,
                                        prefs.adhanNotificationsEnabled, enabled
                                    )
                                },
                                modifier = Modifier.testTag("settings_daily_reminder_switch")
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                AdhanScheduler.showImmediateTestNotification(
                                    context,
                                    "মুসলিম বাংলা • আজান টেস্ট",
                                    "আসসালাতু খাইরুম মিনান নাউম — আপনার আজান নোটিফিকেশন সঠিকভাবে কাজ করছে।"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_adhan_notification_button")
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("টেস্ট নোটিফিকেশন পাঠান")
                        }
                    }
                }

                // Offline Storage Manager Card
                val downloadedSurahsCount = surahs.count { it.isTextDownloaded }
                val downloadedHadithBooksCount = hadithBooks.count { it.isDownloaded }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "অফলাইন স্টোরেজ ম্যানেজার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("সংরক্ষিত সূরা: $downloadedSurahsCount / ১১৪ টি")
                        Text("সংরক্ষিত হাদিস গ্রন্থ: $downloadedHadithBooksCount / ১০ টি")
                        Text("অফলাইন অডিও তিলাওয়াত ফাইল: ${offlineAudios.size} টি")

                        Button(
                            onClick = { viewModel.downloadStarterOfflinePack() },
                            enabled = !isBulkDownloading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_starter_offline_pack_button")
                        ) {
                            if (isBulkDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("গুরুত্বপূর্ণ সূরা অফলাইনে সংরক্ষণ করুন")
                        }

                        OutlinedButton(
                            onClick = { viewModel.downloadAll114SurahsOffline() },
                            enabled = !isBulkDownloading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_all_114_surahs_button")
                        ) {
                            Icon(Icons.Rounded.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("সম্পূর্ণ ১১৪ সূরা ও তাফসীর ডাউনলোড করুন")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
