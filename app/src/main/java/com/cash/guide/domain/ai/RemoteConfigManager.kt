package com.cash.guide.domain.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object RemoteConfigManager {
    private const val PREFS_NAME = "remote_config_prefs"
    const val KEY_GEMINI_API = "gemini_api_key"
    const val KEY_GEMINI_MODEL = "gemini_model"
    const val KEY_ADS_ENABLED = "ads_enabled"
    const val KEY_ADMOB_BANNER = "admob_banner"
    const val KEY_ADMOB_BANNER_HOME = "admob_banner_home"
    const val KEY_ADMOB_BANNER_CALCUL = "admob_banner_calcul"
    const val KEY_ADMOB_INTERSTITIAL = "admob_interstitial"
    const val KEY_ADMOB_INTERSTITIAL_EXPORT = "admob_interstitial_export"
    const val KEY_ADMOB_REWARDED = "admob_rewarded"
    private const val CONFIG_URL = "https://raw.githubusercontent.com/warqa603/sarf/master/remote_config.json"

    fun getGeminiApiKey(context: Context, fallback: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GEMINI_API, fallback) ?: fallback
    }

    fun getGeminiModel(context: Context, fallback: String = "gemini-2.5-flash"): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GEMINI_MODEL, fallback) ?: fallback
    }

    fun areAdsEnabled(context: Context, fallback: Boolean = true): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ADS_ENABLED, fallback)
    }

    fun getAdUnitId(context: Context, slot: String, fallback: String = ""): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(slot, fallback) ?: fallback
    }

    suspend fun fetchAndCacheConfig(context: Context) = withContext(Dispatchers.IO) {
        try {
            val url = URL(CONFIG_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()

                if (json.has("gemini_api_key")) {
                    val newKey = json.getString("gemini_api_key")
                    if (newKey.isNotBlank()) {
                        editor.putString(KEY_GEMINI_API, newKey)
                    }
                }

                if (json.has("gemini_model")) {
                    val newModel = json.getString("gemini_model")
                    if (newModel.isNotBlank()) {
                        editor.putString(KEY_GEMINI_MODEL, newModel)
                    }
                }

                if (json.has("ads_enabled")) {
                    editor.putBoolean(KEY_ADS_ENABLED, json.getBoolean("ads_enabled"))
                }

                if (json.has(KEY_ADMOB_BANNER)) {
                    editor.putString(KEY_ADMOB_BANNER, json.getString(KEY_ADMOB_BANNER))
                }

                if (json.has(KEY_ADMOB_BANNER_HOME)) {
                    editor.putString(KEY_ADMOB_BANNER_HOME, json.getString(KEY_ADMOB_BANNER_HOME))
                }

                if (json.has(KEY_ADMOB_BANNER_CALCUL)) {
                    editor.putString(KEY_ADMOB_BANNER_CALCUL, json.getString(KEY_ADMOB_BANNER_CALCUL))
                }

                if (json.has(KEY_ADMOB_INTERSTITIAL)) {
                    editor.putString(KEY_ADMOB_INTERSTITIAL, json.getString(KEY_ADMOB_INTERSTITIAL))
                }

                if (json.has(KEY_ADMOB_INTERSTITIAL_EXPORT)) {
                    editor.putString(KEY_ADMOB_INTERSTITIAL_EXPORT, json.getString(KEY_ADMOB_INTERSTITIAL_EXPORT))
                }

                if (json.has(KEY_ADMOB_REWARDED)) {
                    editor.putString(KEY_ADMOB_REWARDED, json.getString(KEY_ADMOB_REWARDED))
                }

                editor.apply()
                Log.d("RemoteConfig", "Remote config updated successfully from GitHub.")
            }
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Failed to fetch remote config", e)
        }
    }
}
