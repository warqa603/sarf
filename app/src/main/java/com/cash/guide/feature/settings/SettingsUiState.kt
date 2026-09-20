package com.cash.guide.feature.settings

import com.cash.guide.data.backup.BackupPayload
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.notebook.JournalThemeId

data class SettingsUiState(
    val currentLanguage: String = "fr",
    val defaultCurrency: MoneyUnit = MoneyUnit.DIRHAM,
    val selectedTheme: JournalThemeId = JournalThemeId.WHITE_NOTEBOOK,
    val isLoading: Boolean = true,
    val isProcessingBackup: Boolean = false,
    val restoreCandidate: BackupPayload? = null,
    val isLockEnabled: Boolean = false,
    val useBiometrics: Boolean = true,
    val hasPinSet: Boolean = false,
    val lockTimeoutSeconds: Int = 0,
    val isBiometricAvailable: Boolean = false
)
