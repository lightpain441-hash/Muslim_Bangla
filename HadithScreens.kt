package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.HadithBookEntity
import com.example.data.local.HadithEntity
import com.example.data.local.NarratorGradeEntity
import com.example.domain.islamic.HijriCalendarHelper
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodels.HadithViewModel
import com.example.ui.viewmodels.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithHubScreen(
    viewModel: HadithViewModel,
    onOpenBook: (String) -> Unit,
    onOpenHadith: (String) -> Unit
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("সহীহ হাদিস সংগ্রহ (১০ গ্রন্থ)", fontWeight = FontWeight.Bold) }
        )
        if (isDownloading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        SnackbarHost(hostState = snackbarHostState)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearch,
            placeholder = { Text("হাদিস, অধ্যায় বা রাবীর নাম খুঁজুন...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "খুঁজুন") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("hadith_search_input")
        )

        if (searchQuery.isNotBlank()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(searchResults, key = { it.id }) { h ->
                    HadithSummaryCard(h, onClick = { onOpenHadith(h.id) })
                }
            }
            return
        }

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("হাদিস গ্রন্থ (১০টি)") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("সংরক্ষিত হাদিস") })
        }

        if (selectedTab == 0) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(books, key = { it.id }) { book ->
                    HadithBookCard(
                        book = book,
                        onOpen = {
                            viewModel.selectBook(book.id)
                            onOpenBook(book.id)
                        },
                        onDownload = { viewModel.downloadBook(book) }
                    )
                }
            }
        } else {
            val saved = (bookmarksState as? UiState.Success)?.data
                ?.filter { it.itemType == "HADITH" }.orEmpty()
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (saved.isEmpty()) {
                    item {
                        Text(
                            text = "এখনো কোনো হাদিস বুকমার্ক করা হয়নি।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(saved, key = { it.id }) { bm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenHadith(bm.id.removePrefix("hadith_")) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(bm.titleBangla, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            if (!bm.subtitle.isNullOrBlank()) Text(bm.subtitle, style = MaterialTheme.typography.labelSmall)
                            if (!bm.excerpt.isNullOrBlank()) Text(bm.excerpt, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithBookCard(
    book: HadithBookEntity,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("hadith_book_${book.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.titleBangla, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(book.compilerBangla, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "মোট ${HijriCalendarHelper.toBanglaDigits(book.totalHadithCount)} হাদিস",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = book.titleArabic,
                    fontFamily = AmiriFontFamily,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = if (book.isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                        contentDescription = "অফলাইন ডাউনলোড",
                        tint = if (book.isDownloaded) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithBookListScreen(
    bookId: String,
    viewModel: HadithViewModel,
    onBack: () -> Unit,
    onOpenHadith: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(bookId) { viewModel.selectBook(bookId) }

    val books by viewModel.books.collectAsStateWithLifecycle()
    val hadiths by viewModel.hadithsInBook.collectAsStateWithLifecycle()
    val book = books.firstOrNull { it.id == bookId }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(book?.titleBangla ?: "হাদিস তালিকা", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান")
                }
            },
            actions = {
                if (book != null) {
                    IconButton(onClick = { viewModel.downloadBook(book) }) {
                        Icon(Icons.Rounded.Download, contentDescription = "আরও হাদিস ডাউনলোড")
                    }
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(hadiths, key = { it.id }) { hadith ->
                HadithSummaryCard(hadith = hadith, onClick = { onOpenHadith(hadith.id) })
            }
        }
    }
}

@Composable
private fun HadithSummaryCard(hadith: HadithEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "হাদিস #${HijriCalendarHelper.toBanglaDigits(hadith.hadithNumber)} • ${hadith.chapterBangla}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = hadith.gradeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (hadith.arabicText.isNotBlank()) {
                Text(
                    text = hadith.arabicText,
                    fontFamily = AmiriFontFamily,
                    fontSize = 20.sp,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = hadith.banglaTranslation,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "বিস্তারিত সনদ ও জারহ-তাদীল দেখুন →",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithDetailScreen(
    hadithId: String,
    viewModel: HadithViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(hadithId) { viewModel.selectHadith(hadithId) }

    val hadith by viewModel.selectedHadith.collectAsStateWithLifecycle()
    val narrators by viewModel.narratorGrades.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val bookTitle = books.firstOrNull { it.id == hadith?.bookId }?.titleBangla ?: "হাদিস"
    val isBookmarked = remember(bookmarksState, hadithId) {
        (bookmarksState as? UiState.Success)?.data?.any { it.id == "hadith_$hadithId" } == true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("$bookTitle #${hadith?.hadithNumber ?: ""}", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান")
                }
            },
            actions = {
                val h = hadith
                if (h != null) {
                    IconButton(onClick = { viewModel.toggleBookmarkHadith(h, bookTitle, isBookmarked) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "বুকমার্ক",
                            tint = if (isBookmarked) GoldAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString("${h.arabicText}\n\n${h.banglaTranslation}\n— $bookTitle #${h.hadithNumber}"))
                        scope.launch { snackbarHostState.showSnackbar("হাদিস কপি করা হয়েছে") }
                    }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "কপি করুন")
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${h.arabicText}\n\n${h.banglaTranslation}\n— $bookTitle #${h.hadithNumber}")
                        }
                        context.startActivity(Intent.createChooser(intent, "হাদিস শেয়ার করুন"))
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "শেয়ার")
                    }
                }
            }
        )
        SnackbarHost(hostState = snackbarHostState)

        val h = hadith
        if (h == null) {
            BoxLoadingMessage("হাদিস লোড হচ্ছে...")
        } else {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 820.dp)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "অধ্যায়: ${h.chapterBangla}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (h.arabicText.isNotBlank()) {
                                Text(
                                    text = h.arabicText,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 24.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(
                                text = h.banglaTranslation,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Sanad & Grade Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("হাদিসের মান: ${h.gradeLabel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(h.gradeExplanation, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("সনদ (বর্ণনাকারী চেইন):", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(h.sanadChainBangla, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Jarh wa Ta'dil Section
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = EmeraldPrimary)
                        Text(
                            text = "জারহ ও তাদীল (বর্ণনাকারীদের মানদণ্ড বিশ্লেষণ)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(narrators, key = { it.id }) { narrator ->
                    NarratorGradeCard(narrator)
                }
            }
        }
    }
}

@Composable
private fun NarratorGradeCard(n: NarratorGradeEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${HijriCalendarHelper.toBanglaDigits(n.orderInChain)}. ${n.narratorNameBangla}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = n.overallGrade,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (n.narratorNameArabic.isNotBlank()) {
                Text(n.narratorNameArabic, fontFamily = AmiriFontFamily, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            GradeMetricRow("আদল (চরিত্র ও ন্যায়পরায়ণতা)", n.adlStatus)
            GradeMetricRow("দবত (স্মৃতিশক্তি ও সংরক্ষণ)", n.dabtMemory)
            GradeMetricRow("বুদ্ধিমত্তা ও প্রজ্ঞা", n.intellectStatus)
            GradeMetricRow("মানসিক অবস্থা", n.psychologicalState)
            Text(
                text = "মুহাদ্দিসদের মূল্যায়ন: ${n.scholarVerdict}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GradeMetricRow(label: String, value: String) {
    Text(
        text = "• $label: $value",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun BoxLoadingMessage(msg: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(msg)
    }
}
