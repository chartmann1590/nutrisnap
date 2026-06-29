package com.charles.nutrisnap.data.badge

import com.charles.nutrisnap.data.db.BadgeDao
import com.charles.nutrisnap.data.db.BadgeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepository @Inject constructor(
    private val badgeDao: BadgeDao
) {
    fun getAll(): Flow<List<BadgeEntity>> = badgeDao.getAll()

    suspend fun awardIfNew(badgeType: BadgeType) {
        val entity = BadgeEntity(
            badgeType = badgeType.name,
            earnedAtMs = System.currentTimeMillis(),
            seen = false
        )
        badgeDao.insertOrIgnore(entity)
    }

    suspend fun markSeen(badgeType: BadgeType) {
        badgeDao.markSeen(badgeType.name)
    }
}
