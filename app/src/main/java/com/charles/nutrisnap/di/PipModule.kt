package com.charles.nutrisnap.di

import com.charles.nutrisnap.feature.pip.DefaultPipSnapshotSource
import com.charles.nutrisnap.feature.pip.PipSnapshotSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PipModule {
    @Binds
    @Singleton
    abstract fun bindPipSnapshotSource(impl: DefaultPipSnapshotSource): PipSnapshotSource
}
