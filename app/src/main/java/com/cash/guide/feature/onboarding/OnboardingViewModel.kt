package com.cash.guide.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.SettingsRepository
import com.cash.guide.domain.MoneyUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val selectedLanguage: String = "fr",
    val userName: String = "",
    val selectedCurrency: MoneyUnit = MoneyUnit.DIRHAM,
    val isCompleted: Boolean = false
)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initialLang = settingsRepository.appLanguage.first()
            val initialName = settingsRepository.userName.first()
            val initialCurrency = settingsRepository.defaultCurrency.first()
            _uiState.value = _uiState.value.copy(
                selectedLanguage = initialLang,
                userName = initialName,
                selectedCurrency = initialCurrency
            )
        }
    }

    fun selectLanguage(langCode: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = langCode)
        viewModelScope.launch {
            settingsRepository.setAppLanguage(langCode)
        }
    }

    fun updateUserName(name: String) {
        _uiState.value = _uiState.value.copy(userName = name)
    }

    fun selectCurrency(currency: MoneyUnit) {
        _uiState.value = _uiState.value.copy(selectedCurrency = currency)
    }

    fun completeOnboarding(onFinish: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            settingsRepository.setAppLanguage(state.selectedLanguage)
            if (state.userName.isNotBlank()) {
                settingsRepository.setUserName(state.userName.trim())
            }
            settingsRepository.setDefaultCurrency(state.selectedCurrency)
            settingsRepository.setHasSeenOnboarding(true)
            onFinish()
        }
    }

    fun skipOnboarding(onFinish: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setHasSeenOnboarding(true)
            onFinish()
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(settingsRepository) as T
        }
    }
}
