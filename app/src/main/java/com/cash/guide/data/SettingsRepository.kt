package com.cash.guide.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.notebook.JournalThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hssabi_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val PINNED_CALCULATION_IDS = stringSetPreferencesKey("pinned_calculation_ids")
        val USER_NAME = stringPreferencesKey("user_name")
        val JOURNAL_THEME = stringPreferencesKey("journal_theme")
        val IS_VIP_UNLOCKED = booleanPreferencesKey("is_vip_unlocked")
        val ACTIVATED_VIP_CODE = stringPreferencesKey("activated_vip_code")
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME]?.trim() ?: ""
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name.trim()
        }
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "fr"
    }

    val defaultCurrency: Flow<MoneyUnit> = context.dataStore.data.map { preferences ->
        when (preferences[PreferencesKeys.DEFAULT_CURRENCY]) {
            "RIAL" -> MoneyUnit.RIAL
            else -> MoneyUnit.DIRHAM
        }
    }

    val pinnedCalculationIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PINNED_CALCULATION_IDS] ?: emptySet()
    }

    val journalTheme: Flow<JournalThemeId> = context.dataStore.data.map { preferences ->
        val name = preferences[PreferencesKeys.JOURNAL_THEME]
        if (name == "KRAFT_VINTAGE") {
            JournalThemeId.EMERALD_REGISTRY
        } else {
            JournalThemeId.entries.find { it.name == name } ?: JournalThemeId.CLASSIC_YELLOW
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setDefaultCurrency(unit: MoneyUnit) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CURRENCY] = unit.name
        }
    }

    suspend fun setJournalTheme(themeId: JournalThemeId) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.JOURNAL_THEME] = themeId.name
        }
    }

    suspend fun togglePinCalculation(calculationId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PINNED_CALCULATION_IDS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(calculationId)) {
                current.remove(calculationId)
            } else {
                current.add(calculationId)
            }
            preferences[PreferencesKeys.PINNED_CALCULATION_IDS] = current
        }
    }

    val isVipUnlocked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_VIP_UNLOCKED] ?: false
    }

    val activatedVipCode: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVATED_VIP_CODE]
    }

    suspend fun setVipUnlocked(unlocked: Boolean, code: String = "") {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VIP_UNLOCKED] = unlocked
            if (unlocked) {
                preferences[PreferencesKeys.ACTIVATED_VIP_CODE] = code
            } else {
                preferences.remove(PreferencesKeys.ACTIVATED_VIP_CODE)
            }
        }
    }
}
