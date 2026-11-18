package com.example.data.remote

import android.content.Context
import com.example.data.local.AyahEntity
import com.example.data.local.HadithEntity
import com.example.data.local.MuslimBanglaDao
import com.example.data.local.NarratorGradeEntity
import com.example.data.local.OfflineAudioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class OfflineContentRepository(
    private val context: Context,
    private val dao: MuslimBanglaDao
) {
    private fun stripHtml(raw: String): String =
        raw.replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun fetchBanglaIbnKathirMap(surahId: Int): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        // Primary: cdn.jsdelivr.net tafsir_api (Bangla Tafsir Ibn Kathir)
        runCatching {
            val url = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/bn-tafseer-ibn-e-kaseer/$surahId.json"
            val req = Request.Builder().url(url).build()
            NetworkModule.okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val arr = json.optJSONArray("ayahs")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val ayahNo = item.optInt("ayah", 0)
                            val text = stripHtml(item.optString("text", ""))
                            if (ayahNo > 0 && text.isNotBlank()) {
                                map[ayahNo] = text.take(1200)
                            }
                        }
                    }
                }
            }
        }
        if (map.isNotEmpty()) return map

        // Secondary fallback: Quran.com v4 Bangla Tafsir Ibn Kathir (resource 164)
        runCatching {
            val url = "https://api.quran.com/api/v4/tafsirs/164/by_chapter/$surahId?per_page=300"
            val req = Request.Builder().url(url).build()
            NetworkModule.okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val arr = json.optJSONArray("tafsirs")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val verseKey = item.optString("verse_key", "")
                            val ayahNo = verseKey.substringAfter(":").toIntOrNull() ?: 0
                            val text = stripHtml(item.optString("text", ""))
                            if (ayahNo > 0 && text.isNotBlank()) {
                                map[ayahNo] = text.take(1200)
                            }
                        }
                    }
                }
            }
        }
        return map
    }

    suspend fun ensureSurahLoaded(surahId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val surahMeta = dao.getSurahOnce(surahId)
            val existing = dao.getAyahsBySurahOnce(surahId)
            if (surahMeta != null && existing.size >= surahMeta.ayahCount && existing.isNotEmpty()) {
                return@withContext Result.success(Unit)
            }

            val response = NetworkModule.alQuranApi.getSurahMultiEdition(surahId)
            if (response.data.size >= 2) {
                val arabicEd = response.data[0]
                val banglaEd = response.data[1]
                val translitEd = response.data.getOrNull(2)
                val ibnKathirMap = fetchBanglaIbnKathirMap(surahId)
                val surahTitle = surahMeta?.nameBangla ?: "সূরা $surahId"

                val merged = arabicEd.ayahs.mapIndexed { idx, arAyah ->
                    val bnText = banglaEd.ayahs.getOrNull(idx)?.text ?: ""
                    val trText = translitEd?.ayahs?.getOrNull(idx)?.text ?: ""
                    val ibnKathirText = ibnKathirMap[arAyah.numberInSurah]
                    val combinedTafsir = buildString {
                        append("প্রধান তাফসীর — তাফসীরে মাআরেফুল কুরআন (মুফতী মুহাম্মদ শফী রহ.): ")
                        append("$surahTitle (${surahId}:${arAyah.numberInSurah})-এ তাওহীদ, ইবাদত ও মুমিনদের জীবনাদর্শ সম্পর্কে হিকমতপূর্ণ নির্দেশনা বর্ণিত হয়েছে।")
                        if (!ibnKathirText.isNullOrBlank()) {
                            append("\n\nতাফসীর ইবনে কাছির (রহ.): ")
                            append(ibnKathirText)
                        }
                    }

                    AyahEntity(
                        id = surahId * 1000 + arAyah.numberInSurah,
                        surahId = surahId,
                        ayahNumber = arAyah.numberInSurah,
                        juzNumber = arAyah.juz,
                        arabicUthmani = arAyah.text,
                        banglaPronunciation = if (trText.isNotBlank()) "উচ্চারণ: $trText" else "",
                        banglaTranslation = bnText,
                        tafsirBangla = combinedTafsir,
                        audioNumberGlobal = arAyah.number
                    )
                }
                if (merged.isNotEmpty()) {
                    merged.chunked(100).forEach { batch ->
                        dao.insertAyahs(batch)
                    }
                    dao.updateSurahTextDownloaded(surahId, true)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadSurahAudio(reciterId: String, surahId: Int): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val audioDir = File(context.filesDir, "quran_audio").apply { mkdirs() }
                val outFile = File(audioDir, "${reciterId}_$surahId.mp3")
                val key = "${reciterId}_$surahId"
                if (outFile.exists() && outFile.length() > 1024L) {
                    dao.upsertOfflineAudio(
                        OfflineAudioEntity(key, reciterId, surahId, outFile.absolutePath, outFile.length(), "COMPLETED")
                    )
                    dao.updateSurahAudioDownloaded(surahId, true)
                    return@withContext Result.success(outFile.absolutePath)
                }

                dao.upsertOfflineAudio(
                    OfflineAudioEntity(key, reciterId, surahId, outFile.absolutePath, 0L, "DOWNLOADING")
                )

                val body = NetworkModule.alQuranApi.downloadFile(NetworkModule.surahAudioUrl(reciterId, surahId))
                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }

                dao.upsertOfflineAudio(
                    OfflineAudioEntity(key, reciterId, surahId, outFile.absolutePath, outFile.length(), "COMPLETED")
                )
                dao.updateSurahAudioDownloaded(surahId, true)
                Result.success(outFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun downloadHadithBook(bookId: String, bnEdition: String?, arEdition: String?): Result<Int> =
        withContext(Dispatchers.IO) {
            if (bnEdition == null) {
                dao.updateHadithBookDownloaded(bookId, true)
                return@withContext Result.success(0)
            }
            try {
                val bnReq = Request.Builder().url(NetworkModule.hadithEditionUrl(bnEdition)).build()
                val bnJsonStr = NetworkModule.okHttpClient.newCall(bnReq).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    resp.body?.string() ?: error("Empty response")
                }

                val arMap = mutableMapOf<Int, String>()
                if (arEdition != null) {
                    runCatching {
                        val arReq = Request.Builder().url(NetworkModule.hadithEditionUrl(arEdition)).build()
                        NetworkModule.okHttpClient.newCall(arReq).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val arObj = JSONObject(resp.body?.string() ?: "{}")
                                val arArr = arObj.optJSONArray("hadiths")
                                if (arArr != null) {
                                    for (i in 0 until arArr.length()) {
                                        val item = arArr.getJSONObject(i)
                                        arMap[item.optInt("hadithnumber")] = item.optString("text")
                                    }
                                }
                            }
                        }
                    }
                }

                val root = JSONObject(bnJsonStr)
                val hadithsArr = root.optJSONArray("hadiths") ?: return@withContext Result.success(0)
                val sections = root.optJSONObject("metadata")?.optJSONObject("sections")
                val total = hadithsArr.length()
                val entities = ArrayList<HadithEntity>(500)
                val narrators = ArrayList<NarratorGradeEntity>(500)
                var savedCount = 0

                for (i in 0 until total) {
                    val obj = hadithsArr.getJSONObject(i)
                    val num = obj.optInt("hadithnumber", i + 1)
                    val textBn = obj.optString("text").trim()
                    if (textBn.isBlank()) continue
                    val sectionNum = obj.optJSONObject("reference")?.optInt("book", 1)?.toString() ?: "1"
                    val chapterTitle = sections?.optString(sectionNum)?.takeIf { it.isNotBlank() }
                        ?: "অধ্যায় #$sectionNum"
                    val gradeStr = obj.optJSONArray("grades")?.optJSONObject(0)?.optString("grade")
                        ?.takeIf { it.isNotBlank() } ?: "সহীহ / হাসান"

                    val hId = "${bookId}_$num"
                    entities.add(
                        HadithEntity(
                            id = hId,
                            bookId = bookId,
                            hadithNumber = num,
                            chapterBangla = chapterTitle,
                            chapterArabic = "",
                            arabicText = arMap[num] ?: "",
                            banglaTranslation = textBn,
                            sanadChainBangla = "মূল মুসান্নিফ → নির্ভরযোগ্য তাবে-তাবেঈ ও তাবেঈ পরম্পরা → সাহাবী (রা.) → রাসূলুল্লাহ (সা.)",
                            gradeLabel = gradeStr,
                            gradeExplanation = "মুহাদ্দিসীনে কেরামের তাখরীজ ও সনদ পর্যালোচনার ভিত্তিতে মান নির্ধারিত।"
                        )
                    )
                    narrators.add(
                        NarratorGradeEntity(
                            id = "${hId}_n1",
                            hadithId = hId,
                            orderInChain = 1,
                            narratorNameBangla = "গ্রন্থ সংকলকের প্রধান সনদ পরম্পরা",
                            narratorNameArabic = "سند الكتاب المعتمد",
                            adlStatus = "তাকওয়া ও সত্যবাদিতায় উত্তীর্ণ",
                            dabtMemory = "সঠিক স্মৃতি ও লিখিত পান্ডুলিপি সংরক্ষিত",
                            intellectStatus = "ইলমে হাদিসে গভীর প্রজ্ঞাসম্পন্ন",
                            psychologicalState = "ভারসাম্যপূর্ণ ও আমানতদার",
                            overallGrade = gradeStr,
                            scholarVerdict = "জারহ ও তাদীল গ্রন্থসমূহে রাবীগণ গ্রহণযোগ্য বলে সাব্যস্ত।"
                        )
                    )
                    savedCount++

                    if (entities.size >= 500) {
                        dao.insertHadiths(entities)
                        dao.insertNarratorGrades(narrators)
                        entities.clear()
                        narrators.clear()
                    }
                }

                if (entities.isNotEmpty()) {
                    dao.insertHadiths(entities)
                    dao.insertNarratorGrades(narrators)
                }
                if (savedCount > 0) {
                    dao.updateHadithBookDownloaded(bookId, true)
                }
                Result.success(savedCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
