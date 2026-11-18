package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.AudioPlaybackController
import com.example.data.local.SeedDataSurahs
import com.example.data.remote.NetworkModule
import com.example.domain.islamic.HijriCalendarHelper
import com.example.domain.islamic.PrayerTimeCalculator
import com.example.notifications.AdhanScheduler
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestDeep
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodels.HomeSettingsViewModel

@Composable
fun HomeScreen(
    viewModel: HomeSettingsViewModel,
    onOpenQuranSurah: (Int) -> Unit,
    onOpenHadithDetail: (String) -> Unit,
    onOpenToolRoute: (String) -> Unit,
    onOpenMatrimonial: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val schedule = remember(prefs.latitude, prefs.longitude, prefs.prayerCalcMethod) {
        PrayerTimeCalculator.calculateForToday(
            prefs.latitude,
            prefs.longitude,
            prefs.prayerCalcMethod
        )
    }
    val hijri = remember { HijriCalendarHelper.gregorianToHijri() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আসসালামু আলাইকুম, ${prefs.localDisplayName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${hijri.formattedBangla} • ${prefs.cityName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            AdhanScheduler.showImmediateTestNotification(
                                context,
                                "নামাজের সময় ও আজান সক্রিয়",
                                "পরবর্তী ওয়াক্ত: ${schedule.nextPrayer.nameBangla} (${schedule.nextPrayer.formatted12h})"
                            )
                        },
                        modifier = Modifier.testTag("home_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "আজান ও রিমাইন্ডার",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "সেটিংস ও অফলাইন"
                        )
                    }
                }
            }

            // Hero Prayer Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenToolRoute("prayer_qibla") }
                        .testTag("home_prayer_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_islamic_hero_banner),
                            contentDescription = "নামাজের সময়সূচি ব্যানার",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            ForestDeep.copy(alpha = 0.82f),
                                            EmeraldPrimary.copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "পরবর্তী নামাজ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldAccent
                                    )
                                    Text(
                                        text = "${schedule.nextPrayer.nameBangla} — ${schedule.nextPrayer.formatted12h}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    val hrs = schedule.minutesRemaining / 60
                                    val mins = schedule.minutesRemaining % 60
                                    Text(
                                        text = "বাকি আছে ${HijriCalendarHelper.toBanglaDigits(hrs)} ঘ. ${HijriCalendarHelper.toBanglaDigits(mins)} মি. • সূর্যোদয় ${schedule.sunriseFormatted}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.86f)
                                    )
                                }
                                Surface(
                                    color = GoldAccent.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = "কিবলা\n${HijriCalendarHelper.toBanglaDigits(schedule.qiblaDegrees.toInt())}°",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                schedule.slots.forEach { slot ->
                                    Surface(
                                        color = if (slot.isNext) GoldAccent else Color.White.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = slot.nameBangla,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (slot.isNext) ForestDeep else Color.White
                                            )
                                            Text(
                                                text = slot.formatted12h.substringBefore(" "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (slot.isNext) ForestDeep else Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }

                            val miniHighlights = listOf(
                                "সাহারি" to schedule.sahariFormatted,
                                "সূর্যদয়" to schedule.sunriseFormatted,
                                "ইশরাক" to schedule.ishraqFormatted,
                                "দ্বিপ্রহর (চাশত)" to schedule.chashtFormatted,
                                "সূর্য অস্ত" to schedule.sunsetFormatted,
                                "ইফতার" to schedule.iftarFormatted
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                miniHighlights.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { (label, timeStr) ->
                                            Surface(
                                                color = Color.White.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = GoldAccent,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = timeStr,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Important Hijri Day Banner if applicable
            if (hijri.specialDayBangla != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "আজকের বিশেষ ইসলামিক দিন: ${hijri.specialDayBangla}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // 8-tile Quick Actions Grid
            item {
                Text(
                    text = "দ্রুত আমল ও ইসলামিক সেবা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val actions = listOf(
                    QuickActionItem("দুআ ও যিকির", Icons.Rounded.VolunteerActivism, "dua"),
                    QuickActionItem("তাসবীহ", Icons.Rounded.Fingerprint, "tasbih"),
                    QuickActionItem("কিবলা কম্পাস", Icons.Rounded.Explore, "prayer_qibla"),
                    QuickActionItem("যাকাত", Icons.Rounded.AccountBalanceWallet, "zakat"),
                    QuickActionItem("হিজরি ক্যালেন্ডার", Icons.Rounded.CalendarMonth, "hijri"),
                    QuickActionItem("ইসলামিক কুইজ", Icons.Rounded.Quiz, "quiz"),
                    QuickActionItem("ম্যাট্রিমোনি", Icons.Rounded.Favorite, "matrimonial"),
                    QuickActionItem("অফলাইন ও সেটিংস", Icons.Rounded.DownloadForOffline, "settings")
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    actions.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                QuickActionTile(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        when (item.routeKey) {
                                            "matrimonial" -> onOpenMatrimonial()
                                            "settings" -> onOpenSettings()
                                            else -> onOpenToolRoute(item.routeKey)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Daily Ayah Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { onOpenQuranSurah(2) },
                                label = { Text("আজকের আয়াত • আল-বাকারা ২:১৫২") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "فَاذْكُرُونِي أَذْكُرْكُمْ\n“সুতরাং তোমরা আমাকে স্মরণ করো, আমিও তোমাদেরকে স্মরণ রাখবো।” — সূরা আল-বাকারা ২:১৫২\n(মুসলিম বাংলা অ্যাপ)"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "আয়াত শেয়ার করুন"))
                                }
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "আয়াত শেয়ার")
                            }
                        }

                        Text(
                            text = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
                            fontFamily = AmiriFontFamily,
                            fontSize = 24.sp,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "“সুতরাং তোমরা আমাকে স্মরণ করো, আমিও তোমাদেরকে স্মরণ রাখবো। আর আমার কৃতজ্ঞতা প্রকাশ করো; অকৃতজ্ঞ হয়ো না।”",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = { onOpenQuranSurah(2) }) {
                                Text("তাফসীর পড়ুন")
                            }
                            FilledTonalButton(onClick = {
                                val reciter = SeedDataSurahs.reciters.firstOrNull { it.id == prefs.selectedReciterId }
                                    ?: SeedDataSurahs.reciters.first()
                                AudioPlaybackController.play(
                                    context = context,
                                    sourceUrlOrPath = NetworkModule.ayahAudioUrl(reciter.id, 159),
                                    surahId = 2,
                                    surahNameBangla = "আল-বাকারা • আয়াত ১৫২",
                                    reciterNameBangla = reciter.nameBangla,
                                    ayahNumber = 152
                                )
                            }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("তিলাওয়াত শুনুন")
                            }
                        }
                    }
                }
            }

            // Daily Hadith Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenHadithDetail("bukhari_1") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "আজকের সহীহ হাদিস • সহীহ বুখারী #১",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "“সকল আমল নিয়তের উপর নির্ভরশীল এবং প্রত্যেক ব্যক্তি তাই পাবে যা সে নিয়ত করেছে।”",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "সহীহ (মুত্তাফাকুন আলাইহি)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            TextButton(onClick = { onOpenHadithDetail("bukhari_1") }) {
                                Text("সনদ ও জারহ-তাদীল →")
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class QuickActionItem(val title: String, val icon: ImageVector, val routeKey: String)

@Composable
private fun QuickActionTile(
    item: QuickActionItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("quick_action_${item.routeKey}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
