package com.littlethingsandroidai.domain.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.littlethingsandroidai.core.persistence.SessionService
import com.littlethingsandroidai.service.AppDataWithAuthorizationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel(
    private val appDataService: AppDataWithAuthorizationService,
    private val sessionService: SessionService,
    private val onLoginSuccess: () -> Unit,
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _errorMessage.value = "Google token is empty."
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null

            runCatching {
                appDataService.authUseCase.executeGoogleLogin(idToken)
            }.onSuccess {
                onLoginSuccess()
            }.onFailure { throwable ->
                _errorMessage.value = throwable.message ?: "Google sign-in failed."
            }

            _loading.value = false
        }
    }

    fun signInWithMockGoogle() {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null

            sessionService.updateTokens(
                accessToken = SignInDevConfig.MOCK_ACCESS_TOKEN,
                refreshToken = SignInDevConfig.MOCK_REFRESH_TOKEN,
            )
            onLoginSuccess()

            _loading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val appDataService: AppDataWithAuthorizationService,
        private val sessionService: SessionService,
        private val onLoginSuccess: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SignInViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return SignInViewModel(
                appDataService = appDataService,
                sessionService = sessionService,
                onLoginSuccess = onLoginSuccess,
            ) as T
        }
    }
}
