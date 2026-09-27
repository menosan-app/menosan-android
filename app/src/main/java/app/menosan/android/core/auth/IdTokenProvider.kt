package app.menosan.android.core.auth

fun interface IdTokenProvider {
    fun idToken(forceRefresh: Boolean): String?
}
