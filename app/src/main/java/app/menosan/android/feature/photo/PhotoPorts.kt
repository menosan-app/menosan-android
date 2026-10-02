package app.menosan.android.feature.photo

import app.menosan.android.core.model.Taxonomy
import app.menosan.android.core.network.ApiResult
import app.menosan.android.core.network.ConnectivityObserver
import app.menosan.android.core.network.safeApiCall
import app.menosan.android.data.remote.MenosanApi
import app.menosan.android.data.remote.dto.PhotoAnalysisDto
import app.menosan.android.data.repo.TaxonomyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

sealed interface PhotoInput {
    data class Camera(val file: File) : PhotoInput

    data class Gallery(val uri: String) : PhotoInput
}

class PhotoUnreadableException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PhotoTooLargeException : Exception("The photo is too large.")

interface PhotoProcessor {
    suspend fun prepare(input: PhotoInput): ByteArray

    suspend fun crop(jpeg: ByteArray, area: CropRect): ByteArray
}

data class CaptureTarget(val file: File, val uri: String)

interface PhotoFiles {
    fun newCaptureTarget(): CaptureTarget

    fun delete(file: File)

    fun deleteAll(keep: File? = null)
}

fun interface PhotoAnalysisClient {
    suspend fun analyze(jpeg: ByteArray): ApiResult<PhotoAnalysisDto>
}

interface NetworkStatus {
    fun isOnline(): Boolean
    val online: Flow<Boolean>

    fun reportUnreachable()
}

fun interface TaxonomySource {
    suspend fun taxonomy(): Taxonomy
}

class ApiPhotoAnalysisClient @Inject constructor(private val api: MenosanApi) : PhotoAnalysisClient {
    override suspend fun analyze(jpeg: ByteArray): ApiResult<PhotoAnalysisDto> =
        safeApiCall { api.analyzePhoto(MenosanApi.imagePart(jpeg)) }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoModule {
    @Binds
    abstract fun processor(impl: AndroidPhotoProcessor): PhotoProcessor

    @Binds
    abstract fun files(impl: CachePhotoFiles): PhotoFiles

    @Binds
    abstract fun analysisClient(impl: ApiPhotoAnalysisClient): PhotoAnalysisClient

    companion object {
        @Provides
        fun networkStatus(observer: ConnectivityObserver): NetworkStatus = object : NetworkStatus {
            override fun isOnline(): Boolean = observer.isOnline()
            override val online: Flow<Boolean> = observer.online
            override fun reportUnreachable() = observer.reportUnreachable()
        }

        @Provides
        fun taxonomySource(repository: TaxonomyRepository): TaxonomySource = TaxonomySource { repository.taxonomy() }
    }
}
