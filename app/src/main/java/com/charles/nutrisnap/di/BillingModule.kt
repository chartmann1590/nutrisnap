package com.charles.nutrisnap.di

import com.charles.nutrisnap.data.BillingRepository
import com.charles.nutrisnap.data.PremiumAccess
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds
    @Singleton
    abstract fun bindPremiumAccess(repository: BillingRepository): PremiumAccess
}
