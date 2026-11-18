package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MuslimBanglaDao {
    // --- QURAN ---
    @Query("SELECT * FROM surahs ORDER BY id ASC")
    fun observeAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE id = :surahId LIMIT 1")
    fun observeSurah(surahId: Int): Flow<SurahEntity?>

    @Query("SELECT * FROM surahs WHERE id = :surahId LIMIT 1")
    suspend fun getSurahOnce(surahId: Int): SurahEntity?

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun countSurahs(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Query("UPDATE surahs SET isTextDownloaded = :downloaded WHERE id = :surahId")
    suspend fun updateSurahTextDownloaded(surahId: Int, downloaded: Boolean)

    @Query("UPDATE surahs SET isAudioDownloaded = :downloaded WHERE id = :surahId")
    suspend fun updateSurahAudioDownloaded(surahId: Int, downloaded: Boolean)

    @Query("SELECT * FROM ayahs WHERE surahId = :surahId ORDER BY ayahNumber ASC")
    fun observeAyahsBySurah(surahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahId = :surahId ORDER BY ayahNumber ASC")
    suspend fun getAyahsBySurahOnce(surahId: Int): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE juzNumber = :juz ORDER BY surahId ASC, ayahNumber ASC")
    fun observeAyahsByJuz(juz: Int): Flow<List<AyahEntity>>

    @Query(
        """
        SELECT * FROM ayahs
        WHERE banglaTranslation LIKE '%' || :query || '%'
           OR banglaPronunciation LIKE '%' || :query || '%'
           OR tafsirBangla LIKE '%' || :query || '%'
           OR arabicUthmani LIKE '%' || :query || '%'
        ORDER BY surahId ASC, ayahNumber ASC
        LIMIT 80
        """
    )
    fun searchAyahs(query: String): Flow<List<AyahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    // --- HADITH ---
    @Query("SELECT * FROM hadith_books")
    fun observeHadithBooks(): Flow<List<HadithBookEntity>>

    @Query("SELECT COUNT(*) FROM hadith_books")
    suspend fun countHadithBooks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadithBooks(books: List<HadithBookEntity>)

    @Query("UPDATE hadith_books SET isDownloaded = :downloaded WHERE id = :bookId")
    suspend fun updateHadithBookDownloaded(bookId: String, downloaded: Boolean)

    @Query("SELECT * FROM hadiths WHERE bookId = :bookId ORDER BY hadithNumber ASC")
    fun observeHadithsByBook(bookId: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE id = :hadithId LIMIT 1")
    fun observeHadithById(hadithId: String): Flow<HadithEntity?>

    @Query(
        """
        SELECT * FROM hadiths
        WHERE banglaTranslation LIKE '%' || :query || '%'
           OR chapterBangla LIKE '%' || :query || '%'
           OR sanadChainBangla LIKE '%' || :query || '%'
           OR arabicText LIKE '%' || :query || '%'
        ORDER BY hadithNumber ASC
        LIMIT 80
        """
    )
    fun searchHadiths(query: String): Flow<List<HadithEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    // --- JARH WA TA'DIL ---
    @Query("SELECT * FROM narrator_grades WHERE hadithId = :hadithId ORDER BY orderInChain ASC")
    fun observeNarratorsForHadith(hadithId: String): Flow<List<NarratorGradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNarratorGrades(narrators: List<NarratorGradeEntity>)

    // --- DUA ---
    @Query("SELECT * FROM duas ORDER BY categoryId ASC, id ASC")
    fun observeAllDuas(): Flow<List<DuaEntity>>

    @Query("SELECT * FROM duas WHERE banglaMeaning LIKE '%' || :query || '%' OR titleBangla LIKE '%' || :query || '%'")
    fun searchDuas(query: String): Flow<List<DuaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuas(duas: List<DuaEntity>)

    // --- OFFLINE AUDIO ---
    @Query("SELECT * FROM offline_audio WHERE reciterId = :reciterId AND surahId = :surahId LIMIT 1")
    suspend fun getOfflineAudio(reciterId: String, surahId: Int): OfflineAudioEntity?

    @Query("SELECT * FROM offline_audio")
    fun observeAllOfflineAudio(): Flow<List<OfflineAudioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineAudio(audio: OfflineAudioEntity)

    @Query("DELETE FROM offline_audio WHERE id = :id")
    suspend fun deleteOfflineAudio(id: String)

    // --- BOOKMARKS ---
    @Query("SELECT * FROM bookmarks ORDER BY createdAtMillis DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: String)

    // --- AYAH NOTES ---
    @Query("SELECT * FROM ayah_notes ORDER BY updatedAtMillis DESC")
    fun observeAyahNotes(): Flow<List<AyahNoteEntity>>

    @Query("SELECT * FROM ayah_notes WHERE id = :noteId LIMIT 1")
    suspend fun getAyahNoteById(noteId: String): AyahNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAyahNote(note: AyahNoteEntity)

    // --- QUIZ ATTEMPTS ---
    @Query("SELECT * FROM quiz_attempts ORDER BY createdAtMillis DESC")
    fun observeQuizAttempts(): Flow<List<QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity)

    // --- MATRIMONIAL ---
    @Query("SELECT COUNT(*) FROM matrimonial_profiles")
    suspend fun countMatrimonialProfiles(): Int

    @Query("SELECT * FROM matrimonial_profiles WHERE active = 1 ORDER BY updatedAtMillis DESC")
    fun observeActiveMatrimonialProfiles(): Flow<List<MatrimonialProfileEntity>>

    @Query("SELECT * FROM matrimonial_profiles WHERE userId = :userId LIMIT 1")
    fun observeMatrimonialProfileByUser(userId: String): Flow<MatrimonialProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatrimonialProfile(profile: MatrimonialProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMatrimonialProfilesIfAbsent(profiles: List<MatrimonialProfileEntity>)

    @Query("SELECT * FROM matrimonial_requests WHERE senderId = :userId OR receiverId = :userId ORDER BY updatedAtMillis DESC")
    fun observeMatrimonialRequests(userId: String): Flow<List<MatrimonialRequestEntity>>

    @Query("SELECT * FROM matrimonial_requests WHERE id = :requestId LIMIT 1")
    suspend fun getMatrimonialRequestById(requestId: String): MatrimonialRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatrimonialRequest(request: MatrimonialRequestEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMatrimonialRequestIfAbsent(request: MatrimonialRequestEntity)

    @Query("UPDATE matrimonial_requests SET status = :status, photoRevealApproved = :approvePhoto, updatedAtMillis = :updatedAt WHERE id = :requestId")
    suspend fun updateMatrimonialRequestResponse(requestId: String, status: String, approvePhoto: Boolean, updatedAt: Long)

    @Query("UPDATE matrimonial_requests SET photoRevealApproved = :approved, updatedAtMillis = :updatedAt WHERE id = :requestId")
    suspend fun updateMatrimonialPhotoReveal(requestId: String, approved: Boolean, updatedAt: Long)

    @Query("SELECT * FROM matrimonial_messages WHERE requestId = :requestId ORDER BY createdAtMillis ASC")
    fun observeMatrimonialMessages(requestId: String): Flow<List<MatrimonialMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatrimonialMessage(message: MatrimonialMessageEntity)

    // --- PRAYER SCHEDULE CACHE ---
    @Query("SELECT * FROM prayer_schedule_cache WHERE id = 'today_schedule' LIMIT 1")
    fun observeCachedPrayerSchedule(): Flow<PrayerScheduleCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerScheduleCache(cache: PrayerScheduleCacheEntity)
}
