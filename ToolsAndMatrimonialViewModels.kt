package com.example.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookmarkEntity
import com.example.data.local.DuaEntity
import com.example.data.local.LocalUserDataRepository
import com.example.data.local.MatrimonialMessageEntity
import com.example.data.local.MatrimonialProfileEntity
import com.example.data.local.MatrimonialRequestEntity
import com.example.data.local.MuslimBanglaDao
import com.example.data.local.PrayerScheduleCacheEntity
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPrefsState
import com.example.data.remote.MatrimonialFirestoreRepository
import com.example.domain.islamic.DailyPrayerSchedule
import com.example.domain.islamic.PrayerTimeCalculator
import com.google.firebase.firestore.FirebaseFirestoreException
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

internal fun <T> Flow<T>.asFirestoreUiState(tag: String): Flow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(tag, "Firestore flow error", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "ম্যাট্রিমোনিয়াল তথ্য লোড করতে সমস্যা হয়েছে"))
        }

class ToolsViewModel(
    dao: MuslimBanglaDao,
    private val prefsRepo: UserPreferencesRepository,
    private val localUserRepo: LocalUserDataRepository
) : ViewModel() {

    val prefs: StateFlow<UserPrefsState> = prefsRepo.preferencesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        UserPrefsState()
    )

    val duas: StateFlow<List<DuaEntity>> = dao.observeAllDuas().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    val quizAttemptsState: StateFlow<UiState<List<QuizAttemptEntity>>> =
        localUserRepo.observeQuizAttempts()
            .asUiState("ToolsVM")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val bookmarksState: StateFlow<UiState<List<BookmarkEntity>>> =
        localUserRepo.observeBookmarks()
            .asUiState("ToolsVM")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val cachedPrayerSchedule: StateFlow<PrayerScheduleCacheEntity?> =
        localUserRepo.observeCachedPrayerSchedule()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun clearFeedback() { _feedback.value = null }
    fun postFeedback(message: String) { _feedback.value = message }

    fun incrementTasbih(currentCount: Int, target: Int) = viewModelScope.launch {
        val next = currentCount + 1
        prefsRepo.updateTasbih(next, target)
    }

    fun resetTasbih(target: Int) = viewModelScope.launch {
        prefsRepo.updateTasbih(0, target)
    }

    fun setTasbihTarget(currentCount: Int, target: Int) = viewModelScope.launch {
        prefsRepo.updateTasbih(currentCount, target)
    }

    fun toggleBookmarkDua(dua: DuaEntity, alreadyBookmarked: Boolean) = viewModelScope.launch {
        val id = "dua_${dua.id}"
        val res = if (alreadyBookmarked) {
            localUserRepo.removeBookmark(id)
        } else {
            localUserRepo.addBookmark(
                bookmarkId = id,
                itemType = "DUA",
                refId = dua.id,
                titleBangla = dua.titleBangla,
                subtitle = dua.reference,
                excerpt = dua.banglaMeaning
            )
        }
        _feedback.value = if (res.isSuccess) {
            if (alreadyBookmarked) "দুআ বুকমার্ক সরানো হয়েছে।" else "দুআ লোকাল ডাটাবেসে বুকমার্ক করা হয়েছে।"
        } else "বুকমার্ক ব্যর্থ হয়েছে।"
    }

    fun saveQuizAttempt(category: String, score: Int, total: Int) = viewModelScope.launch {
        val res = localUserRepo.saveQuizAttempt(category, score, total)
        _feedback.value = if (res.isSuccess) "কুইজের ফলাফল লোকাল ডাটাবেসে সংরক্ষিত হয়েছে।" else "ফলাফল সেভ করা যায়নি।"
    }

    fun setLocation(lat: Double, lng: Double, city: String) = viewModelScope.launch {
        prefsRepo.setLocation(lat, lng, city)
        val currentMethod = prefs.value.prayerCalcMethod
        val schedule = PrayerTimeCalculator.calculateForToday(lat, lng, currentMethod)
        localUserRepo.cachePrayerSchedule(city, lat, lng, currentMethod.titleBangla, schedule)
        _feedback.value = "লোকেশন ও নামাজের সময়সূচি সেট হয়েছে: $city"
    }

    fun syncPrayerScheduleCache(
        city: String,
        lat: Double,
        lng: Double,
        methodName: String,
        schedule: DailyPrayerSchedule
    ) = viewModelScope.launch {
        localUserRepo.cachePrayerSchedule(city, lat, lng, methodName, schedule)
    }

    fun setAdhanEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setAdhanNotifications(enabled)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MatrimonialViewModel(
    private val repo: MatrimonialFirestoreRepository,
    val currentUserId: String,
    prefsRepo: UserPreferencesRepository
) : ViewModel() {

    val prefs: StateFlow<UserPrefsState> = prefsRepo.preferencesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        UserPrefsState()
    )

    val activeProfilesState: StateFlow<UiState<List<MatrimonialProfileEntity>>> =
        repo.observeActiveProfiles(currentUserId)
            .asFirestoreUiState("MatrimonialVM")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myProfileState: StateFlow<UiState<MatrimonialProfileEntity?>> =
        repo.observeMyProfile(currentUserId)
            .asFirestoreUiState("MatrimonialVM")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val requestsState: StateFlow<UiState<List<MatrimonialRequestEntity>>> =
        repo.observeMyRequests(currentUserId)
            .asFirestoreUiState("MatrimonialVM")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _activeChatRequestId = MutableStateFlow<String?>(null)
    val activeMessagesState: StateFlow<UiState<List<MatrimonialMessageEntity>>> =
        _activeChatRequestId.flatMapLatest { reqId ->
            if (reqId.isNullOrBlank()) flowOf(UiState.Success(emptyList()))
            else repo.observeMessages(reqId, currentUserId).asFirestoreUiState("MatrimonialChatVM")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun clearFeedback() { _feedback.value = null }
    fun openChat(requestId: String) { _activeChatRequestId.value = requestId }

    fun saveMyProfile(profile: MatrimonialProfileEntity) = viewModelScope.launch {
        val res = repo.upsertProfile(profile)
        _feedback.value = res.fold(
            onSuccess = { "আপনার ম্যাট্রিমোনিয়াল বায়োডাটা ক্লাউডে সংরক্ষিত ও প্রকাশিত হয়েছে।" },
            onFailure = { "বায়োডাটা সেভ করা যায়নি: ${it.localizedMessage ?: "অনুগ্রহ করে আবার চেষ্টা করুন"}" }
        )
    }

    fun sendInterestRequest(
        receiverProfile: MatrimonialProfileEntity,
        senderLabel: String,
        introMessage: String
    ) = viewModelScope.launch {
        val res = repo.sendInterestRequest(
            receiverId = receiverProfile.userId,
            senderName = senderLabel.ifBlank { prefs.value.localDisplayName },
            receiverName = receiverProfile.profileCode,
            introMessage = introMessage
        )
        _feedback.value = res.fold(
            onSuccess = { "আগ্রহ ও প্রস্তাব নিরাপদে পাঠানো হয়েছে।" },
            onFailure = { "প্রস্তাব পাঠানো যায়নি: ${it.localizedMessage ?: "অনুগ্রহ করে আবার চেষ্টা করুন"}" }
        )
    }

    fun respondToRequest(requestId: String, accept: Boolean, approvePhoto: Boolean) = viewModelScope.launch {
        val res = repo.respondToRequest(requestId, accept, approvePhoto)
        _feedback.value = res.fold(
            onSuccess = { if (accept) "প্রস্তাব গৃহীত হয়েছে—এখন শালীন মেসেজিং চালু আছে।" else "প্রস্তাব প্রত্যাখ্যান করা হয়েছে।" },
            onFailure = { "প্রতিক্রিয়া আপডেট ব্যর্থ হয়েছে।" }
        )
    }

    fun togglePhotoReveal(requestId: String, approved: Boolean) = viewModelScope.launch {
        val res = repo.togglePhotoReveal(requestId, approved)
        _feedback.value = if (res.isSuccess) {
            if (approved) "এই প্রস্তাবের জন্য ছবি দেখার অনুমতি চালু হয়েছে।" else "ছবি পুনরায় ঝাপসা করা হয়েছে।"
        } else "অনুমতি পরিবর্তন ব্যর্থ হয়েছে।"
    }

    fun sendMessage(requestId: String, text: String) = viewModelScope.launch {
        val res = repo.sendMessage(requestId, text)
        if (res.isFailure) {
            _feedback.value = "বার্তা পাঠানো যায়নি (অনুমোদিত প্রস্তাব নিশ্চিত করুন)।"
        }
    }
}
