package com.example.data.remote

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AlQuranEditionsResponse(
    val code: Int = 0,
    val status: String = "",
    val data: List<AlQuranSurahEdition> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahEdition(
    val number: Int = 0,
    val name: String = "",
    val englishName: String = "",
    val ayahs: List<AlQuranAyahDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AlQuranAyahDto(
    val number: Int = 0,
    val text: String = "",
    val numberInSurah: Int = 0,
    val juz: Int = 1
)

interface AlQuranCloudApi {
    @GET("v1/surah/{surahNumber}/editions/quran-uthmani,bn.bengali,en.transliteration")
    suspend fun getSurahMultiEdition(@Path("surahNumber") surahNumber: Int): AlQuranEditionsResponse

    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): ResponseBody
}

object NetworkModule {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    val alQuranApi: AlQuranCloudApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AlQuranCloudApi::class.java)
    }

    fun surahAudioUrl(reciterId: String, surahId: Int): String =
        "https://cdn.islamic.network/quran/audio-surah/128/$reciterId/$surahId.mp3"

    fun ayahAudioUrl(reciterId: String, globalAyahNumber: Int): String =
        "https://cdn.islamic.network/quran/audio/128/$reciterId/$globalAyahNumber.mp3"

    fun hadithEditionUrl(editionId: String): String =
        "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/$editionId.min.json"
}
