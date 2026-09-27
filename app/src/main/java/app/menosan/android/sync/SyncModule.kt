package app.menosan.android.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun entryRemote(impl: ApiEntryRemote): EntryRemote

    @Binds
    abstract fun transactionRunner(impl: RoomTransactionRunner): TransactionRunner

    @Binds
    abstract fun syncNotices(impl: PrefsSyncNotices): SyncNotices

    @Binds
    abstract fun syncRequester(impl: WorkManagerSyncRequester): SyncRequester

    @Multibinds
    abstract fun afterSyncActions(): Set<AfterSyncAction>
}
