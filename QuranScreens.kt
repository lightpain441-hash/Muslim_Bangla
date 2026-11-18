package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioPlaybackController
import com.example.data.local.AyahEntity
import com.example.data.local.QuranLayoutMode
import com.example.data.local.SeedDataSurahs
import com.example.data.local.SurahEntity
import com.example.data.remote.NetworkModule
import com.example.domain.islamic.HijriCalendarHelper
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestDeep
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodels.QuranViewModel
import com.example.ui.viewmodels.UiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranHubScreen(
    viewModel: QuranViewModel,
    onOpenSurah: (Int) -> Unit
) {
    val surahs by viewModel.allSurahs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val notesState by viewModel.ayahNotesState.collectAsStateWithLifecycle()
    val juzAyahs by viewModel.ayahsForSelectedJuz.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedJuz by rememberSaveable { mutableIntStateOf(30) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("পবিত্র কুরআন (১১৪ সূরা)", fontWeight = FontWeight.Bold) }
        )
        if (isDownloading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        SnackbarHost(hostState = snackbarHostState)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearch,
            placeholder = { Text("আয়াত, অনুবাদ বা তাফসীর খুঁজুন...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "খুঁজুন") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("quran_search_input")
        )

        if (searchQuery.isNotBlank()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(searchResults, key = { it.id }) { ayah ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSurah(ayah.surahId) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "সূরা ${ayah.surahId} : আয়াত ${ayah.ayahNumber}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ayah.arabicUthmani,
                                fontFamily = AmiriFontFamily,
                                fontSize = 20.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(text = ayah.banglaTranslation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            return
        }

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("সূরা (১১৪)") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("জুজ/পারা (৩০)") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("বুকমার্ক ও নোট") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(surahs, key = { it.id }) { surah ->
                        SurahListRow(
                            surah = surah,
                            onClick = { onOpenSurah(surah.id) },
                            onDownload = { viewModel.downloadSurahOffline(surah.id) },
                            onPlay = {
                                viewModel.downloadSurahAudioOffline(surah.id, prefs.selectedReciterId)
                            }
                        )
                    }
                }
            }
            1 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..30).toList()) { juz ->
                            FilterChip(
                                selected = selectedJuz == juz,
                                onClick = {
                                    selectedJuz = juz
                                    viewModel.selectJuz(juz)
                                },
                                label = { Text("পারা ${HijriCalendarHelper.toBanglaDigits(juz)}") }
                            )
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val surahsInJuz = surahs.filter { it.juzStart == selectedJuz }
                        items(surahsInJuz, key = { it.id }) { surah ->
                            SurahListRow(
                                surah = surah,
                                onClick = { onOpenSurah(surah.id) },
                                onDownload = { viewModel.downloadSurahOffline(surah.id) },
                                onPlay = { viewModel.downloadSurahAudioOffline(surah.id, prefs.selectedReciterId) }
                            )
                        }
                        items(juzAyahs, key = { it.id }) { ayah ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSurah(ayah.surahId) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "সূরা ${ayah.surahId}:${ayah.ayahNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = ayah.arabicUthmani,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 22.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(text = ayah.banglaTranslation, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                val quranBookmarks = (bookmarksState as? UiState.Success)?.data
                    ?.filter { it.itemType == "QURAN_AYAH" }.orEmpty()
                val notes = (notesState as? UiState.Success)?.data.orEmpty()
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("সংরক্ষিত আয়াত (${quranBookmarks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(quranBookmarks, key = { it.id }) { bm ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    bm.refId.substringBefore(":").toIntOrNull()?.let(onOpenSurah)
                                },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(bm.titleBangla, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                if (!bm.excerpt.isNullOrBlank()) Text(bm.excerpt, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ব্যক্তিগত আয়াত নোট (${notes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(notes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSurah(note.surahId.toInt()) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("${note.surahNameBangla} (${note.surahId}:${note.ayahNumber})", fontWeight = FontWeight.SemiBold)
                                Text(note.noteText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahListRow(
    surah: SurahEntity,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("surah_item_${surah.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = HijriCalendarHelper.toBanglaDigits(surah.id),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${surah.nameBangla} (${surah.meaningBangla})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${surah.revelationPlace} • ${HijriCalendarHelper.toBanglaDigits(surah.ayahCount)} আয়াত • পারা ${HijriCalendarHelper.toBanglaDigits(surah.juzStart)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.nameArabic,
                    fontFamily = AmiriFontFamily,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (surah.isTextDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                            contentDescription = "অফলাইন টেক্সট ডাউনলোড",
                            tint = if (surah.isTextDownloaded) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "অডিও ডাউনলোড",
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahId: Int,
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    LaunchedEffect(surahId) {
        viewModel.openSurah(surahId)
    }

    val context = LocalContext.current
    val surah by viewModel.selectedSurah.collectAsStateWithLifecycle()
    val ayahs by viewModel.ayahsForSelectedSurah.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val notesState by viewModel.ayahNotesState.collectAsStateWithLifecycle()
    val audioState by AudioPlaybackController.state.collectAsStateWithLifecycle()

    val bookmarkedSet = remember(bookmarksState) {
        (bookmarksState as? UiState.Success)?.data?.map { it.id }?.toSet() ?: emptySet()
    }
    val notesMap = remember(notesState) {
        (notesState as? UiState.Success)?.data?.associateBy { "${it.surahId}_${it.ayahNumber}" } ?: emptyMap()
    }

    var showSettingsSheet by remember { mutableStateOf(false) }
    var noteTargetAyah by remember { mutableStateOf<AyahEntity?>(null) }
    var noteDraft by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val nightBg = if (prefs.nightReadingMode) Color(0xFF07110D) else MaterialTheme.colorScheme.background
    val nightCard = if (prefs.nightReadingMode) Color(0xFF102019) else MaterialTheme.colorScheme.surface
    val nightText = if (prefs.nightReadingMode) Color(0xFFE5EFEA) else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(nightBg)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(surah?.nameBangla ?: "সূরা $surahId", fontWeight = FontWeight.Bold)
                    Text(
                        text = "${surah?.revelationPlace ?: ""} • ${surah?.ayahCount ?: 0} আয়াত",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("quran_reader_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        val next = if (prefs.quranLayoutMode == QuranLayoutMode.STACKED)
                            QuranLayoutMode.SIDE_BY_SIDE else QuranLayoutMode.STACKED
                        viewModel.toggleLayoutMode(next)
                    }
                ) {
                    Icon(Icons.Rounded.ViewColumn, contentDescription = "পাশাপাশি মোড")
                }
                IconButton(onClick = { viewModel.toggleNightMode(!prefs.nightReadingMode) }) {
                    Icon(
                        Icons.Rounded.NightlightRound,
                        contentDescription = "নাইট মোড",
                        tint = if (prefs.nightReadingMode) GoldAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = "ফন্ট ও ক্বারী সেটিংস")
                }
            }
        )

        if (isDownloading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        SnackbarHost(hostState = snackbarHostState)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 820.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ayahs.isEmpty() && !isDownloading) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("এই সূরার সম্পূর্ণ টেক্সট অফলাইনে ডাউনলোড করা হয়নি।")
                            Button(onClick = { viewModel.downloadSurahOffline(surahId) }) {
                                Text("এখনই ডাউনলোড করুন")
                            }
                        }
                    }
                }
            }

            items(ayahs, key = { it.id }) { ayah ->
                var tafsirExpanded by remember { mutableStateOf(false) }
                val isBookmarked = "quran_${ayah.surahId}_${ayah.ayahNumber}" in bookmarkedSet
                val existingNote = notesMap["${ayah.surahId}_${ayah.ayahNumber}"]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = nightCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${surahId}:${ayah.ayahNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    val reciter = SeedDataSurahs.reciters.firstOrNull { it.id == prefs.selectedReciterId }
                                        ?: SeedDataSurahs.reciters.first()
                                    AudioPlaybackController.play(
                                        context = context,
                                        sourceUrlOrPath = NetworkModule.ayahAudioUrl(reciter.id, ayah.audioNumberGlobal),
                                        surahId = surahId,
                                        surahNameBangla = "${surah?.nameBangla ?: "সূরা $surahId"} • আয়াত ${ayah.ayahNumber}",
                                        reciterNameBangla = reciter.nameBangla,
                                        ayahNumber = ayah.ayahNumber,
                                        speed = prefs.playbackSpeed
                                    )
                                }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "আয়াত শুনুন", tint = EmeraldPrimary)
                                }
                                IconButton(onClick = {
                                    viewModel.toggleBookmarkAyah(surah?.nameBangla ?: "সূরা $surahId", ayah, isBookmarked)
                                }) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = "বুকমার্ক",
                                        tint = if (isBookmarked) GoldAccent else nightText
                                    )
                                }
                                IconButton(onClick = {
                                    noteDraft = existingNote?.noteText ?: ""
                                    noteTargetAyah = ayah
                                }) {
                                    Icon(Icons.Rounded.EditNote, contentDescription = "নোট লিখুন", tint = nightText)
                                }
                                IconButton(onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${ayah.arabicUthmani}\n\n${ayah.banglaTranslation}\n— ${surah?.nameBangla ?: ""} (${surahId}:${ayah.ayahNumber})"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "আয়াত শেয়ার করুন"))
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "শেয়ার", tint = nightText)
                                }
                            }
                        }

                        if (prefs.quranLayoutMode == QuranLayoutMode.SIDE_BY_SIDE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = ayah.banglaTranslation,
                                    fontSize = prefs.banglaFontSizeSp.sp,
                                    lineHeight = (prefs.banglaFontSizeSp * 1.55f).sp,
                                    color = nightText,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = ayah.arabicUthmani,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = prefs.arabicFontSizeSp.sp,
                                    lineHeight = (prefs.arabicFontSizeSp * 1.7f).sp,
                                    textAlign = TextAlign.End,
                                    color = nightText,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Text(
                                text = ayah.arabicUthmani,
                                fontFamily = AmiriFontFamily,
                                fontSize = prefs.arabicFontSizeSp.sp,
                                lineHeight = (prefs.arabicFontSizeSp * 1.7f).sp,
                                textAlign = TextAlign.End,
                                color = nightText,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (prefs.showTransliteration && ayah.banglaPronunciation.isNotBlank()) {
                                Text(
                                    text = ayah.banglaPronunciation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = ayah.banglaTranslation,
                                fontSize = prefs.banglaFontSizeSp.sp,
                                lineHeight = (prefs.banglaFontSizeSp * 1.55f).sp,
                                color = nightText
                            )
                        }

                        if (existingNote != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "আপনার নোট: ${existingNote.noteText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = { tafsirExpanded = !tafsirExpanded },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (tafsirExpanded) "তাফসীর লুকান" else "তাফসীরে মাআরেফুল কুরআন ও ইবনে কাছির পড়ুন")
                            Icon(
                                imageVector = if (tafsirExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(visible = tafsirExpanded) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ayah.tafsirBangla,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = nightText,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Persistent Surah Audio Bar
        Surface(
            color = ForestDeep,
            contentColor = Color.White,
            tonalElevation = 6.dp
        ) {
            val reciter = SeedDataSurahs.reciters.firstOrNull { it.id == prefs.selectedReciterId }
                ?: SeedDataSurahs.reciters.first()
            val localAudioFile = remember(reciter.id, surahId) {
                File(context.filesDir, "quran_audio/${reciter.id}_$surahId.mp3")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (audioState.surahId == surahId && (audioState.isPlaying || audioState.isLoading)) {
                            AudioPlaybackController.togglePlayPause(context)
                        } else {
                            val src = if (localAudioFile.exists()) localAudioFile.absolutePath
                            else NetworkModule.surahAudioUrl(reciter.id, surahId)
                            AudioPlaybackController.play(
                                context = context,
                                sourceUrlOrPath = src,
                                surahId = surahId,
                                surahNameBangla = surah?.nameBangla ?: "সূরা $surahId",
                                reciterNameBangla = reciter.nameBangla,
                                speed = prefs.playbackSpeed,
                                isOfflineFile = localAudioFile.exists()
                            )
                        }
                    },
                    modifier = Modifier.testTag("surah_audio_play_pause")
                ) {
                    Icon(
                        imageVector = if (audioState.isPlaying && audioState.surahId == surahId)
                            Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "সম্পূর্ণ সূরা প্লে/পজ",
                        tint = GoldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reciter.nameBangla,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = if (audioState.errorMessage != null) audioState.errorMessage!!
                        else if (localAudioFile.exists()) "অফলাইন অডিও প্রস্তুত"
                        else "অনলাইন স্ট্রিমিং / ডাউনলোডযোগ্য",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
                TextButton(onClick = {
                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                    val next = speeds[(speeds.indexOf(prefs.playbackSpeed) + 1) % speeds.size]
                    viewModel.setPlaybackSpeed(next)
                    AudioPlaybackController.setSpeed(context, next)
                }) {
                    Icon(Icons.Rounded.Speed, contentDescription = "স্পিড", tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(" ${prefs.playbackSpeed}x", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { AudioPlaybackController.toggleRepeat(context) }) {
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = "রিপিট",
                        tint = if (audioState.isRepeatEnabled) GoldAccent else Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = { viewModel.downloadSurahAudioOffline(surahId, reciter.id) }) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = "অডিও অফলাইন ডাউনলোড",
                        tint = if (localAudioFile.exists()) GoldAccent else Color.White
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("রিডার ও ক্বারী সেটিংস", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("ক্বারী নির্বাচন করুন:", style = MaterialTheme.typography.labelLarge)
                SeedDataSurahs.reciters.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setReciter(r.id) }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(r.nameBangla, fontWeight = FontWeight.Medium)
                            Text(r.styleBangla, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (prefs.selectedReciterId == r.id) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "নির্বাচিত", tint = EmeraldPrimary)
                        }
                    }
                }
                HorizontalDivider()
                Text("আরবি ফন্ট সাইজ: ${prefs.arabicFontSizeSp.toInt()}sp")
                Slider(
                    value = prefs.arabicFontSizeSp,
                    onValueChange = { viewModel.setFontSizes(it, prefs.banglaFontSizeSp) },
                    valueRange = 20f..42f
                )
                Text("বাংলা ফন্ট সাইজ: ${prefs.banglaFontSizeSp.toInt()}sp")
                Slider(
                    value = prefs.banglaFontSizeSp,
                    onValueChange = { viewModel.setFontSizes(prefs.arabicFontSizeSp, it) },
                    valueRange = 13f..24f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("বাংলা উচ্চারণ দেখান")
                    Switch(checked = prefs.showTransliteration, onCheckedChange = viewModel::toggleTransliteration)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (noteTargetAyah != null) {
        AlertDialog(
            onDismissRequest = { noteTargetAyah = null },
            title = { Text("আয়াত ${noteTargetAyah!!.surahId}:${noteTargetAyah!!.ayahNumber} নোট") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text("আপনার ব্যক্তিগত শিক্ষা বা নোট") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = noteTargetAyah!!
                    if (noteDraft.isNotBlank()) {
                        viewModel.saveAyahNote(target.surahId, target.ayahNumber, surah?.nameBangla ?: "সূরা", noteDraft)
                    }
                    noteTargetAyah = null
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                TextButton(onClick = { noteTargetAyah = null }) { Text("বাতিল") }
            }
        )
    }
}
