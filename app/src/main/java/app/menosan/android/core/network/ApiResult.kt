package app.menosan.android.core.network

import app.menosan.android.data.remote.dto.ErrorEnvelope
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.Response
import java.io.IOException

enum class ApiErrorCode {
    UNAUTHENTICATED, ACCOUNT_NOT_FOUND, VALIDATION_FAILED, INVALID_TIMESTAMP, WEEK_CLOSED, NOT_FOUND, CONFLICT,
    ADOPTION_WINDOW_CLOSED, ANALYSIS_FAILED, NOT_WASTE, IMAGE_TOO_LARGE, RATE_LIMITED, INTERNAL,

    UNKNOWN;

    companion object {
        fun from(code: String?): ApiErrorCode = entries.firstOrNull { it.name == code } ?: UNKNOWN
    }
}

sealed interface ApiError {
    data class Http(
        val status: Int,
        val code: ApiErrorCode,
        val message: String?,
        val details: JsonObject,
        val requestId: String?,
    ) : ApiError {
        val field: String? get() = (details["field"] as? JsonPrimitive)?.content
    }

    data class Network(val cause: IOException) : ApiError

    data class Unexpected(val cause: Throwable) : ApiError
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T, val status: Int) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value), status)
    is ApiResult.Failure -> this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.value

fun ApiResult<*>.isError(code: ApiErrorCode): Boolean =
    ((this as? ApiResult.Failure)?.error as? ApiError.Http)?.code == code

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    val response = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        return ApiResult.Failure(ApiError.Network(e))
    } catch (e: Exception) {
        return ApiResult.Failure(ApiError.Unexpected(e))
    }
    if (response.isSuccessful) {
        val body = response.body()
        @Suppress("UNCHECKED_CAST")
        return when {
            body != null -> ApiResult.Success(body, response.code())
            else -> ApiResult.Success(Unit as T, response.code())
        }
    }
    return ApiResult.Failure(response.toHttpError())
}

fun Response<*>.toHttpError(): ApiError.Http {
    val raw = try {
        errorBody()?.string()
    } catch (_: IOException) {
        null
    }
    val envelope = raw?.let { runCatching { MenosanJson.decodeFromString(ErrorEnvelope.serializer(), it) }.getOrNull() }
    return ApiError.Http(
        status = code(),
        code = ApiErrorCode.from(envelope?.error?.code),
        message = envelope?.error?.message,
        details = envelope?.error?.details ?: JsonObject(emptyMap()),
        requestId = headers()[REQUEST_ID_HEADER],
    )
}

const val REQUEST_ID_HEADER = "X-Request-Id"
