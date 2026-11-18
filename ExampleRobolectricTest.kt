package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalUserDataRepository
import com.example.data.local.MuslimBanglaDatabase
import com.example.data.local.PrayerCalcMethod
import com.example.data.local.SeedDataHadithDuaQuiz
import com.example.data.local.SeedDataMatrimonial
import com.example.data.local.SeedDataSurahs
import com.example.data.local.UserPreferencesRepository
import com.example.domain.islamic.HijriCalendarHelper
import com.example.domain.islamic.PrayerTimeCalculator
import com.example.domain.islamic.ZakatCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("মুসলিম বাংলা", appName)
    }

    @Test
    fun `verify complete 114 surahs 10 hadith books and matrimonial seeds`() {
        assertEquals(114, SeedDataSurahs.all114Surahs().size)
        assertEquals(5, SeedDataSurahs.reciters.size)
        assertEquals(10, SeedDataHadithDuaQuiz.all10HadithBooks().size)
        assertTrue(SeedDataHadithDuaQuiz.starterNarratorGrades().isNotEmpty())
        assertTrue(SeedDataHadithDuaQuiz.starterDuas().isNotEmpty())
        assertTrue(SeedDataHadithDuaQuiz.quizQuestions.isNotEmpty())
        assertTrue(SeedDataMatrimonial.starterProfiles().size >= 6)
    }

    @Test
    fun `verify room database bookmarks notes and matrimonial persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MuslimBanglaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.dao()
            MuslimBanglaDatabase.seedIfEmpty(dao)
            val repo = LocalUserDataRepository(dao, UserPreferencesRepository(context))

            // Bookmarks in Room
            repo.addBookmark("quran_1_1", "QURAN_AYAH", "1:1", "আল-ফাতিহা (১:১)", "بِسْمِ اللَّهِ", "শুরু করছি আল্লাহর নামে")
            val bookmarks = repo.observeBookmarks().first()
            assertEquals(1, bookmarks.size)
            assertEquals("quran_1_1", bookmarks.first().id)

            // Ayah Notes in Room
            repo.saveAyahNote(1, 1, "আল-ফাতিহা", "প্রতিদিন ফরজ নামাজে পাঠ করা হয়")
            val notes = repo.observeAyahNotes().first()
            assertEquals(1, notes.size)
            assertEquals("প্রতিদিন ফরজ নামাজে পাঠ করা হয়", notes.first().noteText)

            // Matrimonial in Room (accept incoming request & send chat message)
            val profiles = repo.observeActiveMatrimonialProfiles().first()
            assertEquals(6, profiles.size)
            val requests = repo.observeMatrimonialRequests().first()
            assertEquals(1, requests.size)
            val reqId = requests.first().id
            repo.respondToMatrimonialRequest(reqId, accept = true, approvePhotoReveal = false)
            repo.sendMatrimonialMessage(reqId, "আসসালামু আলাইকুম, অভিভাবকের সাথে কথা বলুন।")
            val messages = repo.observeMatrimonialMessages(reqId).first()
            assertEquals(2, messages.size)

            // Prayer schedule Room cache
            val dhakaSchedule = PrayerTimeCalculator.calculateForToday(23.8103, 90.4125, PrayerCalcMethod.MWL)
            repo.cachePrayerSchedule("ঢাকা মহানগর, ঢাকা", 23.8103, 90.4125, PrayerCalcMethod.MWL.titleBangla, dhakaSchedule)
            val cached = repo.observeCachedPrayerSchedule().first()
            assertEquals("ঢাকা মহানগর, ঢাকা", cached?.locationName)
            assertEquals(dhakaSchedule.sahariFormatted, cached?.sahari)
            assertEquals(dhakaSchedule.iftarFormatted, cached?.iftar)
        } finally {
            db.close()
        }
    }

    @Test
    fun `verify standard asr prayer time calculation and qibla bearing for dhaka even under utc timezone`() {
        val originalTz = TimeZone.getDefault()
        try {
            // Simulate device running in UTC while calculating Dhaka (UTC+6) prayer times
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
                set(2026, Calendar.MARCH, 15, 10, 0, 0)
            }
            val dhakaSchedule = PrayerTimeCalculator.calculateForToday(
                latitude = 23.8103,
                longitude = 90.4125,
                method = PrayerCalcMethod.MWL,
                now = cal
            )
            assertEquals(5, dhakaSchedule.slots.size)
            val fajrHour = dhakaSchedule.slots[0].hour24
            val dhuhrHour = dhakaSchedule.slots[1].hour24
            val asrHour = dhakaSchedule.slots[2].hour24
            val maghribHour = dhakaSchedule.slots[3].hour24
            assertTrue("Dhaka Fajr should be around 04:xx-05:xx local time, got $fajrHour", fajrHour in 4..5)
            assertTrue("Dhaka Dhuhr should be around 12:xx local time, got $dhuhrHour", dhuhrHour == 12)
            assertTrue("Dhaka Standard Asr should be around 15:xx-16:xx local time, got $asrHour", asrHour in 15..16)
            assertTrue("Dhaka Maghrib should be around 18:xx local time, got $maghribHour", maghribHour == 18)
            assertTrue("Dhaka Qibla bearing should face west-northwest (~277 deg)", dhakaSchedule.qiblaDegrees in 270.0..285.0)
            assertEquals(6, dhakaSchedule.extendedTimes.size)
            assertEquals(dhakaSchedule.slots[0].formatted12h, dhakaSchedule.sahariFormatted)
            assertEquals(dhakaSchedule.slots[3].formatted12h, dhakaSchedule.iftarFormatted)
            assertTrue(dhakaSchedule.sunriseFormatted.endsWith("AM"))
            assertTrue(dhakaSchedule.ishraqFormatted.endsWith("AM"))
            assertTrue(dhakaSchedule.chashtFormatted.endsWith("AM"))
            assertTrue(dhakaSchedule.sunsetFormatted.endsWith("PM"))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `verify all 8 divisions 64 districts and bangladesh upazila lookup`() {
        val divisions = com.example.domain.islamic.BangladeshLocations.divisions
        assertEquals(8, divisions.size)
        val totalDistricts = divisions.sumOf { it.districts.size }
        assertEquals(64, totalDistricts)
        val allUpazilas = com.example.domain.islamic.BangladeshLocations.allUpazilas
        assertTrue("Expected at least 490 upazilas, got ${allUpazilas.size}", allUpazilas.size >= 490)

        val teknafResults = com.example.domain.islamic.BangladeshLocations.searchUpazilas("টেকনাফ")
        assertTrue(teknafResults.any { it.nameBangla == "টেকনাফ" && it.districtBangla == "কক্সবাজার" })

        val nearestDhaka = com.example.domain.islamic.BangladeshLocations.findNearestUpazila(23.8103, 90.4125)
        assertEquals("ঢাকা", nearestDhaka?.districtBangla)
    }

    @Test
    fun `verify hijri conversion and zakat nisab calculation`() {
        val hijri = HijriCalendarHelper.gregorianToHijri()
        assertTrue(hijri.year >= 1445)
        assertTrue(hijri.month in 1..12)

        val ineligible = ZakatCalculator.calculate(50000.0, 0.0, 0.0, 0.0, 0.0, 10000.0, 145.0)
        assertFalse(ineligible.isEligible)
        assertEquals(0.0, ineligible.payableZakat, 0.01)

        val eligible = ZakatCalculator.calculate(500000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 145.0)
        assertTrue(eligible.isEligible)
        assertEquals(12500.0, eligible.payableZakat, 0.01)
    }
}
