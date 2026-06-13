package com.tradelog.app.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/** One row of the Forex Factory weekly calendar JSON feed. */
@Serializable
data class FFEvent(
    @SerialName("title") val title: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("date") val date: String = "",
    @SerialName("impact") val impact: String = "",
    @SerialName("forecast") val forecast: String = "",
    @SerialName("previous") val previous: String = ""
)

interface ForexFactoryApi {
    @GET("ff_calendar_thisweek.json")
    suspend fun thisWeek(): List<FFEvent>
}

object NetworkModule {
    private const val BASE_URL = "https://nfs.faireconomy.media/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val api: ForexFactoryApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ForexFactoryApi::class.java)
    }
}
