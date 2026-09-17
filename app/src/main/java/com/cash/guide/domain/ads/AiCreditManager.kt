package com.cash.guide.domain.ads

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiCreditManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _credits = MutableStateFlow(loadCredits())
    val credits: StateFlow<Int> = _credits.asStateFlow()

    init {
        checkAndResetDailyCredits()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun checkAndResetDailyCredits() {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_RESET_DATE, "")
        val currentPrefsCredits = prefs.getInt(KEY_CREDITS, DAILY_FREE_CREDITS)
        
        if (lastDate != today || currentPrefsCredits > DAILY_FREE_CREDITS) {
            // New day or needs correction: grant daily free credits
            prefs.edit()
                .putString(KEY_LAST_RESET_DATE, today)
                .putInt(KEY_CREDITS, DAILY_FREE_CREDITS)
                .apply()
            _credits.value = DAILY_FREE_CREDITS
        }
    }

    private fun loadCredits(): Int {
        if (IS_TEST_UNLIMITED) return 9999
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_RESET_DATE, "")
        return if (lastDate == today) {
            prefs.getInt(KEY_CREDITS, DAILY_FREE_CREDITS)
        } else {
            DAILY_FREE_CREDITS
        }
    }

    /**
     * Checks if user has at least 1 credit and consumes it. Returns true if successful.
     */
    fun consumeCredit(): Boolean {
        if (IS_TEST_UNLIMITED) return true
        checkAndResetDailyCredits()
        val current = _credits.value
        return if (current > 0) {
            val updated = current - 1
            prefs.edit().putInt(KEY_CREDITS, updated).apply()
            _credits.value = updated
            true
        } else {
            false
        }
    }

    /**
     * Adds rewarded credits (e.g. after watching a Rewarded Ad).
     */
    fun addRewardCredits(amount: Int = REWARD_CREDITS_PER_AD) {
        checkAndResetDailyCredits()
        val updated = _credits.value + amount
        prefs.edit().putInt(KEY_CREDITS, updated).apply()
        _credits.value = updated
    }

    fun getAvailableCredits(): Int {
        if (IS_TEST_UNLIMITED) return 9999
        checkAndResetDailyCredits()
        return _credits.value
    }

    companion object {
        private const val PREFS_NAME = "sarf_ai_credits"
        private const val KEY_CREDITS = "credits_count"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"

        const val DAILY_FREE_CREDITS = 3
        const val REWARD_CREDITS_PER_AD = 3
        val IS_TEST_UNLIMITED: Boolean = false

        @Volatile
        private var instance: AiCreditManager? = null

        fun getInstance(context: Context): AiCreditManager {
            return instance ?: synchronized(this) {
                instance ?: AiCreditManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
