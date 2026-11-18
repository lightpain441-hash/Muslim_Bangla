package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SeedDataHadithDuaQuiz
import com.example.domain.islamic.HijriCalendarHelper
import com.example.domain.islamic.PrayerTimeCalculator
import com.example.domain.islamic.ZakatCalculator
import com.example.notifications.AdhanScheduler
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestDeep
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodels.ToolsViewModel
import com.example.ui.viewmodels.UiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsHubScreen(onNavigateTool: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("ইসলামিক আমল ও টুলস", fontWeight = FontWeight.Bold) })
        val itemsList = listOf(
            Triple("prayer_qibla", "নামাজের সময়সূচি ও কিবলা কম্পাস", Icons.Rounded.Explore),
            Triple("hijri", "হিজরি ক্যালেন্ডার ও গুরুত্বপূর্ণ দিন", Icons.Rounded.CalendarMonth),
            Triple("dua", "দৈনন্দিন দুআ ও যিকির সংগ্রহ", Icons.Rounded.VolunteerActivism),
            Triple("tasbih", "ডিজিটাল তাসবীহ কাউন্টার", Icons.Rounded.Fingerprint),
            Triple("zakat", "যাকাত ক্যালকুলেটর (নিসাবসহ)", Icons.Rounded.AccountBalanceWallet),
            Triple("quiz", "ইসলামিক কুইজ (কুরআন ও হাদিস)", Icons.Rounded.Quiz)
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(itemsList) { (route, title, icon) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateTool(route) }
                        .testTag("tool_card_$route"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerQiblaScreen(
    viewModel: ToolsViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val cachedSchedule by viewModel.cachedPrayerSchedule.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val schedule = remember(prefs.latitude, prefs.longitude, prefs.prayerCalcMethod) {
        PrayerTimeCalculator.calculateForToday(
            prefs.latitude, prefs.longitude, prefs.prayerCalcMethod
        )
    }
    val hijriToday = remember { HijriCalendarHelper.gregorianToHijri() }

    LaunchedEffect(prefs.cityName, prefs.latitude, prefs.longitude, prefs.prayerCalcMethod) {
        viewModel.syncPrayerScheduleCache(
            city = prefs.cityName,
            lat = prefs.latitude,
            lng = prefs.longitude,
            methodName = prefs.prayerCalcMethod.titleBangla,
            schedule = schedule
        )
    }

    var deviceAzimuth by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (accel == null || magnet == null) {
            sensorAvailable = false
            onDispose { }
        } else {
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasG = false
            var hasM = false
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasG = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasM = true
                    }
                    if (hasG && hasM) {
                        val r = FloatArray(9)
                        val i = FloatArray(9)
                        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            deviceAzimuth = (deg + 360f) % 360f
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, magnet, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }

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

    val needleRotation by animateFloatAsState(
        targetValue = (schedule.qiblaDegrees.toFloat() - deviceAzimuth + 360f) % 360f,
        label = "qibla_needle"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("নামাজ ও কিবলা কম্পাস", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান") }
            }
        )
        SnackbarHost(hostState = snackbarHostState)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Prayer Countdown & Summary Banner
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer_hero_summary_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ForestDeep, EmeraldPrimary)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${prefs.cityName} • ${hijriToday.formattedBangla}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldAccent
                                )
                                Text(
                                    text = "পরবর্তী: ${schedule.nextPrayer.nameBangla} — ${schedule.nextPrayer.formatted12h}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val hrs = schedule.minutesRemaining / 60
                                val mins = schedule.minutesRemaining % 60
                                Text(
                                    text = "বাকি আছে ${HijriCalendarHelper.toBanglaDigits(hrs)} ঘণ্টা ${HijriCalendarHelper.toBanglaDigits(mins)} মিনিট • স্ট্যান্ডার্ড আসর",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val quickPills = listOf(
                                "সাহারি" to schedule.sahariFormatted,
                                "সূর্যদয়" to schedule.sunriseFormatted,
                                "ইফতার" to schedule.iftarFormatted
                            )
                            quickPills.forEach { (title, timeStr) ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldAccent
                                        )
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Five Fard Prayer Times & Adhan Toggle
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("prayer_fard_times_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("পাঁচ ওয়াক্ত নামাজের সময়সূচি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "আজান অ্যালার্ট ও স্ট্যান্ডার্ড আসর (ছায়া বস্তুর সমান)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                        context, prefs.latitude, prefs.longitude,
                                        prefs.prayerCalcMethod, enabled, prefs.dailyAyahReminderEnabled
                                    )
                                }
                            }
                        )
                    }
                    schedule.slots.forEach { slot ->
                        Surface(
                            color = if (slot.isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text(slot.nameBangla, fontWeight = FontWeight.SemiBold)
                                    if (slot.isNext) {
                                        Surface(
                                            color = EmeraldPrimary,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "পরবর্তী",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(slot.formatted12h, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (cachedSchedule != null) {
                        Text(
                            text = "অফলাইন Room ক্যাশ: ${cachedSchedule?.locationName} (${cachedSchedule?.dateKey})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Extended Times: Sahari, Iftar, Sunrise, Ishraq, Chasht/Midday, Sunset
            ExtendedPrayerTimesCard(schedule = schedule)

            // 4. Division -> District -> Upazila & GPS Location Selector
            BangladeshLocationSelectorCard(
                currentCityName = prefs.cityName,
                currentLat = prefs.latitude,
                currentLng = prefs.longitude,
                prayerCalcMethod = prefs.prayerCalcMethod,
                adhanNotificationsEnabled = prefs.adhanNotificationsEnabled,
                dailyAyahReminderEnabled = prefs.dailyAyahReminderEnabled,
                testTagPrefix = "prayer",
                onGpsStatusMessage = { msg -> viewModel.postFeedback(msg) },
                onLocationSelected = { lat, lng, label ->
                    viewModel.setLocation(lat, lng, label)
                }
            )

            // 5. Qibla Compass Card
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("কিবলা কম্পাস (কাবা শরীফের দিক)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "কিবলার কোণ: ${HijriCalendarHelper.toBanglaDigits(schedule.qiblaDegrees.roundToInt())}° (উত্তর থেকে)",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary
                    )
                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f - 10f
                            drawCircle(
                                color = EmeraldPrimary.copy(alpha = 0.14f),
                                radius = r
                            )
                            drawCircle(
                                color = EmeraldPrimary,
                                radius = r,
                                style = Stroke(width = 6f)
                            )
                            rotate(degrees = needleRotation, pivot = center) {
                                val tip = Offset(center.x, center.y - r + 18f)
                                val path = Path().apply {
                                    moveTo(tip.x, tip.y)
                                    lineTo(center.x - 16f, center.y)
                                    lineTo(center.x + 16f, center.y)
                                    close()
                                }
                                drawPath(path = path, color = GoldAccent)
                                drawCircle(color = ForestDeep, radius = 12f, center = center)
                            }
                        }
                    }
                    Text(
                        text = if (sensorAvailable) "ফোন সমতল রাখুন এবং সোনালী তীর কাবার দিকে নির্দেশ করবে।"
                        else "এই ডিভাইসে ম্যাগনেটোমিটার সেন্সর নেই; ওপরের ডিগ্রি (${schedule.qiblaDegrees.roundToInt()}°) অনুসরণ করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val today = remember { HijriCalendarHelper.gregorianToHijri() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("হিজরি ক্যালেন্ডার", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান") }
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestDeep, contentColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("আজকের হিজরি তারিখ", style = MaterialTheme.typography.labelLarge, color = GoldAccent)
                        Text(today.formattedBangla, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (today.specialDayBangla != null) {
                            Text(today.specialDayBangla, color = GoldAccent)
                        }
                    }
                }
            }
            item {
                Text("গুরুত্বপূর্ণ ইসলামিক দিনসমূহ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(HijriCalendarHelper.importantDays) { day ->
                val monthName = HijriCalendarHelper.hijriMonthsBangla[day.hijriMonth - 1]
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(day.titleBangla, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${HijriCalendarHelper.toBanglaDigits(day.hijriDay)} $monthName", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(day.descriptionBangla, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaDhikrScreen(viewModel: ToolsViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val duas by viewModel.duas.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val categories = remember(duas) { listOf("সব") + duas.map { it.categoryBangla }.distinct() }
    var selectedCategory by remember { mutableStateOf("সব") }
    val filtered = remember(duas, selectedCategory) {
        if (selectedCategory == "সব") duas else duas.filter { it.categoryBangla == selectedCategory }
    }
    val bookmarkedIds = remember(bookmarksState) {
        (bookmarksState as? UiState.Success)?.data?.map { it.id }?.toSet() ?: emptySet()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("দৈনন্দিন দুআ ও যিকির", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান") }
            }
        )
        SnackbarHost(hostState = snackbarHostState)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) }
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.id }) { dua ->
                val isBm = "dua_${dua.id}" in bookmarkedIds
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dua.titleBangla, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Row {
                                IconButton(onClick = { viewModel.toggleBookmarkDua(dua, isBm) }) {
                                    Icon(
                                        imageVector = if (isBm) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = "বুকমার্ক",
                                        tint = if (isBm) GoldAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${dua.titleBangla}\n\n${dua.arabicText}\n\n${dua.banglaMeaning}\n— ${dua.reference}")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "দুআ শেয়ার করুন"))
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "শেয়ার")
                                }
                            }
                        }
                        Text(
                            text = dua.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 23.sp,
                            lineHeight = 38.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(dua.banglaPronunciation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(dua.banglaMeaning, style = MaterialTheme.typography.bodyMedium)
                        Text("${dua.reference} • পাঠ: ${dua.repeatCount} বার", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(viewModel: ToolsViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("ডিজিটাল তাসবীহ কাউন্টার", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান") }
            },
            actions = {
                IconButton(onClick = { viewModel.resetTasbih(prefs.tasbihTarget) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "রিসেট")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(33, 99, 100, 1000).forEach { target ->
                    FilterChip(
                        selected = prefs.tasbihTarget == target,
                        onClick = { viewModel.setTasbihTarget(prefs.localTasbihCount, target) },
                        label = { Text("টার্গেট ${HijriCalendarHelper.toBanglaDigits(target)}") }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.incrementTasbih(prefs.localTasbihCount, prefs.tasbihTarget)
                    }
                    .testTag("tasbih_tap_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = HijriCalendarHelper.toBanglaDigits(prefs.localTasbihCount),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "টার্গেট: ${HijriCalendarHelper.toBanglaDigits(prefs.tasbihTarget)}",
                        color = GoldAccent,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text("ট্যাপ করুন", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                text = "সুবহানাল্লাহ • আলহামদুলিল্লাহ • আল্লাহু আকবার",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var cash by remember { mutableStateOf("") }
    var gold by remember { mutableStateOf("") }
    var silver by remember { mutableStateOf("") }
    var business by remember { mutableStateOf("") }
    var debt by remember { mutableStateOf("") }
    var silverPrice by remember { mutableStateOf("145") }

    val result = remember(cash, gold, silver, business, debt, silverPrice) {
        ZakatCalculator.calculate(
            cashAndBank = cash.toDoubleOrNull() ?: 0.0,
            goldValue = gold.toDoubleOrNull() ?: 0.0,
            silverValue = silver.toDoubleOrNull() ?: 0.0,
            investmentsAndShares = 0.0,
            businessInventory = business.toDoubleOrNull() ?: 0.0,
            shortTermDebts = debt.toDoubleOrNull() ?: 0.0,
            silverPricePerGramBdt = silverPrice.toDoubleOrNull() ?: 145.0
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("যাকাত ক্যালকুলেটর", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান") }
            }
        )
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .align(Alignment.CenterHorizontally)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ZakatField("নগদ টাকা ও ব্যাংক ব্যালেন্স (টাকা)", cash) { cash = it }
            ZakatField("স্বর্ণের বর্তমান বাজারমূল্য (টাকা)", gold) { gold = it }
            ZakatField("রৌপ্যের বর্তমান বাজারমূল্য (টাকা)", silver) { silver = it }
            ZakatField("ব্যবসায়িক পণ্য ও বিনিয়োগ (টাকা)", business) { business = it }
            ZakatField("স্বল্পমেয়াদি ঋণ / দেনা (টাকা)", debt) { debt = it }
            ZakatField("প্রতি গ্রাম রৌপ্যের দাম (নিসাব গণনায়)", silverPrice) { silverPrice = it }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("যাকাত হিসাবের সারসংক্ষেপ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("মোট সম্পদ: ৳${String.format("%,.0f", result.totalAssets)}")
                    Text("নীট যাকাতযোগ্য সম্পদ: ৳${String.format("%,.0f", result.netZakatableWealth)}")
                    Text("রৌপ্য নিসাব (৬১২.৩৬ গ্রাম): ৳${String.format("%,.0f", result.nisabThreshold)}")
                    Text(
                        text = if (result.isEligible) "প্রদেয় যাকাত (২.৫%): ৳${String.format("%,.0f", result.payableZakat)}"
                        else "আপনার সম্পদ নিসাব পরিমাণের নিচে থাকায় যাকাত ফরজ নয়।",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestDeep
                    )
                }
            }
        }
    }
}

@Composable
private fun ZakatField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicQuizScreen(viewModel: ToolsViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val questions = SeedDataHadithDuaQuiz.quizQuestions
    val attemptsState by viewModel.quizAttemptsState.collectAsStateWithLifecycle()

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("ইসলামিক কুইজ", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান") }
            }
        )

        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!finished) {
                val q = questions[currentIndex]
                Text("প্রশ্ন ${currentIndex + 1} / ${questions.size} • ${q.category}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(q.questionBangla, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        q.options.forEachIndexed { idx, opt ->
                            val isSelected = selectedOption == idx
                            val isCorrect = idx == q.correctIndex
                            val container = when {
                                selectedOption == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                isCorrect -> MaterialTheme.colorScheme.primaryContainer
                                isSelected -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                            Surface(
                                color = container,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = selectedOption == null) {
                                        selectedOption = idx
                                        if (idx == q.correctIndex) score++
                                    }
                            ) {
                                Text(opt, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (selectedOption != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ব্যাখ্যা: ${q.explanationBangla}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                            }
                            Button(
                                onClick = {
                                    if (currentIndex + 1 < questions.size) {
                                        currentIndex++
                                        selectedOption = null
                                    } else {
                                        finished = true
                                        viewModel.saveQuizAttempt("কুরআন ও হাদিস", score, questions.size)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (currentIndex + 1 < questions.size) "পরবর্তী প্রশ্ন" else "ফলাফল দেখুন")
                            }
                        }
                    }
                }
            } else {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("কুইজ সম্পন্ন!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("আপনার স্কোর: $score / ${questions.size}", style = MaterialTheme.typography.titleLarge, color = EmeraldPrimary)
                        OutlinedButton(onClick = {
                            currentIndex = 0
                            selectedOption = null
                            score = 0
                            finished = false
                        }) {
                            Text("আবার খেলুন")
                        }
                    }
                }
            }

            val history = (attemptsState as? UiState.Success)?.data.orEmpty()
            if (history.isNotEmpty()) {
                Text("পূর্ববর্তী ফলাফল", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                history.take(5).forEach { attempt ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(attempt.category)
                            Text("${attempt.score} / ${attempt.totalQuestions}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
