package com.example.domain.islamic

import com.example.data.local.PrayerCalcMethod
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class PrayerTimeSlot(
    val key: String,
    val nameBangla: String,
    val hour24: Int,
    val minute: Int,
    val formatted12h: String,
    val isNext: Boolean = false
)

data class ExtendedPrayerTime(
    val key: String,
    val titleBangla: String,
    val subtitleBangla: String,
    val hour24: Int,
    val minute: Int,
    val formatted12h: String
)

data class DailyPrayerSchedule(
    val slots: List<PrayerTimeSlot>,
    val sunriseFormatted: String,
    val sunsetFormatted: String,
    val ishraqFormatted: String,
    val chashtFormatted: String,
    val zawalFormatted: String,
    val sahariFormatted: String,
    val iftarFormatted: String,
    val extendedTimes: List<ExtendedPrayerTime>,
    val nextPrayer: PrayerTimeSlot,
    val minutesRemaining: Int,
    val qiblaDegrees: Double
)

data class CityPreset(
    val nameBangla: String,
    val lat: Double,
    val lng: Double,
    val utcOffsetHours: Double
)

object PrayerTimeCalculator {
    private const val STANDARD_ASR_SHADOW_FACTOR = 1.0

    val bangladeshCities = listOf(
        CityPreset("ঢাকা, বাংলাদেশ", 23.8103, 90.4125, 6.0),
        CityPreset("চট্টগ্রাম, বাংলাদেশ", 22.3569, 91.7832, 6.0),
        CityPreset("সিলেট, বাংলাদেশ", 24.8949, 91.8687, 6.0),
        CityPreset("রাজশাহী, বাংলাদেশ", 24.3745, 88.6042, 6.0),
        CityPreset("খুলনা, বাংলাদেশ", 22.8456, 89.5403, 6.0),
        CityPreset("রংপুর, বাংলাদেশ", 25.7439, 89.2752, 6.0),
        CityPreset("বরিশাল, বাংলাদেশ", 22.7010, 90.3535, 6.0),
        CityPreset("ময়মনসিংহ, বাংলাদেশ", 24.7471, 90.4203, 6.0),
        CityPreset("মক্কা, সৌদি আরব", 21.4225, 39.8262, 3.0),
        CityPreset("মদিনা, সৌদি আরব", 24.4672, 39.6112, 3.0),
        CityPreset("লন্ডন, যুক্তরাজ্য", 51.5072, -0.1276, 0.0)
    )

