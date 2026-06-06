package com.example.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.model.ProfileEntity
import com.example.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OnboardingUiState
 * ─────────────────
 * Holds form inputs, errors, loading flags, and completion states
 * for the step-by-step profile wizard.
 */
data class OnboardingUiState(
    val currentStep: Int = 0, // 0: Shop Details, 1: Location, 2: Plan Selection
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null,

    // Inputs
    val ownerName: String = "",
    val storeName: String = "",
    val businessType: String = "",
    val pincode: String = "",
    val city: String = "",
    val state: String = "",
    val plan: String = "pro",

    // Field-level Errors
    val ownerNameError: String? = null,
    val storeNameError: String? = null,
    val businessTypeError: String? = null,
    val pincodeError: String? = null,
    val cityError: String? = null,
    val stateError: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private inline fun <reified T : Any> createFake(): T {
            return java.lang.reflect.Proxy.newProxyInstance(
                T::class.java.classLoader,
                arrayOf(T::class.java)
            ) { _, method, _ ->
                val returnType = method.returnType
                val genericReturnType = method.genericReturnType
                val genericString = genericReturnType.toString()
                if (returnType.isAssignableFrom(kotlinx.coroutines.flow.Flow::class.java) ||
                    returnType.isAssignableFrom(kotlinx.coroutines.flow.StateFlow::class.java)) {
                    if (genericString.contains("Int") || genericString.contains("Integer")) {
                        kotlinx.coroutines.flow.MutableStateFlow(0)
                    } else if (genericString.contains("Boolean")) {
                        kotlinx.coroutines.flow.MutableStateFlow(false)
                    } else {
                        kotlinx.coroutines.flow.MutableStateFlow(emptyList<Any>())
                    }
                } else if (returnType == Result::class.java) {
                    Result.success(Unit)
                } else if (returnType == Boolean::class.javaPrimitiveType || returnType == Boolean::class.java) {
                    false
                } else if (returnType == Int::class.javaPrimitiveType || returnType == Int::class.java) {
                    0
                } else if (returnType == Long::class.javaPrimitiveType || returnType == Long::class.java) {
                    0L
                } else if (returnType == List::class.java) {
                    emptyList<Any>()
                } else {
                    null
                }
            } as T
        }
    }

    constructor(application: android.app.Application) : this(
        authRepository = createFake(),
        profileRepository = createFake()
    )

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ── Input setters ────────────────────────────────────────────────────────

    fun onOwnerNameChanged(value: String) =
        _uiState.update { it.copy(ownerName = value, ownerNameError = null) }

    fun onStoreNameChanged(value: String) =
        _uiState.update { it.copy(storeName = value, storeNameError = null) }

    fun onBusinessTypeChanged(value: String) =
        _uiState.update { it.copy(businessType = value, businessTypeError = null) }

    fun onPincodeChanged(value: String) {
        val clean = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(pincode = clean, pincodeError = null) }
    }

    fun onCityChanged(value: String) =
        _uiState.update { it.copy(city = value, cityError = null) }

    fun onStateChanged(value: String) =
        _uiState.update { it.copy(state = value, stateError = null) }

    fun onPlanSelected(value: String) =
        _uiState.update { it.copy(plan = value) }

    fun clearError() =
        _uiState.update { it.copy(errorMessage = null) }

    // ── Step Navigation ──────────────────────────────────────────────────────

    fun nextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            0 -> {
                if (validateStepZero(state)) {
                    _uiState.update { it.copy(currentStep = 1) }
                }
            }
        }
    }

    fun previousStep() {
        val step = _uiState.value.currentStep
        if (step > 0) {
            _uiState.update { it.copy(currentStep = step - 1) }
        }
    }

    // ── Save Profile ─────────────────────────────────────────────────────────

    fun completeOnboarding(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!validateStepOne(state)) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "User session not found. Please log in again.") }
                return@launch
            }

            // Resolve existing logical store_id or generate one if missing
            val storeId = try {
                val existing = profileRepository.getProfile(currentUser.id).getOrNull()
                existing?.storeId ?: java.util.UUID.randomUUID().toString()
            } catch (e: Exception) {
                java.util.UUID.randomUUID().toString()
            }

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
            val isoString = sdf.format(java.util.Date())

            val profile = ProfileEntity(
                id = currentUser.id,
                storeId = storeId,
                ownerName = state.ownerName.trim(),
                storeName = state.storeName.trim(),
                phone = currentUser.phone,
                pincode = state.pincode,
                city = state.city.trim(),
                state = state.state.trim(),
                businessType = state.businessType,
                plan = "pro",
                onboardedAt = isoString,
                updatedAt = System.currentTimeMillis()
            )

            profileRepository.saveProfile(profile)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isCompleted = true) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to save profile details") }
                }
        }
    }

    // ── Validation ───────────────────────────────────────────────────────────

    private fun validateStepZero(state: OnboardingUiState): Boolean {
        val ownerErr = if (state.ownerName.isBlank()) "Owner name is required" else null
        val storeErr = if (state.storeName.isBlank()) "Store name is required" else null
        val typeErr = if (state.businessType.isBlank()) "Business type is required" else null
        
        _uiState.update { 
            it.copy(
                ownerNameError = ownerErr, 
                storeNameError = storeErr, 
                businessTypeError = typeErr
            ) 
        }
        return ownerErr == null && storeErr == null && typeErr == null
    }

    private fun validateStepOne(state: OnboardingUiState): Boolean {
        val pinErr = when {
            state.pincode.isBlank() -> "Pincode is required"
            state.pincode.length < 6 -> "Must be exactly 6 digits"
            else -> null
        }
        val cityErr = if (state.city.isBlank()) "City is required" else null
        val stateErr = if (state.state.isBlank()) "State is required" else null

        _uiState.update { 
            it.copy(
                pincodeError = pinErr, 
                cityError = cityErr, 
                stateError = stateErr
            ) 
        }
        return pinErr == null && cityErr == null && stateErr == null
    }

    /** Helper function to map Indian Pincode prefixes to Cities/States */
    private fun lookupPincode(pin: String): Pair<String?, String?> {
        val prefix = pin.take(2)
        return when (prefix) {
            "11" -> "New Delhi" to "Delhi"
            "40" -> "Mumbai" to "Maharashtra"
            "56" -> "Bengaluru" to "Karnataka"
            "60" -> "Chennai" to "Tamil Nadu"
            "70" -> "Kolkata" to "West Bengal"
            "50" -> "Hyderabad" to "Telangana"
            "38" -> "Ahmedabad" to "Gujarat"
            "20" -> "Kanpur" to "Uttar Pradesh"
            "30" -> "Jaipur" to "Rajasthan"
            "46" -> "Bhopal" to "Madhya Pradesh"
            "78" -> "Guwahati" to "Assam"
            else -> null to null
        }
    }
}
