package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.PrayerCalcMethod
import com.example.domain.islamic.BangladeshLocations
import com.example.domain.islamic.DailyPrayerSchedule
import com.example.domain.islamic.PrayerTimeCalculator
import com.example.notifications.AdhanScheduler
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.widget.MuslimBanglaWidgetProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

private fun fetchGpsLocation(
    context: Context,
    onResolved: (Double, Double, String) -> Unit,
    onError: (String) -> Unit
) {
    runCatching {
        if (!hasLocationPermission(context)) {
            onError("লোকেশন অনুমতি দেওয়া হয়নি। নিচ থেকে বিভাগ, জেলা ও উপজেলা বেছে নিন।")
            return
        }
        val client = LocationServices.getFusedLocationProviderClient(context)

        fun applyCoordinates(lat: Double, lng: Double) {
            val nearest = BangladeshLocations.findNearestUpazila(lat, lng)
            val label = if (nearest != null) {
                "${nearest.displayLabel} (জিপিএস)"
            } else {
                "জিপিএস অবস্থান (${String.format("%.2f, %.2f", lat, lng)})"
            }
            onResolved(lat, lng, label)
        }

        val cts = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { fresh ->
                if (fresh != null) {
                    applyCoordinates(fresh.latitude, fresh.longitude)
                } else {
                    client.lastLocation
                        .addOnSuccessListener { cached ->
                            if (cached != null) {
                                applyCoordinates(cached.latitude, cached.longitude)
                            } else {
                                onError("ডিভাইসের জিপিএস পাওয়া যায়নি। ফোনের Location চালু করুন অথবা উপজেলা সিলেক্ট করুন।")
                            }
                        }
                        .addOnFailureListener {
                            onError("জিপিএস লোকেশন পড়া যায়নি। অনুগ্রহ করে উপজেলা সিলেক্ট করুন।")
                        }
                }
            }
            .addOnFailureListener {
                client.lastLocation
                    .addOnSuccessListener { cached ->
                        if (cached != null) {
                            applyCoordinates(cached.latitude, cached.longitude)
                        } else {
                            onError("জিপিএস সেবা এই মুহূর্তে পাওয়া যাচ্ছে না। অনুগ্রহ করে বিভাগ → জেলা → উপজেলা সিলেক্ট করুন।")
                        }
                    }
                    .addOnFailureListener {
                        onError("জিপিএস সেবা এই মুহূর্তে পাওয়া যাচ্ছে না। অনুগ্রহ করে উপজেলা সিলেক্ট করুন।")
                    }
            }
    }.onFailure {
        onError("জিপিএস চালু করতে সমস্যা হয়েছে। নিচ থেকে উপজেলা সিলেক্ট করুন।")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BangladeshLocationSelectorCard(
    currentCityName: String,
    currentLat: Double,
    currentLng: Double,
    prayerCalcMethod: PrayerCalcMethod,
    adhanNotificationsEnabled: Boolean,
    dailyAyahReminderEnabled: Boolean,
    testTagPrefix: String,
    onGpsStatusMessage: (String) -> Unit = {},
    onLocationSelected: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current

    fun applyAndSyncLocation(lat: Double, lng: Double, label: String) {
        onLocationSelected(lat, lng, label)
        AdhanScheduler.scheduleDailyPrayers(
            context = context,
            latitude = lat,
            longitude = lng,
            method = prayerCalcMethod,
            adhanEnabled = adhanNotificationsEnabled,
            dailyReminderEnabled = dailyAyahReminderEnabled
        )
        MuslimBanglaWidgetProvider.refreshAll(context)
    }

    val initialSelection = remember(currentCityName) {
        BangladeshLocations.resolveSelectionFromCityName(currentCityName)
    }
    var selectedDivision by remember { mutableStateOf(initialSelection.first) }
    var selectedDistrict by remember { mutableStateOf(initialSelection.second) }
    var selectedUpazila by remember { mutableStateOf(initialSelection.third) }

    LaunchedEffect(currentCityName) {
        val resolved = BangladeshLocations.resolveSelectionFromCityName(currentCityName)
        if (currentCityName.contains(resolved.third.nameBangla)) {
            selectedDivision = resolved.first
            selectedDistrict = resolved.second
            selectedUpazila = resolved.third
        }
    }

    var divisionExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var upazilaExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLocating by remember { mutableStateOf(false) }

    val searchResults = remember(searchQuery) {
        BangladeshLocations.searchUpazilas(searchQuery)
    }

    fun startGpsLookup() {
        isLocating = true
        fetchGpsLocation(
            context = context,
            onResolved = { lat, lng, label ->
                isLocating = false
                applyAndSyncLocation(lat, lng, label)
            },
            onError = { msg ->
                isLocating = false
                onGpsStatusMessage(msg)
            }
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            startGpsLookup()
        } else {
            onGpsStatusMessage("জিপিএস অনুমতি বাতিল করা হয়েছে। নিচ থেকে বিভাগ → জেলা → উপজেলা বেছে নিন।")
        }
    }

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "নামাজের লোকেশন: $currentCityName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${prayerCalcMethod.titleBangla} • স্ট্যান্ডার্ড আসর • ${String.format("%.2f", currentLat)}°, ${String.format("%.2f", currentLng)}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    enabled = !isLocating,
                    onClick = {
                        if (hasLocationPermission(context)) {
                            startGpsLookup()
                        } else {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("${testTagPrefix}_gps_location_button")
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("খুঁজছে...")
                    } else {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "জিপিএস লোকেশন", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("জিপিএস")
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "বাংলাদেশের বিভাগ → জেলা → উপজেলা নির্বাচন করুন",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            // Search Upazila directly
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("সরাসরি উপজেলা বা জেলা খুঁজুন") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "মুছুন")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_upazila_search_input")
            )

            if (searchResults.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    searchResults.take(8).forEach { upazila ->
                        FilterChip(
                            selected = currentCityName.contains(upazila.nameBangla) && currentCityName.contains(upazila.districtBangla),
                            onClick = {
                                applyAndSyncLocation(upazila.lat, upazila.lng, upazila.displayLabel)
                                searchQuery = ""
                            },
                            label = { Text(upazila.displayLabel) }
                        )
                    }
                }
            }

            // 1. Division Dropdown
            ExposedDropdownMenuBox(
                expanded = divisionExpanded,
                onExpandedChange = { divisionExpanded = !divisionExpanded }
            ) {
                OutlinedTextField(
                    value = "${selectedDivision.nameBangla} বিভাগ",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("১. বিভাগ") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .testTag("${testTagPrefix}_division_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = divisionExpanded,
                    onDismissRequest = { divisionExpanded = false }
                ) {
                    BangladeshLocations.divisions.forEach { div ->
                        DropdownMenuItem(
                            text = { Text("${div.nameBangla} বিভাগ (${div.districts.size} জেলা)") },
                            onClick = {
                                selectedDivision = div
                                val firstDist = div.districts.first()
                                selectedDistrict = firstDist
                                val firstUpazila = firstDist.upazilas.first()
                                selectedUpazila = firstUpazila
                                divisionExpanded = false
                                applyAndSyncLocation(
                                    firstUpazila.lat,
                                    firstUpazila.lng,
                                    firstUpazila.displayLabel
                                )
                            }
                        )
                    }
                }
            }

            // 2. District & 3. Upazila Dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDistrict.nameBangla,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("২. জেলা") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .testTag("${testTagPrefix}_district_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        selectedDivision.districts.forEach { dist ->
                            DropdownMenuItem(
                                text = { Text("${dist.nameBangla} (${dist.upazilas.size})") },
                                onClick = {
                                    selectedDistrict = dist
                                    val firstUpazila = dist.upazilas.first()
                                    selectedUpazila = firstUpazila
                                    districtExpanded = false
                                    applyAndSyncLocation(
                                        firstUpazila.lat,
                                        firstUpazila.lng,
                                        firstUpazila.displayLabel
                                    )
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = upazilaExpanded,
                    onExpandedChange = { upazilaExpanded = !upazilaExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUpazila.nameBangla,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("৩. উপজেলা") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upazilaExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .testTag("${testTagPrefix}_upazila_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = upazilaExpanded,
                        onDismissRequest = { upazilaExpanded = false }
                    ) {
                        selectedDistrict.upazilas.forEach { upz ->
                            DropdownMenuItem(
                                text = { Text(upz.nameBangla) },
                                onClick = {
                                    selectedUpazila = upz
                                    upazilaExpanded = false
                                    applyAndSyncLocation(upz.lat, upz.lng, upz.displayLabel)
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "দ্রুত নির্বাচন",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PrayerTimeCalculator.bangladeshCities.filter {
                    it.nameBangla.contains("ঢাকা") ||
                        it.nameBangla.contains("চট্টগ্রাম") ||
                        it.nameBangla.contains("সিলেট") ||
                        it.nameBangla.contains("মক্কা") ||
                        it.nameBangla.contains("মদিনা")
                }.forEach { preset ->
                    FilterChip(
                        selected = currentCityName == preset.nameBangla,
                        onClick = { applyAndSyncLocation(preset.lat, preset.lng, preset.nameBangla) },
                        label = { Text(preset.nameBangla.substringBefore(",")) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExtendedPrayerTimesCard(schedule: DailyPrayerSchedule) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.testTag("prayer_extended_times_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "সাহারি, ইফতার ও বিশেষ সময়ের সূচি",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Prominent Sahari & Iftar Pair
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FastingTimeBadgeCard(
                    title = "সাহারি",
                    subtitle = "শেষ সময় (সুবহে সাদিক)",
                    time = schedule.sahariFormatted,
                    icon = Icons.Rounded.NightsStay,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                FastingTimeBadgeCard(
                    title = "ইফতার",
                    subtitle = "মাগরিবের শুরু",
                    time = schedule.iftarFormatted,
                    icon = Icons.Rounded.Restaurant,
                    containerColor = GoldAccent.copy(alpha = 0.22f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // Sunrise, Ishraq, Chasht/Midday, Sunset 2x2 Grid
            val solarItems = listOf(
                Triple("সূর্যদয় (Sunrise)", schedule.sunriseFormatted, Icons.Rounded.WbTwilight to "ফজরের শেষ"),
                Triple("ইশরাক", schedule.ishraqFormatted, Icons.Rounded.WbSunny to "সূর্যোদয়ের ১৫ মি. পর"),
                Triple("দ্বিপ্রহর (চাশত)", schedule.chashtFormatted, Icons.Rounded.LightMode to "জাওয়াল: ${schedule.zawalFormatted}"),
                Triple("সূর্য অস্ত (Sunset)", schedule.sunsetFormatted, Icons.Rounded.Brightness3 to "দিনের সমাপ্তি")
            )

            solarItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { (title, time, iconAndSub) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = iconAndSub.first,
                                        contentDescription = title,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = iconAndSub.second,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastingTimeBadgeCard(
    title: String,
    subtitle: String,
    time: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
