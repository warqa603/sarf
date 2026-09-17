package com.cash.guide.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.R
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.SettingsRepository
import com.cash.guide.data.backup.BackupManager
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.domain.export.ExcelExportHelper
import com.cash.guide.domain.export.FileExportManager
import com.cash.guide.domain.export.PdfExportHelper
import com.cash.guide.ui.notebook.JournalThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.cash.guide.data.SecurityRepository
import com.cash.guide.domain.BiometricHelper
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val calculationRepository: CalculationRepository? = null,
    private val securityRepository: SecurityRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.appLanguage,
                settingsRepository.defaultCurrency,
                settingsRepository.journalTheme
            ) { lang, currency, theme ->
                _uiState.update {
                    it.copy(
                        currentLanguage = lang,
                        defaultCurrency = currency,
                        selectedTheme = theme,
                        isLoading = false
                    )
                }
            }.collect {}
        }

        securityRepository?.let { secRepo ->
            viewModelScope.launch {
                combine(
                    secRepo.isLockEnabled,
                    secRepo.useBiometrics,
                    secRepo.hasPinSet,
                    secRepo.lockTimeoutSeconds
                ) { isLock, useBio, hasPin, timeout ->
                    _uiState.update {
                        it.copy(
                            isLockEnabled = isLock,
                            useBiometrics = useBio,
                            hasPinSet = hasPin,
                            lockTimeoutSeconds = timeout
                        )
                    }
                }.collect {}
            }
        }
    }

    fun selectLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(languageCode)
            val appLocaleTag = when (languageCode) {
                "dar" -> "ar-MA"
                "ar" -> "ar"
                "en" -> "en"
                else -> "fr"
            }
            val locale = java.util.Locale.forLanguageTag(appLocaleTag)
            java.util.Locale.setDefault(locale)
            val appLocale = LocaleListCompat.forLanguageTags(appLocaleTag)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun selectDefaultCurrency(unit: MoneyUnit) {
        viewModelScope.launch {
            settingsRepository.setDefaultCurrency(unit)
        }
    }

    fun selectTheme(themeId: JournalThemeId) {
        viewModelScope.launch {
            settingsRepository.setJournalTheme(themeId)
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBackup = true) }
            val success = try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    backupManager.writeBackupToStream(stream)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _uiState.update { it.copy(isProcessingBackup = false) }
            }
            onResult(success)
        }
    }

    fun shareBackup(context: Context, chooserTitle: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBackup = true) }
            val success = try {
                val shareUri = backupManager.createShareableBackupFile(context)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Warqa Backup (.calc)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _uiState.update { it.copy(isProcessingBackup = false) }
            }
            onResult(success)
        }
    }

    fun loadBackupForInspection(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBackup = true) }
            try {
                val result = context.contentResolver.openInputStream(uri)?.use { stream ->
                    backupManager.readBackupFromStream(stream)
                }
                val payload = result?.getOrNull()
                if (payload != null) {
                    _uiState.update { it.copy(restoreCandidate = payload, isProcessingBackup = false) }
                    onResult(true)
                } else {
                    _uiState.update { it.copy(isProcessingBackup = false) }
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isProcessingBackup = false) }
                onResult(false)
            }
        }
    }

    fun confirmRestore(replaceExisting: Boolean, onResult: (Boolean) -> Unit) {
        val payload = _uiState.value.restoreCandidate ?: run {
            onResult(false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBackup = true) }
            val success = try {
                backupManager.restore(payload, replaceExisting)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _uiState.update { it.copy(restoreCandidate = null, isProcessingBackup = false) }
            }
            onResult(success)
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(restoreCandidate = null) }
    }

    fun exportAllToExcel(context: Context, onResult: (Boolean, Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBackup = true) }
            var hasCalculations = true
            val success = try {
                val calcs = calculationRepository?.getAllSaved() ?: emptyList()
                if (calcs.isEmpty()) {
                    hasCalculations = false
                    false
                } else {
                    val groups = calculationRepository?.getAllGroups() ?: emptyList()
                    val groupMap = groups.associate { it.id to it.name }
                    val csvFile = withContext(Dispatchers.IO) {
                        ExcelExportHelper.exportAllCalculations(context, calcs, groupMap)
                    }
                    FileExportManager.shareFile(
                        context = context,
                        file = csvFile,
                        mimeType = FileExportManager.MIME_CSV,
                        subject = context.getString(R.string.export_all_excel)
                    )
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _uiState.update { it.copy(isProcessingBackup = false) }
            }
            onResult(success, hasCalculations)
        }
    }

    fun exportAllToPdf(context: Context, isRtl: Boolean, onResult: (Boolean, Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBackup = true) }
            var hasCalculations = true
            val success = try {
                val calcs = calculationRepository?.getAllSaved() ?: emptyList()
                if (calcs.isEmpty()) {
                    hasCalculations = false
                    false
                } else {
                    val groups = calculationRepository?.getAllGroups() ?: emptyList()
                    val groupMap = groups.associate { it.id to it.name }
                    val pdfFile = withContext(Dispatchers.IO) {
                        PdfExportHelper.exportAllCalculationsPdf(context, calcs, groupMap, isRtl)
                    }
                    FileExportManager.shareFile(
                        context = context,
                        file = pdfFile,
                        mimeType = FileExportManager.MIME_PDF,
                        subject = context.getString(R.string.export_all_pdf)
                    )
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _uiState.update { it.copy(isProcessingBackup = false) }
            }
            onResult(success, hasCalculations)
        }
    }

    fun reloadSampleData(context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.cash.guide.data.DataSeeder.seedCleanData(context)
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityRepository?.setLockEnabled(enabled)
        }
    }

    fun setUseBiometrics(use: Boolean) {
        viewModelScope.launch {
            securityRepository?.setUseBiometrics(use)
        }
    }

    fun setLockTimeoutSeconds(seconds: Int) {
        viewModelScope.launch {
            securityRepository?.setLockTimeoutSeconds(seconds)
        }
    }

    fun savePin(pin: String) {
        viewModelScope.launch {
            securityRepository?.setPin(pin)
            securityRepository?.setLockEnabled(true)
        }
    }

    fun disableLock() {
        viewModelScope.launch {
            securityRepository?.setLockEnabled(false)
        }
    }

    fun checkBiometricAvailability(context: Context) {
        val available = BiometricHelper.isBiometricAvailable(context)
        _uiState.update { it.copy(isBiometricAvailable = available) }
    }
}

