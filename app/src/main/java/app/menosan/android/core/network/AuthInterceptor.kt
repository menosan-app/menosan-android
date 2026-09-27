package app.menosan.android.core.network

import app.menosan.android.core.auth.IdTokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokens: IdTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokens.idToken(forceRefresh = false) ?: return chain.proceed(original)

        val response = chain.proceed(original.withBearer(token))
        if (response.code != 401) return response

        val fresh = tokens.idToken(forceRefresh = true)
        if (fresh == null || fresh == token) return response
        response.close()
        return chain.proceed(original.withBearer(fresh))
    }

    private fun okhttp3.Request.withBearer(token: String) =
        newBuilder().header("Authorization", "Bearer $token").build()
}
