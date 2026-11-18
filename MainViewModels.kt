package com.example.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppLanguage
import com.example.data.local.AyahEntity
import com.example.data.local.AyahNoteEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.HadithBookEntity
import com.example.data.local.HadithEntity
import com.example.data.local.LocalUserDataRepository
import com.example.data.local.MuslimBanglaDao
import com.example.data.local.NarratorGradeEntity
import com.example.data.local.OfflineAudioEntity
import com.example.data.local.PrayerCalcMethod
import com.example.data.local.QuranLayoutMode
import com.example.data.local.SeedDataSurahs
import com.example.data.local.SurahEntity
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPrefsState
import com.example.data.remote.OfflineContentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

internal fun <T> Flow<T>.asUiState(tag: String): Flow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(tag, "Flow error", error)
            emit(UiState.Error(error.localizedMessage ?: "ডেটা লোড করতে সমস্যা হয়েছে"))
        }

class HomeSettingsViewModel(
    private val prefsRepo: UserPreferencesRepository,
    private val localUserRepo: LocalUserDataRepository,
    dao: MuslimBanglaDao,
    private val offlineRepo: OfflineContentRepository
) : ViewModel() {

    val prefsState: StateFlow<UserPrefsState> = prefsRepo.preferencesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        UserPrefsState()
    )

    val offlineAudios: StateFlow<List<OfflineAudioEntity>> = dao.observeAllOfflineAudio().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    val surahs: StateFlow<List<SurahEntity>> = dao.observeAllSurahs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    val hadithBooks: StateFlow<List<HadithBookEntity>> = dao.observeHadithBooks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBulkDownloading = MutableStateFlow(false)
    val isBulkDownloading: StateFlow<Boolean> = _isBulkDownloading.asStateFlow()

    fun clearStatus() { _statusMessage.value = null }
    fun postStatus(message: String) { _statusMessage.value = message }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setNightReading(enabled: Boolean) = viewModelScope.launch { prefsRepo.setNightReadingMode(enabled) }
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { prefsRepo.setAppLanguage(lang) }
    fun setFontSizes(ar: Float, bn: Float) = viewModelScope.launch { prefsRepo.setFontSizes(ar, bn) }
    fun setPrayerMethod(method: PrayerCalcMethod) = viewModelScope.launch { prefsRepo.setPrayerMethod(method) }
    fun setAdhanEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAdhanNotifications(enabled) }
    fun setDailyReminder(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDailyReminder(enabled) }
    fun updateLocalDisplayName(name: String) = viewModelScope.launch {
        localUserRepo.updateLocalDisplayName(name)
        _statusMessage.value = "স্থানীয় প্রোফাইলের নাম সংরক্ষিত হয়েছে।"
    }

    fun setLocation(lat: Double, lng: Double, city: String) = viewModelScope.launch {
        prefsRepo.setLocation(lat, lng, city)
        val method = prefsState.value.prayerCalcMethod
        val schedule = com.example.domain.islamic.PrayerTimeCalculator.calculateForToday(lat, lng, method)
        localUserRepo.cachePrayerSchedule(city, lat, lng, method.titleBangla, schedule)
        _statusMessage.value = "লোকেশন ও নামাজের সময়সূচি আপডেট হয়েছে: $city"
    }

    fun downloadStarterOfflinePack() = viewModelScope.launch {
        if (_isBulkDownloading.value) return@launch
        _isBulkDownloading.value = true
        _statusMessage.value = "গুরুত্বপূর্ণ সূরাসমূহ ও তাফসীর ডাউনলোড হচ্ছে..."
        var successCount = 0
        listOf(1, 36, 55, 67, 103, 108, 112, 113, 114).forEach { id ->
            if (offlineRepo.ensureSurahLoaded(id).isSuccess) successCount++
        }
        _isBulkDownloading.value = false
        _statusMessage.value = "$successCount টি গুরুত্বপূর্ণ সূরা ও তাফসীর অফলাইনের জন্য সংরক্ষিত হয়েছে।"
    }

    fun downloadAll114SurahsOffline() = viewModelScope.launch {
        if (_isBulkDownloading.value) return@launch
        _isBulkDownloading.value = true
        _statusMessage.value = "সম্পূর্ণ ১১৪ সূরা ও তাফসীর ডাউনলোড শুরু হয়েছে..."
        var successCount = 0
        for (surahId in 1..114) {
            if (offlineRepo.ensureSurahLoaded(surahId).isSuccess) {
                successCount++
            }
        }
        _isBulkDownloading.value = false
        _statusMessage.value = "$successCount / ১১৪ টি সূরা (তাফসীরে মাআরেফুল কুরআন ও ইবনে কাছিরসহ) অফলাইনে সংরক্ষিত হয়েছে।"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModel(
    private val dao: MuslimBanglaDao,
    private val offlineRepo: OfflineContentRepository,
    private val localUserRepo: LocalUserDataRepository,
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    val allSurahs: StateFlow<List<SurahEntity>> = dao.observeAllSurahs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    val prefs: StateFlow<UserPrefsState> = prefsRepo.preferencesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        UserPrefsState()
    )

    val bookmarksState: StateFlow<UiState<List<BookmarkEntity>>> = localUserRepo.observeBookmarks()
        .asUiState("QuranVM")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val ayahNotesState: StateFlow<UiState<List<AyahNoteEntity>>> = localUserRepo.observeAyahNotes()
        .asUiState("QuranVM")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<AyahEntity>> = _searchQuery.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else dao.searchAyahs(q.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedSurahId = MutableStateFlow(1)
    val selectedSurah: StateFlow<SurahEntity?> = _selectedSurahId.flatMapLatest { id ->
        dao.observeSurah(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val ayahsForSelectedSurah: StateFlow<List<AyahEntity>> = _selectedSurahId.flatMapLatest { id ->
        dao.observeAyahsBySurah(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedJuz = MutableStateFlow(30)
    val ayahsForSelectedJuz: StateFlow<List<AyahEntity>> = _selectedJuz.flatMapLatest { juz ->
        dao.observeAyahsByJuz(juz)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun updateSearch(q: String) { _searchQuery.value = q }
    fun clearFeedback() { _feedback.value = null }
    fun selectJuz(juz: Int) { _selectedJuz.value = juz }

    fun openSurah(surahId: Int) {
        _selectedSurahId.value = surahId
        viewModelScope.launch {
            _isDownloading.value = true
            offlineRepo.ensureSurahLoaded(surahId)
            _isDownloading.value = false
        }
    }

    fun downloadSurahOffline(surahId: Int) = viewModelScope.launch {
        _isDownloading.value = true
        val res = offlineRepo.ensureSurahLoaded(surahId)
        _isDownloading.value = false
        _feedback.value = if (res.isSuccess) "সূরা অফলাইনে সংরক্ষিত হয়েছে।" else "ডাউনলোডে সমস্যা হয়েছে (অফলাইন/নেটওয়ার্ক চেক করুন)।"
    }

    fun downloadSurahAudioOffline(surahId: Int, reciterId: String) = viewModelScope.launch {
        _isDownloading.value = true
        val reciterName = SeedDataSurahs.reciters.firstOrNull { it.id == reciterId }?.nameBangla ?: reciterId
        val res = offlineRepo.downloadSurahAudio(reciterId, surahId)
        _isDownloading.value = false
        _feedback.value = if (res.isSuccess) "$reciterName - অডিও অফলাইনে ডাউনলোড সম্পন্ন।" else "অডিও ডাউনলোড ব্যর্থ হয়েছে।"
    }

    fun toggleBookmarkAyah(surahName: String, ayah: AyahEntity, alreadyBookmarked: Boolean) = viewModelScope.launch {
        val id = "quran_${ayah.surahId}_${ayah.ayahNumber}"
        val res = if (alreadyBookmarked) {
            localUserRepo.removeBookmark(id)
        } else {
            localUserRepo.addBookmark(
                bookmarkId = id,
                itemType = "QURAN_AYAH",
                refId = "${ayah.surahId}:${ayah.ayahNumber}",
                titleBangla = "$surahName (${ayah.surahId}:${ayah.ayahNumber})",
                subtitle = ayah.arabicUthmani.take(120),
                excerpt = ayah.banglaTranslation
            )
        }
        _feedback.value = if (res.isSuccess) {
            if (alreadyBookmarked) "বুকমার্ক সরানো হয়েছে।" else "আয়াত লোকাল ডাটাবেসে বুকমার্ক করা হয়েছে।"
        } else "বুকমার্ক আপডেট ব্যর্থ হয়েছে।"
    }

    fun saveAyahNote(surahId: Int, ayahNumber: Int, surahName: String, noteText: String) = viewModelScope.launch {
        val res = localUserRepo.saveAyahNote(surahId, ayahNumber, surahName, noteText)
        _feedback.value = if (res.isSuccess) "আয়াত নোট লোকাল ডাটাবেসে সংরক্ষিত হয়েছে।" else "নোট সেভ করা যায়নি।"
    }

    fun setReciter(id: String) = viewModelScope.launch { prefsRepo.setSelectedReciter(id) }
    fun setPlaybackSpeed(speed: Float) = viewModelScope.launch { prefsRepo.setPlaybackSpeed(speed) }
    fun toggleTransliteration(show: Boolean) = viewModelScope.launch { prefsRepo.setShowTransliteration(show) }
    fun toggleLayoutMode(mode: QuranLayoutMode) = viewModelScope.launch { prefsRepo.setQuranLayoutMode(mode) }
    fun toggleNightMode(enabled: Boolean) = viewModelScope.launch { prefsRepo.setNightReadingMode(enabled) }
    fun setFontSizes(ar: Float, bn: Float) = viewModelScope.launch { prefsRepo.setFontSizes(ar, bn) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HadithViewModel(
    private val dao: MuslimBanglaDao,
    private val offlineRepo: OfflineContentRepository,
    private val localUserRepo: LocalUserDataRepository
) : ViewModel() {

    val books: StateFlow<List<HadithBookEntity>> = dao.observeHadithBooks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    private val _selectedBookId = MutableStateFlow("bukhari")
    val hadithsInBook: StateFlow<List<HadithEntity>> = _selectedBookId.flatMapLatest { id ->
        dao.observeHadithsByBook(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedHadithId = MutableStateFlow("bukhari_1")
    val selectedHadith: StateFlow<HadithEntity?> = _selectedHadithId.flatMapLatest { id ->
        dao.observeHadithById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val narratorGrades: StateFlow<List<NarratorGradeEntity>> = _selectedHadithId.flatMapLatest { id ->
        dao.observeNarratorsForHadith(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<HadithEntity>> = _searchQuery.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else dao.searchHadiths(q.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val bookmarksState: StateFlow<UiState<List<BookmarkEntity>>> = localUserRepo.observeBookmarks()
        .asUiState("HadithVM")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun selectBook(bookId: String) { _selectedBookId.value = bookId }
    fun selectHadith(hadithId: String) { _selectedHadithId.value = hadithId }
    fun updateSearch(q: String) { _searchQuery.value = q }
    fun clearFeedback() { _feedback.value = null }

    fun downloadBook(book: HadithBookEntity) = viewModelScope.launch {
        _isDownloading.value = true
        val res = offlineRepo.downloadHadithBook(book.id, book.apiEditionBangla, book.apiEditionArabic)
        _isDownloading.value = false
        _feedback.value = res.fold(
            onSuccess = { count ->
                if (count > 0) "${book.titleBangla}: $count টি হাদিস অফলাইনে ডাউনলোড হয়েছে।"
                else "${book.titleBangla} অফলাইনের জন্য প্রস্তুত।"
            },
            onFailure = { "হাদিস ডাউনলোড ব্যর্থ হয়েছে (অফলাইন সংযোগ চেক করুন)।" }
        )
    }

    fun toggleBookmarkHadith(hadith: HadithEntity, bookTitle: String, alreadyBookmarked: Boolean) = viewModelScope.launch {
        val bId = "hadith_${hadith.id}"
        val res = if (alreadyBookmarked) {
            localUserRepo.removeBookmark(bId)
        } else {
            localUserRepo.addBookmark(
                bookmarkId = bId,
                itemType = "HADITH",
                refId = "${hadith.bookId}:${hadith.hadithNumber}",
                titleBangla = "$bookTitle #${hadith.hadithNumber}",
                subtitle = hadith.gradeLabel,
                excerpt = hadith.banglaTranslation
            )
        }
        _feedback.value = if (res.isSuccess) {
            if (alreadyBookmarked) "বুকমার্ক সরানো হয়েছে।" else "হাদিস লোকাল ডাটাবেসে বুকমার্ক করা হয়েছে।"
        } else "বুকমার্ক ব্যর্থ হয়েছে।"
    }
}
