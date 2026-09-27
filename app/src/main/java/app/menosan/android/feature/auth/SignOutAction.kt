package app.menosan.android.feature.auth

import app.menosan.android.core.auth.AuthService
import app.menosan.android.core.auth.SessionStore
import javax.inject.Inject

class SignOutAction @Inject constructor(
    private val auth: AuthService,
    private val google: GoogleSignInClient,
    private val session: SessionStore,
) {
    suspend operator fun invoke() {
        session.clear()
        auth.signOut()
        google.clearCredentialState()
    }
}
