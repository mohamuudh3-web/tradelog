package com.tradelog.app.network

import com.tradelog.app.data.SyncStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Supabase project — anon key is publish-safe (Row-Level Security protects data). */
object SupabaseConfig {
    const val URL = "https://dhrrugmeyrjgubdjnksa.supabase.co"
    const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRocnJ1Z21leXJqZ3ViZGpua3NhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE1MDE4OTUsImV4cCI6MjA5NzA3Nzg5NX0.5tK1ZKc63kVGDQxHLXiUrynrflwQTJvWItj43_LvIBU"
}

@Serializable
private data class AuthUser(val id: String = "", val email: String = "")

@Serializable
private data class AuthResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 3600,
    val user: AuthUser = AuthUser()
)

class SupabaseException(message: String) : Exception(message)

/**
 * Thin Supabase client over OkHttp: GoTrue auth, PostgREST tables, and Storage uploads.
 * Tokens live in [SyncStore]; the access token is refreshed automatically when near expiry.
 */
class SupabaseClient(private val store: SyncStore) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val jsonMedia = "application/json".toMediaType()

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ---------------- Auth ----------------

    suspend fun signIn(email: String, password: String): Result<Unit> =
        authPassword("token?grant_type=password", email, password)

    suspend fun signUp(email: String, password: String): Result<Unit> =
        authPassword("signup", email, password)

    private suspend fun authPassword(path: String, email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = json.encodeToString(
                    AuthBody.serializer(),
                    AuthBody(email.trim(), password)
                ).toRequestBody(jsonMedia)
                val req = Request.Builder()
                    .url("${SupabaseConfig.URL}/auth/v1/$path")
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
                val auth = execAuth(req)
                if (auth.accessToken.isBlank() || auth.user.id.isBlank()) {
                    throw SupabaseException("Sign-in did not return a session. Check email/password.")
                }
                store.saveSession(
                    email = auth.user.email.ifBlank { email.trim() },
                    userId = auth.user.id,
                    accessToken = auth.accessToken,
                    refreshToken = auth.refreshToken,
                    expiresAt = System.currentTimeMillis() + auth.expiresIn * 1000
                )
                Unit
            }
        }

    @Serializable
    private data class AuthBody(val email: String, val password: String)

    private fun execAuth(req: Request): AuthResponse {
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw SupabaseException(parseError(text, res.code))
            return json.decodeFromString(AuthResponse.serializer(), text)
        }
    }

    /** Returns a valid access token, refreshing if it expires within 60s. */
    private suspend fun freshToken(): String {
        val s = store.current()
        if (!s.isLoggedIn) throw SupabaseException("Not signed in.")
        if (s.expiresAt - System.currentTimeMillis() > 60_000) return s.accessToken
        if (s.refreshToken.isBlank()) return s.accessToken
        return withContext(Dispatchers.IO) {
            val body = """{"refresh_token":"${s.refreshToken}"}""".toRequestBody(jsonMedia)
            val req = Request.Builder()
                .url("${SupabaseConfig.URL}/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            val auth = execAuth(req)
            store.updateTokens(
                auth.accessToken,
                auth.refreshToken.ifBlank { s.refreshToken },
                System.currentTimeMillis() + auth.expiresIn * 1000
            )
            auth.accessToken
        }
    }

    // ---------------- PostgREST ----------------

    /** GET a table with a raw query string (e.g. "select=*&updated_at=gt.0"). Returns JSON array text. */
    suspend fun getRows(table: String, query: String): String = withContext(Dispatchers.IO) {
        val token = freshToken()
        val req = Request.Builder()
            .url("${SupabaseConfig.URL}/rest/v1/$table?$query")
            .addHeader("apikey", SupabaseConfig.ANON_KEY)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw SupabaseException(parseError(text, res.code))
            text
        }
    }

    /** Upsert a JSON array body into a table (conflict on primary key uid). */
    suspend fun upsert(table: String, jsonArrayBody: String) = withContext(Dispatchers.IO) {
        val token = freshToken()
        val req = Request.Builder()
            .url("${SupabaseConfig.URL}/rest/v1/$table")
            .addHeader("apikey", SupabaseConfig.ANON_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(jsonArrayBody.toRequestBody(jsonMedia))
            .build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw SupabaseException(parseError(text, res.code))
        }
    }

    // ---------------- Storage ----------------

    /** Uploads bytes to the public `screenshots` bucket and returns the public URL, or null on failure. */
    suspend fun uploadScreenshot(path: String, bytes: ByteArray, contentType: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = freshToken()
                val req = Request.Builder()
                    .url("${SupabaseConfig.URL}/storage/v1/object/screenshots/$path")
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("x-upsert", "true")
                    .post(bytes.toRequestBody(contentType.toMediaType()))
                    .build()
                http.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@runCatching null
                    "${SupabaseConfig.URL}/storage/v1/object/public/screenshots/$path"
                }
            }.getOrNull()
        }

    private fun parseError(text: String, code: Int): String {
        return try {
            val obj = json.parseToJsonElement(text)
            val msg = (obj as? kotlinx.serialization.json.JsonObject)?.let { o ->
                (o["msg"] ?: o["message"] ?: o["error_description"] ?: o["error"])
                    ?.toString()?.trim('"')
            }
            msg ?: "Request failed ($code)"
        } catch (e: Exception) {
            "Request failed ($code)"
        }
    }
}
