package com.charles.nutrisnap.di

import com.charles.nutrisnap.ai.GemmaEngine
import com.charles.nutrisnap.ai.LiteRtGemmaEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    companion object {
        @Provides
        @Singleton
        fun provideGemmaEngine(engine: LiteRtGemmaEngine): GemmaEngine = engine
    }
}
