package com.example.data.local

import com.example.domain.islamic.DailyPrayerSchedule
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocalUserDataRepository(
    private val dao: MuslimBanglaDao,
    private val prefsRepo: UserPreferencesRepository
) {
    val localUserId: String = SeedDataMatrimonial.LOCAL_USER_ID

    // --- BOOKMARKS ---
    fun observeBookmarks(): Flow<List<BookmarkEntity>> = dao.observeBookmarks()

    suspend fun addBookmark(
        bookmarkId: String,
        itemType: String,
        refId: String,
        titleBangla: String,
        subtitle: String?,
        excerpt: String?
    ): Result<Unit> = runCatching {
        dao.upsertBookmark(
            BookmarkEntity(
                id = bookmarkId,
                userId = localUserId,
                itemType = itemType,
                refId = refId.take(100),
                titleBangla = titleBangla.take(200).ifBlank { "সংরক্ষিত" },
                subtitle = subtitle?.take(300),
                excerpt = excerpt?.take(1000),
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeBookmark(bookmarkId: String): Result<Unit> = runCatching {
        dao.deleteBookmarkById(bookmarkId)
    }

    // --- AYAH NOTES ---
    fun observeAyahNotes(): Flow<List<AyahNoteEntity>> = dao.observeAyahNotes()

    suspend fun saveAyahNote(
        surahId: Int,
        ayahNumber: Int,
        surahNameBangla: String,
        noteText: String
    ): Result<Unit> = runCatching {
        val clean = noteText.trim().take(2000)
        require(clean.isNotEmpty()) { "নোট খালি রাখা যাবে না।" }
        val noteId = "note_${surahId}_$ayahNumber"
        val existing = dao.getAyahNoteById(noteId)
        val now = System.currentTimeMillis()
        dao.upsertAyahNote(
            AyahNoteEntity(
                id = noteId,
                userId = localUserId,
                surahId = surahId.toLong(),
                ayahNumber = ayahNumber.toLong(),
                surahNameBangla = surahNameBangla.take(100),
                noteText = clean,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
        )
    }

    // --- QUIZ ATTEMPTS ---
    fun observeQuizAttempts(): Flow<List<QuizAttemptEntity>> = dao.observeQuizAttempts()

    suspend fun saveQuizAttempt(category: String, score: Int, totalQuestions: Int): Result<Unit> = runCatching {
        dao.insertQuizAttempt(
            QuizAttemptEntity(
                userId = localUserId,
                category = category.take(60),
                score = score.toLong(),
                totalQuestions = totalQuestions.toLong(),
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    // --- LOCAL PROFILE / TASBIH ---
    suspend fun updateLocalDisplayName(name: String): Result<Unit> = runCatching {
        val clean = name.trim().take(80).ifBlank { "মুসলিম ব্যবহারকারী" }
        prefsRepo.setLocalDisplayName(clean)
    }

    // --- MATRIMONIAL ---
    fun observeActiveMatrimonialProfiles(): Flow<List<MatrimonialProfileEntity>> =
        dao.observeActiveMatrimonialProfiles()

    fun observeMyMatrimonialProfile(): Flow<MatrimonialProfileEntity?> =
        dao.observeMatrimonialProfileByUser(localUserId)

    suspend fun upsertMatrimonialProfile(profile: MatrimonialProfileEntity): Result<Unit> = runCatching {
        val ageSafe = profile.age.coerceIn(18L, 80L)
        val genderSafe = if (profile.gender == "PATRI") "PATRI" else "PATRO"
        val saved = profile.copy(
            id = localUserId,
            userId = localUserId,
            profileCode = profile.profileCode.ifBlank { "MB-MY01" },
            gender = genderSafe,
            age = ageSafe,
            division = profile.division.trim().take(60).ifBlank { "ঢাকা" },
            education = profile.education.trim().take(120).ifBlank { "স্নাতক" },
            profession = profile.profession.trim().take(120).ifBlank { "পেশাজীবী" },
            practicingNote = profile.practicingNote.trim().take(300).ifBlank { "পাঁচ ওয়াক্ত নামাজ" },
            bioBangla = profile.bioBangla.trim().take(1000).ifBlank { "ইসলামিক জীবনযাপনে আগ্রহী।" },
            waliRelation = profile.waliRelation.trim().take(60).ifBlank { "বাবা" },
            photoUrl = profile.photoUrl?.takeIf { it.isNotBlank() }?.take(500),
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertMatrimonialProfile(saved)
    }

    fun observeMatrimonialRequests(): Flow<List<MatrimonialRequestEntity>> =
        dao.observeMatrimonialRequests(localUserId)

    suspend fun sendMatrimonialRequest(
        receiverId: String,
        senderName: String,
        receiverName: String,
        introMessage: String
    ): Result<Unit> = runCatching {
        require(receiverId != localUserId) { "নিজের প্রোফাইলে প্রস্তাব পাঠানো যাবে না।" }
        val reqId = "${localUserId}_$receiverId"
        val now = System.currentTimeMillis()
        dao.upsertMatrimonialRequest(
            MatrimonialRequestEntity(
                id = reqId,
                senderId = localUserId,
                receiverId = receiverId,
                senderName = senderName.trim().take(100).ifBlank { "আগ্রহী পাত্র/পাত্রী" },
                receiverName = receiverName.trim().take(100).ifBlank { "প্রোফাইল" },
                status = "PENDING",
                introMessage = introMessage.trim().take(400)
                    .ifBlank { "আসসালামু আলাইকুম, অভিভাবকের মাধ্যমে কথা বলতে আগ্রহী।" },
                photoRevealApproved = false,
                waliVerified = true,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    suspend fun respondToMatrimonialRequest(
        requestId: String,
        accept: Boolean,
        approvePhotoReveal: Boolean
    ): Result<Unit> = runCatching {
        val status = if (accept) "ACCEPTED" else "DECLINED"
        val now = System.currentTimeMillis()
        dao.updateMatrimonialRequestResponse(
            requestId = requestId,
            status = status,
            approvePhoto = accept && approvePhotoReveal,
            updatedAt = now
        )
        if (accept) {
            val req = dao.getMatrimonialRequestById(requestId)
            if (req != null) {
                dao.insertMatrimonialMessage(
                    MatrimonialMessageEntity(
                        id = "welcome_${UUID.randomUUID()}",
                        requestId = requestId,
                        senderId = req.senderId,
                        text = "ওয়ালাইকুমুস সালাম। প্রস্তাব গ্রহণের জন্য জাযাকাল্লাহু খাইরান। অভিভাবকের সাথে কথা বলার সময় নির্ধারণ করতে পারেন।",
                        createdAtMillis = now
                    )
                )
            }
        }
    }

    suspend fun togglePhotoReveal(requestId: String, approved: Boolean): Result<Unit> = runCatching {
        dao.updateMatrimonialPhotoReveal(
            requestId = requestId,
            approved = approved,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun observeMatrimonialMessages(requestId: String): Flow<List<MatrimonialMessageEntity>> =
        dao.observeMatrimonialMessages(requestId)

    suspend fun sendMatrimonialMessage(requestId: String, text: String): Result<Unit> = runCatching {
        val req = dao.getMatrimonialRequestById(requestId)
        require(req != null && req.status == "ACCEPTED") { "অনুমোদিত প্রস্তাব ছাড়া বার্তা পাঠানো যাবে না।" }
        val clean = text.trim().take(1000)
        require(clean.isNotEmpty()) { "বার্তা লিখুন।" }
        dao.insertMatrimonialMessage(
            MatrimonialMessageEntity(
                id = UUID.randomUUID().toString(),
                requestId = requestId,
                senderId = localUserId,
                text = clean,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    // --- PRAYER SCHEDULE ROOM CACHE ---
    fun observeCachedPrayerSchedule(): Flow<PrayerScheduleCacheEntity?> =
        dao.observeCachedPrayerSchedule()

    suspend fun cachePrayerSchedule(
        locationName: String,
        latitude: Double,
        longitude: Double,
        methodName: String,
        schedule: DailyPrayerSchedule
    ): Result<Unit> = runCatching {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val slotsByKey = schedule.slots.associateBy { it.key }
        dao.upsertPrayerScheduleCache(
            PrayerScheduleCacheEntity(
                id = "today_schedule",
                dateKey = dateKey,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                methodName = methodName,
                fajr = slotsByKey["fajr"]?.formatted12h.orEmpty(),
                sunrise = schedule.sunriseFormatted,
                ishraq = schedule.ishraqFormatted,
                chasht = schedule.chashtFormatted,
                zawal = schedule.zawalFormatted,
                dhuhr = slotsByKey["dhuhr"]?.formatted12h.orEmpty(),
                asr = slotsByKey["asr"]?.formatted12h.orEmpty(),
                sunset = schedule.sunsetFormatted,
                maghrib = slotsByKey["maghrib"]?.formatted12h.orEmpty(),
                isha = slotsByKey["isha"]?.formatted12h.orEmpty(),
                sahari = schedule.sahariFormatted,
                iftar = schedule.iftarFormatted,
                qiblaDegrees = schedule.qiblaDegrees,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }
}
