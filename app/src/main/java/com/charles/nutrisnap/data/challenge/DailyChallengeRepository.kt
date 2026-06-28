package com.charles.nutrisnap.data.challenge

import com.charles.nutrisnap.data.db.DailyChallengeDao
import com.charles.nutrisnap.data.db.DailyChallengeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyChallengeRepository @Inject constructor(
    private val dailyChallengeDao: DailyChallengeDao
) {
    fun getCompleted(): Flow<List<DailyChallengeEntity>> = dailyChallengeDao.getCompleted()

    suspend fun getForDay(dateEpochDay: Long): DailyChallengeEntity? =
        dailyChallengeDao.getForDay(dateEpochDay)

    suspend fun upsert(entity: DailyChallengeEntity) {
        dailyChallengeDao.upsert(entity)
    }
}
