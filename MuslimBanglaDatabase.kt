package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        HadithBookEntity::class,
        HadithEntity::class,
        NarratorGradeEntity::class,
        DuaEntity::class,
        OfflineAudioEntity::class,
        BookmarkEntity::class,
        AyahNoteEntity::class,
        QuizAttemptEntity::class,
        MatrimonialProfileEntity::class,
        MatrimonialRequestEntity::class,
        MatrimonialMessageEntity::class,
        PrayerScheduleCacheEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MuslimBanglaDatabase : RoomDatabase() {
    abstract fun dao(): MuslimBanglaDao

    companion object {
        @Volatile
        private var INSTANCE: MuslimBanglaDatabase? = null

        fun getInstance(context: Context): MuslimBanglaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MuslimBanglaDatabase::class.java,
                    "muslim_bangla_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { db ->
                        INSTANCE = db
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            seedIfEmpty(db.dao())
                        }
                    }
            }
        }

        suspend fun seedIfEmpty(dao: MuslimBanglaDao) {
            if (dao.countSurahs() == 0) {
                dao.insertSurahs(SeedDataSurahs.all114Surahs())
                dao.insertAyahs(SeedDataSurahs.starterAyahs())
            }
            if (dao.countHadithBooks() == 0) {
                dao.insertHadithBooks(SeedDataHadithDuaQuiz.all10HadithBooks())
                dao.insertHadiths(SeedDataHadithDuaQuiz.starterHadiths())
                dao.insertNarratorGrades(SeedDataHadithDuaQuiz.starterNarratorGrades())
                dao.insertDuas(SeedDataHadithDuaQuiz.starterDuas())
            }
            if (dao.countMatrimonialProfiles() == 0) {
                dao.insertMatrimonialProfilesIfAbsent(SeedDataMatrimonial.starterProfiles())
                dao.insertMatrimonialRequestIfAbsent(SeedDataMatrimonial.starterIncomingRequest())
            }
        }
    }
}
