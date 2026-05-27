package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchRepository(private val watchDao: WatchDao) {

    // Access the full statically loaded catalog
    val watchCatalog: List<WatchItem> = WatchCatalog.watches

    // Flow of favorited watch items
    val favoriteWatchesFlow: Flow<List<WatchItem>> = watchDao.getFavoritesFlow().map { entities ->
        val favIds = entities.map { it.watchId }.toSet()
        watchCatalog.filter { it.id in favIds }
    }

    // Checking favorites directly
    suspend fun isWatchFavorited(watchId: String): Boolean {
        return watchDao.isFavorite(watchId)
    }

    suspend fun toggleFavorite(watchId: String) {
        if (watchDao.isFavorite(watchId)) {
            watchDao.removeFavorite(watchId)
        } else {
            watchDao.addFavorite(FavoriteWatchEntity(watchId = watchId))
        }
    }

    // Profile Actions
    val userProfileFlow: Flow<UserProfileEntity> = watchDao.getProfileFlow().map {
        it ?: UserProfileEntity() // Default profile if none loaded
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        watchDao.saveProfile(profile)
    }

    suspend fun triggerCloudBackup() {
        // Mock sync state mapping representing Firebase Real-Time sync
        val updatedSync = UserProfileEntity(
            isCloudSynced = true,
            lastSyncTime = System.currentTimeMillis()
        )
        watchDao.saveProfile(updatedSync)
    }
}
