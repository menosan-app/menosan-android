package app.menosan.android.di

import app.menosan.android.data.repo.DefaultEntryRepository
import app.menosan.android.data.repo.EntryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun entryRepository(impl: DefaultEntryRepository): EntryRepository
}
