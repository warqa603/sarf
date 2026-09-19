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
    private const val KEY_GEMINI_API = "gemini_api_key"
    private const val CONFIG_URL = "https://raw.githubusercontent.com/warqa603/sarf/master/remote_config.json"

    fun getGeminiApiKey(context: Context, fallback: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GEMINI_API, fallback) ?: fallback
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
                if (json.has("gemini_api_key")) {
                    val newKey = json.getString("gemini_api_key")
                    if (newKey.isNotBlank()) {
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(KEY_GEMINI_API, newKey)
                            .apply()
                        Log.d("RemoteConfig", "API Key updated successfully from GitHub.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Failed to fetch remote config", e)
        }
    }
}
