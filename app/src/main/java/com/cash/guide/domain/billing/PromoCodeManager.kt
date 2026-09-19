package com.cash.guide.domain.billing

import android.content.Context
import android.util.Log
import com.cash.guide.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object PromoCodeManager {

    private const val TAG = "PromoCodeManager"
    private const val PREFS_NAME = "promo_code_cache_prefs"
    private const val KEY_CACHED_CODES = "cached_github_promo_codes"
    private const val GITHUB_PROMO_CODES_URL = "https://raw.githubusercontent.com/warqa603/sarf/master/promo_codes.json"

    /**
     * 50 pre-generated VIP codes embedded directly into the application.
     * Guarantees 100% offline functionality without requiring internet access.
     */
    val DEFAULT_PROMO_CODES: Set<String> = setOf(
        "VIP-7K9M", "VIP-3X8P", "VIP-9W2Z", "VIP-4T7B", "VIP-6M1Q",
        "VIP-8R5V", "VIP-2J4Y", "VIP-5N8C", "VIP-7E3W", "VIP-9L6K",
        "VIP-4D9L", "VIP-8W6T", "VIP-5M2N", "VIP-3P8X", "VIP-7B4Z",
        "VIP-2K9E", "VIP-6T5R", "VIP-9D3Y", "VIP-4Z8F", "VIP-8C2V",
        "VIP-2F8H", "VIP-7K3R", "VIP-6P9C", "VIP-4L2D", "VIP-9H5W",
        "VIP-3N8M", "VIP-5X7K", "VIP-8T4P", "VIP-2M6Q", "VIP-7V5D",
        "VIP-9E4T", "VIP-3W7V", "VIP-5R2Z", "VIP-8B6K", "VIP-7Y3P",
        "VIP-2N9X", "VIP-4C6L", "VIP-6Z8B", "VIP-9K2T", "VIP-3L7N",
        "VIP-8H3K", "VIP-5M7D", "VIP-2T9N", "VIP-6L4W", "VIP-9C8F",
        "VIP-3X5H", "VIP-7P2M", "VIP-4B9V", "VIP-8R7L", "VIP-5W9K"
    )

    sealed class RedeemResult {
        data class Success(val code: String) : RedeemResult()
        object AlreadyVip : RedeemResult()
        object InvalidCode : RedeemResult()
    }

    /**
     * Normalizes and cleans the user input.
     * Supports "VIP-7K9M", "vip-7k9m", "vip7k9m", "VIP7K9M", with or without spaces.
     */
    fun normalizeCode(input: String): String {
        var clean = input.trim().replace(" ", "").uppercase()
        if (!clean.contains("-") && clean.startsWith("VIP") && clean.length > 3) {
            clean = "VIP-" + clean.substring(3)
        }
        return clean
    }

    /**
     * Verifies if a given code is valid against embedded codes and remote cached codes.
     */
    fun isValidCode(context: Context, code: String): Boolean {
        val normalized = normalizeCode(code)
        if (DEFAULT_PROMO_CODES.contains(normalized)) {
            return true
        }
        val cachedRemote = getCachedCodes(context)
        return cachedRemote.contains(normalized)
    }

    /**
     * Redeems a code and unlocks VIP if valid.
     */
    suspend fun redeemCode(context: Context, rawCode: String): RedeemResult = withContext(Dispatchers.IO) {
        val normalized = normalizeCode(rawCode)
        if (normalized.isBlank()) {
            return@withContext RedeemResult.InvalidCode
        }

        if (!isValidCode(context, normalized)) {
            // Attempt a fresh remote sync just in case a new code was recently added to GitHub
            syncCodesFromGithub(context)
            if (!isValidCode(context, normalized)) {
                return@withContext RedeemResult.InvalidCode
            }
        }

        val settingsRepo = SettingsRepository(context)
        settingsRepo.setVipUnlocked(true, normalized)
        BillingManager.getInstance(context).setVipUnlocked(true)

        Log.d(TAG, "Successfully redeemed VIP code: $normalized")
        RedeemResult.Success(normalized)
    }

    /**
     * Fetches the latest promo_codes.json from GitHub and caches them locally.
     */
    suspend fun syncCodesFromGithub(context: Context) = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_PROMO_CODES_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 6000
            connection.readTimeout = 6000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val content = reader.readText()
                reader.close()

                val json = JSONObject(content)
                if (json.has("promo_codes")) {
                    val jsonArray = json.getJSONArray("promo_codes")
                    val remoteCodes = mutableSetOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        val code = jsonArray.optString(i)?.let { normalizeCode(it) }
                        if (!code.isNullOrBlank()) {
                            remoteCodes.add(code)
                        }
                    }

                    if (remoteCodes.isNotEmpty()) {
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putStringSet(KEY_CACHED_CODES, remoteCodes).apply()
                        Log.d(TAG, "Synced ${remoteCodes.size} promo codes from GitHub.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync promo codes from GitHub (using local fallback)", e)
        }
    }

    private fun getCachedCodes(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_CACHED_CODES, emptySet()) ?: emptySet()
    }
}
