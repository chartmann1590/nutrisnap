package com.charles.nutrisnap.data

import com.charles.nutrisnap.data.db.WeightDao
import com.charles.nutrisnap.data.db.WeightEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val weightDao: WeightDao,
) {
    fun observeLatest(): Flow<WeightEntryEntity?> = weightDao.observeLatest()

    fun observeRange(startDay: Long, endDay: Long): Flow<List<WeightEntryEntity>> =
        weightDao.observeRange(startDay, endDay)

    suspend fun logWeight(entry: WeightEntryEntity): Long =
        weightDao.upsert(entry)

    suspend fun deleteWeight(id: Long) = weightDao.deleteById(id)
}
