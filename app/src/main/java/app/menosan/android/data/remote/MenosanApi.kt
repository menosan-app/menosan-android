package app.menosan.android.data.remote

import app.menosan.android.core.model.Taxonomy
import app.menosan.android.data.remote.dto.AdoptRequest
import app.menosan.android.data.remote.dto.CreateAccountRequest
import app.menosan.android.data.remote.dto.CurrentWeekDto
import app.menosan.android.data.remote.dto.EntryDto
import app.menosan.android.data.remote.dto.EntryListDto
import app.menosan.android.data.remote.dto.EntryPutRequest
import app.menosan.android.data.remote.dto.HealthDto
import app.menosan.android.data.remote.dto.MeDto
import app.menosan.android.data.remote.dto.PhotoAnalysisDto
import app.menosan.android.data.remote.dto.ReportDto
import app.menosan.android.data.remote.dto.ReportSummaryDto
import app.menosan.android.data.remote.dto.SyncRequest
import app.menosan.android.data.remote.dto.SyncResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface MenosanApi {
    @GET("health")
    suspend fun health(): Response<HealthDto>

    @GET("v1/taxonomy")
    suspend fun taxonomy(): Response<Taxonomy>

    @GET("v1/me")
    suspend fun me(): Response<MeDto>

    @POST("v1/account")
    suspend fun createAccount(@Body body: CreateAccountRequest): Response<MeDto>

    @DELETE("v1/account")
    suspend fun deleteAccount(): Response<Unit>

    @Streaming
    @GET("v1/export")
    suspend fun export(): Response<ResponseBody>

    @GET("v1/weeks/current")
    suspend fun currentWeek(): Response<CurrentWeekDto>

    @GET("v1/entries")
    suspend fun entries(@Query("weekStart") weekStart: String? = null): Response<EntryListDto>

    @PUT("v1/entries/{id}")
    suspend fun putEntry(@Path("id") id: String, @Body body: EntryPutRequest): Response<EntryDto>

    @DELETE("v1/entries/{id}")
    suspend fun deleteEntry(@Path("id") id: String): Response<Unit>

    @POST("v1/entries/sync")
    suspend fun syncEntries(@Body body: SyncRequest): Response<SyncResponse>

    @Multipart
    @POST("v1/photo-analysis")
    suspend fun analyzePhoto(@Part image: MultipartBody.Part): Response<PhotoAnalysisDto>

    @GET("v1/reports")
    suspend fun reports(): Response<List<ReportSummaryDto>>

    @GET("v1/reports/{weekStart}")
    suspend fun report(@Path("weekStart") weekStart: String): Response<ReportDto>

    @POST("v1/reports/{weekStart}/adoptions")
    suspend fun adopt(@Path("weekStart") weekStart: String, @Body body: AdoptRequest): Response<ReportDto>

    @DELETE("v1/reports/{weekStart}/adoptions/{interventionId}")
    suspend fun unadopt(
        @Path("weekStart") weekStart: String,
        @Path("interventionId") interventionId: String,
    ): Response<ReportDto>

    companion object {
        private val JPEG = "image/jpeg".toMediaType()

        fun imagePart(jpeg: ByteArray): MultipartBody.Part =
            MultipartBody.Part.createFormData("image", "photo.jpg", jpeg.toRequestBody(JPEG))
    }
}
