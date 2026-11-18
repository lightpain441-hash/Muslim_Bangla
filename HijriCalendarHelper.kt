package com.example.domain.islamic

import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNameBangla: String,
    val formattedBangla: String,
    val specialDayBangla: String? = null
)

data class IslamicImportantDay(
    val hijriMonth: Int,
    val hijriDay: Int,
    val titleBangla: String,
    val descriptionBangla: String
)

object HijriCalendarHelper {
    val hijriMonthsBangla = listOf(
        "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানী",
        "জুমাদাল উলা", "জুমাদাস সানী", "রজব", "শাবান",
        "রমজান", "শাওয়াল", "যিলকদ", "যিলহজ"
    )

    val importantDays = listOf(
        IslamicImportantDay(1, 1, "হিজরি নববর্ষ", "ইসলামিক নতুন বছর মুহাররম মাসের সূচনা।"),
        IslamicImportantDay(1, 10, "আশুরা (১০ মুহাররম)", "নফল রোজা ও মুসা (আ.)-এর বিজয়ের স্মরণীয় দিন।"),
        IslamicImportantDay(3, 12, "রবিউল আউয়াল (১২ তারিখ)", "রাসূলুল্লাহ (সা.)-এর জন্ম ও ওফাত দিবস; সিরাত পাঠের মাস।"),
        IslamicImportantDay(7, 27, "শবে মিরাজ স্মরণ", "রজব মাসের ঐতিহাসিক তাৎপর্যপূর্ণ রাত।"),
        IslamicImportantDay(8, 15, "শবে বরাত (নিসফে শাবান)", "ক্ষমা প্রার্থনা ও নফল ইবাদতের বরকতময় রাত।"),
        IslamicImportantDay(9, 1, "রমজানুল মোবারক শুরু", "সিয়াম সাধনা ও কুরআন নাজিলের পবিত্র মাস।"),
        IslamicImportantDay(9, 27, "লাইলাতুল কদর (২৭ রমজান সম্ভাব্য)", "হাজার মাসের চেয়ে উত্তম মহিমান্বিত রজনী।"),
        IslamicImportantDay(10, 1, "পবিত্র ঈদুল ফিতর", "রমজান শেষে আনন্দ ও ফিতরা আদায়ের দিন।"),
        IslamicImportantDay(12, 9, "ইয়াওমে আরাফাহ", "হজের মূল দিন ও রোজার মাধ্যমে দুই বছরের গুনাহ মাফের সুসংবাদ।"),
        IslamicImportantDay(12, 10, "পবিত্র ঈদুল আজহা", "ত্যাগ ও কুরবানীর মহান দিন।")
    )

    fun toBanglaDigits(input: String): String {
        val bangla = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') sb.append(bangla[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    fun toBanglaDigits(num: Int): String = toBanglaDigits(num.toString())

    fun gregorianToHijri(cal: Calendar = Calendar.getInstance()): HijriDate {
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val jd = if ((year > 1582) || (year == 1582 && month > 10) || (year == 1582 && month == 10 && day > 14)) {
            floor((1461 * (year + 4800 + floor((month - 14) / 12.0))) / 4.0) +
                floor((367 * (month - 2 - 12 * floor((month - 14) / 12.0))) / 12.0) -
                floor((3 * floor((year + 4900 + floor((month - 14) / 12.0)) / 100.0)) / 4.0) +
                day - 32075
        } else {
            367 * year - floor((7 * (year + 5001 + floor((month - 9) / 7.0))) / 4.0) +
                floor((275 * month) / 9.0) + day + 1729777
        }

        val l = jd - 1948440 + 10632
        val n = floor((l - 1) / 10631.0)
        val l2 = l - 10631 * n + 354
        val j = (floor((10985 - l2) / 5316.0)) * (floor((50 * l2) / 17719.0)) +
            (floor(l2 / 5670.0)) * (floor((43 * l2) / 15238.0))
        val l3 = l2 - (floor((30 - j) / 15.0)) * (floor((17719 * j) / 50.0)) -
            (floor(j / 16.0)) * (floor((15238 * j) / 43.0)) + 29
        val hMonth = floor((24 * l3) / 709.0).toInt().coerceIn(1, 12)
        val hDay = (l3 - floor((709 * hMonth) / 24.0)).toInt().coerceIn(1, 30)
        val hYear = (30 * n + j - 30).toInt()

        val monthName = hijriMonthsBangla[hMonth - 1]
        val special = importantDays.firstOrNull { it.hijriMonth == hMonth && it.hijriDay == hDay }?.titleBangla
        return HijriDate(
            day = hDay,
            month = hMonth,
            year = hYear,
            monthNameBangla = monthName,
            formattedBangla = "${toBanglaDigits(hDay)} $monthName ${toBanglaDigits(hYear)} হিজরি",
            specialDayBangla = special
        )
    }
}
