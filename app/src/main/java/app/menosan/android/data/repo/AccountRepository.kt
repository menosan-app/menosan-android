package app.menosan.android.data.repo

import app.menosan.android.core.auth.AuthService
import app.menosan.android.core.auth.SessionStore
import app.menosan.android.core.network.ApiError
import app.menosan.android.core.network.ApiErrorCode
import app.menosan.android.core.network.ApiResult
import app.menosan.android.core.network.safeApiCall
import app.menosan.android.data.remote.MenosanApi
import app.menosan.android.data.remote.dto.CreateAccountRequest
import app.menosan.android.data.remote.dto.MeDto
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AccountStatus {
    data class Ready(val me: MeDto) : AccountStatus

    data object NeedsAccount : AccountStatus

    data class Failed(val error: ApiError) : AccountStatus
}

@Singleton
class AccountRepository @Inject constructor(
    private val api: MenosanApi,
    private val auth: AuthService,
    private val session: SessionStore,
) {
    fun hasConfirmedAccount(): Boolean = auth.currentUser?.let { session.isAccountConfirmed(it.uid) } ?: false

    suspend fun checkAccount(): AccountStatus = when (val result = safeApiCall { api.me() }) {
        is ApiResult.Success -> {
            markConfirmed()
            AccountStatus.Ready(result.value)
        }
        is ApiResult.Failure -> {
            val error = result.error
            if (error is ApiError.Http && error.code == ApiErrorCode.ACCOUNT_NOT_FOUND) {
                AccountStatus.NeedsAccount
            } else {
                AccountStatus.Failed(error)
            }
        }
    }

    suspend fun createAccount(): ApiResult<MeDto> =
        safeApiCall { api.createAccount(CreateAccountRequest(consent = true)) }.also {
            if (it is ApiResult.Success) markConfirmed()
        }

    private fun markConfirmed() {
        auth.currentUser?.let { session.markAccountConfirmed(it.uid) }
    }
}
