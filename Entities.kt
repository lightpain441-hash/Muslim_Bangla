package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int,
    val nameArabic: String,
    val nameBangla: String,
    val nameEnglish: String,
    val meaningBangla: String,
    val revelationPlace: String,
    val ayahCount: Int,
    val juzStart: Int,
    val isTextDownloaded: Boolean = false,
    val isAudioDownloaded: Boolean = false
)

@Entity(
    tableName = "ayahs",
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["id"],
            childColumns = ["surahId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("surahId"), Index("juzNumber")]
)
data class AyahEntity(
    @PrimaryKey val id: Int,
    val surahId: Int,
    val ayahNumber: Int,
    val juzNumber: Int,
    val arabicUthmani: String,
    val banglaPronunciation: String,
    val banglaTranslation: String,
    val tafsirBangla: String,
    val audioNumberGlobal: Int
)

@Entity(tableName = "hadith_books")
data class HadithBookEntity(
    @PrimaryKey val id: String,
    val titleBangla: String,
    val titleArabic: String,
    val compilerBangla: String,
    val totalHadithCount: Int,
    val apiEditionBangla: String?,
    val apiEditionArabic: String?,
    val isDownloaded: Boolean = false
)

@Entity(
    tableName = "hadiths",
    foreignKeys = [
        ForeignKey(
            entity = HadithBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class HadithEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val hadithNumber: Int,
    val chapterBangla: String,
    val chapterArabic: String,
    val arabicText: String,
    val banglaTranslation: String,
    val sanadChainBangla: String,
    val gradeLabel: String,
    val gradeExplanation: String
)

@Entity(
    tableName = "narrator_grades",
    foreignKeys = [
        ForeignKey(
            entity = HadithEntity::class,
            parentColumns = ["id"],
            childColumns = ["hadithId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("hadithId")]
)
data class NarratorGradeEntity(
    @PrimaryKey val id: String,
    val hadithId: String,
    val orderInChain: Int,
    val narratorNameBangla: String,
    val narratorNameArabic: String,
    val adlStatus: String,
    val dabtMemory: String,
    val intellectStatus: String,
    val psychologicalState: String,
    val overallGrade: String,
    val scholarVerdict: String
)

@Entity(tableName = "duas")
data class DuaEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val categoryBangla: String,
    val titleBangla: String,
    val arabicText: String,
    val banglaPronunciation: String,
    val banglaMeaning: String,
    val reference: String,
    val repeatCount: Int = 1
)

@Entity(tableName = "offline_audio")
data class OfflineAudioEntity(
    @PrimaryKey val id: String,
    val reciterId: String,
    val surahId: Int,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val downloadStatus: String
)

@Entity(tableName = "bookmarks", indices = [Index("itemType")])
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val itemType: String,
    val refId: String,
    val titleBangla: String,
    val subtitle: String? = null,
    val excerpt: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "ayah_notes", indices = [Index("surahId")])
data class AyahNoteEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val surahId: Long,
    val ayahNumber: Long,
    val surahNameBangla: String,
    val noteText: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    val userId: String = "local_user",
    val category: String,
    val score: Long,
    val totalQuestions: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "matrimonial_profiles", indices = [Index("active"), Index("gender")])
data class MatrimonialProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val profileCode: String,
    val gender: String = "PATRO",
    val age: Long = 25L,
    val division: String = "ঢাকা",
    val education: String = "",
    val profession: String = "",
    val practicingNote: String = "",
    val bioBangla: String = "",
    val waliRelation: String = "বাবা",
    val photoUrl: String? = null,
    val photoBlurred: Boolean = true,
    val active: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "matrimonial_requests", indices = [Index("senderId"), Index("receiverId")])
data class MatrimonialRequestEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val receiverName: String,
    val status: String = "PENDING",
    val introMessage: String,
    val photoRevealApproved: Boolean = false,
    val waliVerified: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val participantIds: List<String>
        get() = listOf(senderId, receiverId)
}

@Entity(
    tableName = "matrimonial_messages",
    foreignKeys = [
        ForeignKey(
            entity = MatrimonialRequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["requestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("requestId")]
)
data class MatrimonialMessageEntity(
    @PrimaryKey val id: String,
    val requestId: String,
    val senderId: String,
    val text: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_schedule_cache")
data class PrayerScheduleCacheEntity(
    @PrimaryKey val id: String = "today_schedule",
    val dateKey: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val methodName: String,
    val fajr: String,
    val sunrise: String,
    val ishraq: String,
    val chasht: String,
    val zawal: String,
    val dhuhr: String,
    val asr: String,
    val sunset: String,
    val maghrib: String,
    val isha: String,
    val sahari: String,
    val iftar: String,
    val qiblaDegrees: Double,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

