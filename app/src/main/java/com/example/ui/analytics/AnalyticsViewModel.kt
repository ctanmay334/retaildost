package com.example.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.SessionManager
import com.example.data.repository.AnalyticsRepository
import com.example.data.repository.StoreAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(val analytics: StoreAnalytics) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

sealed class AiInsightsUiState {
    object Idle : AiInsightsUiState()
    object Loading : AiInsightsUiState()
    data class Success(val insights: String) : AiInsightsUiState()
    data class Error(val message: String) : AiInsightsUiState()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _aiInsightsState = MutableStateFlow<AiInsightsUiState>(AiInsightsUiState.Idle)
    val aiInsightsState: StateFlow<AiInsightsUiState> = _aiInsightsState.asStateFlow()

    fun loadAnalytics() {
        val userId = sessionManager.getSavedUserId() ?: run {
            _uiState.value = AnalyticsUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Loading
            analyticsRepository.getStoreAnalytics(userId)
                .onSuccess { stats ->
                    _uiState.value = AnalyticsUiState.Success(stats)
                    // Automatically generate insights for all accounts
                    generateAiInsights(stats)
                }
                .onFailure { error ->
                    _uiState.value = AnalyticsUiState.Error(error.localizedMessage ?: "Failed to load metrics")
                }
        }
    }

    fun generateAiInsights(analytics: StoreAnalytics) {
        viewModelScope.launch {
            _aiInsightsState.value = AiInsightsUiState.Loading
            analyticsRepository.getAiInsights(analytics)
                .onSuccess { insights ->
                    _aiInsightsState.value = AiInsightsUiState.Success(insights)
                }
                .onFailure { error ->
                    _aiInsightsState.value = AiInsightsUiState.Error(error.localizedMessage ?: "Failed to generate AI insights")
                }
        }
    }

    fun upgradeToPro() {
        val userId = sessionManager.getSavedUserId() ?: return
        viewModelScope.launch {
            analyticsRepository.upgradeToProPlan(userId)
                .onSuccess {
                    // Reload analytics so the plan tier updates to pro immediately
                    loadAnalytics()
                }
                .onFailure {
                    // Log or handle error
                }
        }
    }
}
