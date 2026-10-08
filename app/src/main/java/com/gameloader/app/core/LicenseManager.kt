package com.gameloader.app.core

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object LicenseManager {

    private const val ENDPOINT = "https://akyt.pnl.lat/connect"
    private const val GAME = "PUBG"

    // in-memory only. cleared on process death. never persisted.
    @Volatile
    private var gToken: String = ""

    @Volatile
    private var gExpiry: String = ""

    @Volatile
    private var gRng: Long = 0L

    fun getToken(): String = gToken
    fun getExpiry(): String = gExpiry
    fun getRng(): Long = gRng

    data class Result(
        val success: Boolean,
        val message: String = "",
        val expiry: String = "",
        val token: String = "",
        val rng: Long = 0L
    )

    // ---------- remote check (call from a background thread) ----------

    fun activate(ctx: Context, userKey: String): Result {
        return try {
            val serial = buildSerial(ctx, userKey)
            val body = "game=$GAME&user_key=${enc(userKey)}&serial=${enc(serial)}"

            val url = URL(ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Charset", "UTF-8")
                setRequestProperty("User-Agent", "")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

            val json = JSONObject(text)
            val status = json.optBoolean("status", false)

            if (!status) {
                val reason = json.optString("reason", "Invalid license key")
                return Result(false, reason)
            }

            val data = json.optJSONObject("data")
                ?: return Result(false, "Malformed response (no data)")

            val token = data.optString("token", "")
            val expiry = data.optString("EXP", "")
            val rng = data.optLong("rng", 0L)

            if (token.isEmpty()) {
                return Result(false, "Malformed response (no token)")
            }

            // store in memory only — no shared prefs, no disk
            gToken = token
            gExpiry = expiry
            gRng = rng

            Result(
                success = true,
                message = "Login successful",
                expiry = expiry,
                token = token,
                rng = rng
            )
        } catch (e: Exception) {
            Result(false, e.message ?: "Network error")
        }
    }

    // ---------- helpers ----------

    private fun buildSerial(ctx: Context, userKey: String): String {
        val androidId = try {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (_: Throwable) { "" }

        val raw = userKey + androidId + Build.MODEL + Build.BRAND
        return UUID.nameUUIDFromBytes(raw.toByteArray(Charsets.UTF_8)).toString()
    }

    private fun enc(s: String): String {
        return try {
            java.net.URLEncoder.encode(s, "UTF-8")
        } catch (_: Throwable) {
            s
        }
    }
}