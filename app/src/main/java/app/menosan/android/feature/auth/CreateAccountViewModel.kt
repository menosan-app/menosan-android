package app.menosan.android.feature.auth

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.menosan.android.R
import app.menosan.android.core.auth.AuthService
import app.menosan.android.core.network.ApiError
import app.menosan.android.core.network.ApiErrorCode
import app.menosan.android.core.network.ApiResult
import app.menosan.android.data.repo.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CreateAccountPhase { Idle, SigningIn, Creating }

enum class CreateAccountOutcome {
    Created,

    AlreadyExisted,
}

data class CreateAccountUiState(
    val signedInEmail: String? = null,
    val consentChecked: Boolean = false,
    val phase: CreateAccountPhase = CreateAccountPhase.Idle,
    val slowServer: Boolean = false,
    @field:StringRes val errorRes: Int? = null,
    val requestId: String? = null,
    val outcome: CreateAccountOutcome? = null,
) {
    val busy: Boolean get() = phase != CreateAccountPhase.Idle
    val canSubmit: Boolean get() = consentChecked && !busy
}

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val auth: AuthService,
    private val google: GoogleSignInClient,
    private val accounts: AccountRepository,
    private val signOut: SignOutAction,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateAccountUiState(signedInEmail = auth.currentUser?.email))
    val state: StateFlow<CreateAccountUiState> = _state.asStateFlow()

    fun onConsentChange(checked: Boolean) = _state.update { it.copy(consentChecked = checked, errorRes = null) }

    fun onContinue(activityContext: Context) {
        if (!_state.value.canSubmit) return
        _state.update { it.copy(errorRes = null, requestId = null) }
        viewModelScope.launch {
            if (auth.currentUser == null) {
                _state.update { it.copy(phase = CreateAccountPhase.SigningIn) }
                val result = google.requestIdToken(activityContext)
                if (result !is GoogleSignInResult.Success) {
                    _state.update { it.copy(phase = CreateAccountPhase.Idle, errorRes = result.failureMessage()) }
                    return@launch
                }
                val user = runCatching { auth.signInWithGoogleIdToken(result.idToken) }.getOrNull()
                if (user == null) {
                    _state.update { it.copy(phase = CreateAccountPhase.Idle, errorRes = R.string.sign_in_error_firebase) }
                    return@launch
                }
                _state.update { it.copy(signedInEmail = user.email) }
            }
            create()
        }
    }

    fun onOutcomeHandled() = _state.update { it.copy(outcome = null) }

    fun useDifferentAccount() {
        viewModelScope.launch {
            signOut()
            _state.update { it.copy(signedInEmail = null) }
        }
    }

    private suspend fun create() {
        _state.update { it.copy(phase = CreateAccountPhase.Creating, slowServer = false) }
        val slowTimer = viewModelScope.launch {
            delay(SLOW_SERVER_AFTER_MS)
            _state.update { it.copy(slowServer = true) }
        }
        val result = accounts.createAccount()
        slowTimer.cancel()
        when (result) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    phase = CreateAccountPhase.Idle,
                    outcome = if (result.status == 201) CreateAccountOutcome.Created else CreateAccountOutcome.AlreadyExisted,
                )
            }
            is ApiResult.Failure -> handleFailure(result.error)
        }
    }

    private suspend fun handleFailure(error: ApiError) {
        val message = when {
            error is ApiError.Network -> R.string.error_offline
            error is ApiError.Http && error.code == ApiErrorCode.UNAUTHENTICATED -> {
                signOut()
                R.string.sign_in_error_expired
            }
            else -> R.string.error_server
        }
        _state.update {
            it.copy(
                phase = CreateAccountPhase.Idle,
                slowServer = false,
                signedInEmail = auth.currentUser?.email,
                errorRes = message,
                requestId = (error as? ApiError.Http)?.requestId,
            )
        }
    }

    private companion object {
        const val SLOW_SERVER_AFTER_MS = 5_000L
    }
}
