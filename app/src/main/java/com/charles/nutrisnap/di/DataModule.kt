package com.charles.nutrisnap.di

import android.content.Context
import androidx.room.Room
import com.charles.nutrisnap.data.db.AppDatabase
import com.charles.nutrisnap.data.db.BadgeDao
import com.charles.nutrisnap.data.db.ChatDao
import com.charles.nutrisnap.data.db.DailyChallengeDao
import com.charles.nutrisnap.data.db.MealDao
import com.charles.nutrisnap.data.db.MilestoneDao
import com.charles.nutrisnap.data.db.WeightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "nutrisnap.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()

    @Provides
    @Singleton
    fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()

    @Provides
    @Singleton
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()

    @Provides
    @Singleton
    fun provideBadgeDao(db: AppDatabase): BadgeDao = db.badgeDao()

    @Provides
    @Singleton
    fun provideDailyChallengeDao(db: AppDatabase): DailyChallengeDao = db.dailyChallengeDao()

    @Provides
    @Singleton
    fun provideMilestoneDao(db: AppDatabase): MilestoneDao = db.milestoneDao()
}
