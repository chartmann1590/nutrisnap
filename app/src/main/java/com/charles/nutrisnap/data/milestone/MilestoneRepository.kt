package com.charles.nutrisnap.data.milestone

import com.charles.nutrisnap.data.db.MilestoneDao
import com.charles.nutrisnap.data.db.MilestoneEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MilestoneRepository @Inject constructor(
    private val milestoneDao: MilestoneDao
) {
    fun getAll(): Flow<List<MilestoneEntity>> = milestoneDao.getAll()

    suspend fun record(type: MilestoneType, payload: String = "") {
        val entity = MilestoneEntity(
            type = type.name,
            occurredAtMs = System.currentTimeMillis(),
            payload = payload
        )
        milestoneDao.insert(entity)
    }
}