    fun resolveTimezoneOffsetHours(latitude: Double, longitude: Double, now: Calendar): Double {
        val matchedPreset = bangladeshCities.firstOrNull {
            kotlin.math.abs(it.lat - latitude) < 0.35 && kotlin.math.abs(it.lng - longitude) < 0.35
        }
        if (matchedPreset != null) return matchedPreset.utcOffsetHours
        if (latitude in 20.3..26.7 && longitude in 88.0..92.8) return 6.0
        return TimeZone.getDefault().getOffset(now.timeInMillis) / 3600000.0
    }

    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val kaabaLat = Math.toRadians(21.422487)
        val kaabaLng = Math.toRadians(39.826206)
        val userLat = Math.toRadians(latitude)
        val userLng = Math.toRadians(longitude)
        val dLon = kaabaLng - userLng
        val y = sin(dLon) * cos(kaabaLat)
        val x = cos(userLat) * sin(kaabaLat) - sin(userLat) * cos(kaabaLat) * cos(dLon)
        val deg = Math.toDegrees(atan2(y, x))
        return (deg + 360.0) % 360.0
    }

    fun calculateForToday(
        latitude: Double,
        longitude: Double,
        method: PrayerCalcMethod,
        now: Calendar = Calendar.getInstance()
    ): DailyPrayerSchedule {
        val tzOffsetHours = resolveTimezoneOffsetHours(latitude, longitude, now)
        val localNow = (now.clone() as Calendar).apply {
            val offsetMillis = (tzOffsetHours * 3600_000L).toLong()
            timeZone = TimeZone.getTimeZone("UTC").apply { rawOffset = offsetMillis.toInt() }
        }
        val year = localNow.get(Calendar.YEAR)
        val month = localNow.get(Calendar.MONTH) + 1
        val day = localNow.get(Calendar.DAY_OF_MONTH)

        val jd = julianDay(year, month, day) - longitude / (15.0 * 24.0)
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = asinDeg(sinDeg(e) * sinDeg(l))

        val transit = 12.0 + tzOffsetHours - longitude / 15.0 - eqTime
        val sunrise = transit - hourAngle(latitude, decl, 0.833) / 15.0
        val sunset = transit + hourAngle(latitude, decl, 0.833) / 15.0
        val fajr = transit - hourAngle(latitude, decl, method.fajrAngle) / 15.0
        val dhuhr = transit + 2.0 / 60.0
        // Standard Asr: shadow length equals object length (factor = 1.0) + noon shadow
        val asrAlt = acotDeg(STANDARD_ASR_SHADOW_FACTOR + tanDeg(kotlin.math.abs(latitude - decl)))
        val asr = transit + hourAngle(latitude, decl, -asrAlt) / 15.0
        val maghrib = sunset + 2.0 / 60.0
        val isha = if (method.ishaMinutesAfterMaghrib != null) {
            maghrib + method.ishaMinutesAfterMaghrib / 60.0
        } else {
            transit + hourAngle(latitude, decl, method.ishaAngle) / 15.0
        }

        val ishraq = sunrise + 15.0 / 60.0
        val chasht = sunrise + (transit - sunrise) * 0.5
        val sahari = fajr
        val iftar = maghrib

        val rawSlots = listOf(
            Triple("fajr", "ফজর", fajr),
            Triple("dhuhr", "যোহর", dhuhr),
            Triple("asr", "আসর", asr),
            Triple("maghrib", "মাগরিব", maghrib),
            Triple("isha", "ইশা", isha)
        )

        val nowMinutes = localNow.get(Calendar.HOUR_OF_DAY) * 60 + localNow.get(Calendar.MINUTE)
        var nextIndex = -1
        val parsed = rawSlots.mapIndexed { idx, (key, name, hoursFloat) ->
            val totalMin = ((fixHour(hoursFloat) * 60.0).toInt()).coerceIn(0, 1439)
            val h = totalMin / 60
            val m = totalMin % 60
            if (nextIndex == -1 && totalMin > nowMinutes) {
                nextIndex = idx
            }
            PrayerTimeSlot(
                key = key,
                nameBangla = name,
                hour24 = h,
                minute = m,
                formatted12h = format12Hour(h, m),
                isNext = false
            )
        }
        val effectiveNextIndex = if (nextIndex == -1) 0 else nextIndex
        val finalSlots = parsed.mapIndexed { idx, slot ->
            slot.copy(isNext = idx == effectiveNextIndex)
        }
        val nextSlot = finalSlots[effectiveNextIndex]
        val nextTotalMin = nextSlot.hour24 * 60 + nextSlot.minute
        val remaining = if (nextTotalMin > nowMinutes) {
            nextTotalMin - nowMinutes
        } else {
            (24 * 60 - nowMinutes) + nextTotalMin
        }

        fun toHm(hoursFloat: Double): Pair<Int, Int> {
            val min = (fixHour(hoursFloat) * 60.0).toInt().coerceIn(0, 1439)
            return (min / 60) to (min % 60)
        }

        val (srH, srM) = toHm(sunrise)
        val (ssH, ssM) = toHm(sunset)
        val (ishH, ishM) = toHm(ishraq)
        val (chH, chM) = toHm(chasht)
        val (zawH, zawM) = toHm(transit)
        val (sahH, sahM) = toHm(sahari)
        val (iftH, iftM) = toHm(iftar)

        val srStr = format12Hour(srH, srM)
        val ssStr = format12Hour(ssH, ssM)
        val ishStr = format12Hour(ishH, ishM)
        val chStr = format12Hour(chH, chM)
        val zawStr = format12Hour(zawH, zawM)
        val sahStr = format12Hour(sahH, sahM)
        val iftStr = format12Hour(iftH, iftM)

        val extended = listOf(
            ExtendedPrayerTime(
                key = "sahari",
                titleBangla = "সাহারি",
                subtitleBangla = "সাহারির শেষ সময় (সুবহে সাদিক)",
                hour24 = sahH,
                minute = sahM,
                formatted12h = sahStr
            ),
            ExtendedPrayerTime(
                key = "sunrise",
                titleBangla = "সূর্যদয় (Sunrise)",
                subtitleBangla = "ফজরের ওয়াক্ত সমাপ্তি",
                hour24 = srH,
                minute = srM,
                formatted12h = srStr
            ),
            ExtendedPrayerTime(
                key = "ishraq",
                titleBangla = "ইশরাক",
                subtitleBangla = "সূর্যোদয়ের ১৫ মিনিট পর",
                hour24 = ishH,
                minute = ishM,
                formatted12h = ishStr
            ),
            ExtendedPrayerTime(
                key = "chasht",
                titleBangla = "দ্বিপ্রহর (চাশত)",
                subtitleBangla = "সালাতুদ দুহা • জাওয়াল: $zawStr",
                hour24 = chH,
                minute = chM,
                formatted12h = chStr
            ),
            ExtendedPrayerTime(
                key = "sunset",
                titleBangla = "সূর্য অস্ত (Sunset)",
                subtitleBangla = "সূর্যাস্ত ও দিনের সমাপ্তি",
                hour24 = ssH,
                minute = ssM,
                formatted12h = ssStr
            ),
            ExtendedPrayerTime(
                key = "iftar",
                titleBangla = "ইফতার",
                subtitleBangla = "ইফতারের সময় (মাগরিব শুরু)",
                hour24 = iftH,
                minute = iftM,
                formatted12h = iftStr
            )
        )

        return DailyPrayerSchedule(
            slots = finalSlots,
            sunriseFormatted = srStr,
            sunsetFormatted = ssStr,
            ishraqFormatted = ishStr,
            chashtFormatted = chStr,
            zawalFormatted = zawStr,
            sahariFormatted = sahStr,
            iftarFormatted = iftStr,
            extendedTimes = extended,
            nextPrayer = nextSlot,
            minutesRemaining = remaining,
            qiblaDegrees = calculateQiblaBearing(latitude, longitude)
        )
    }

    private fun format12Hour(hour24: Int, minute: Int): String {
        val period = if (hour24 < 12) "AM" else "PM"
        val h12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format("%02d:%02d %s", h12, minute, period)
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun hourAngle(lat: Double, decl: Double, angle: Double): Double {
        val num = -sinDeg(angle) - sinDeg(lat) * sinDeg(decl)
        val den = cosDeg(lat) * cosDeg(decl)
        val ratio = (num / den).coerceIn(-0.9999, 0.9999)
        return acos(ratio) * 180.0 / PI
    }

    private fun sinDeg(d: Double) = sin(Math.toRadians(d))
    private fun cosDeg(d: Double) = cos(Math.toRadians(d))
    private fun tanDeg(d: Double) = tan(Math.toRadians(d))
    private fun asinDeg(x: Double) = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun atan2Deg(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun acotDeg(x: Double) = Math.toDegrees(atan(1.0 / x))
    private fun fixAngle(a: Double): Double {
        val r = a - 360.0 * floor(a / 360.0)
        return if (r < 0) r + 360.0 else r
    }
    private fun fixHour(h: Double): Double {
        val r = h - 24.0 * floor(h / 24.0)
        return if (r < 0) r + 24.0 else r
    }
}
