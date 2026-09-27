package app.menosan.android.data.repo

import app.menosan.android.sync.AfterSyncAction
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object ReportSyncHooks {
    @Provides
    @IntoSet
    fun refreshReportsAfterSync(reports: ReportRepository): AfterSyncAction =
        AfterSyncAction { reports.refreshAfterSync() }
}
