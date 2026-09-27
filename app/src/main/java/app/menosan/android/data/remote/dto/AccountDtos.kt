package app.menosan.android.data.remote.dto

import app.menosan.android.core.network.IsoDate
import app.menosan.android.core.network.IsoInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MeDto(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val createdAt: IsoInstant,
)

@Serializable
data class CreateAccountRequest(val consent: Boolean)

@Serializable
data class CurrentWeekDto(
    val weekStart: IsoDate,
    val weekEnd: IsoDate,
    val timezone: String,
    val serverNow: IsoInstant,
)

@Serializable
data class HealthDto(val status: String, val db: String)

@Serializable
data class ErrorEnvelope(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String? = null,
    val details: JsonObject = JsonObject(emptyMap()),
)
