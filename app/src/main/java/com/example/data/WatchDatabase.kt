package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_watches")
data class FavoriteWatchEntity(
    @PrimaryKey val watchId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "Vector Enthusiast",
    val statusText: String = "Chronos Collector",
    val syncEnabled: Boolean = true,
    val isCloudSynced: Boolean = true,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val accountTier: String = "Premium Pro"
)

@Dao
interface WatchDao {
    @Query("SELECT * FROM favorite_watches ORDER BY addedAt DESC")
    fun getFavoritesFlow(): Flow<List<FavoriteWatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteWatchEntity)

    @Query("DELETE FROM favorite_watches WHERE watchId = :watchId")
    suspend fun removeFavorite(watchId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_watches WHERE watchId = :watchId LIMIT 1)")
    suspend fun isFavorite(watchId: String): Boolean

    // Profile DAO Operations
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfileEntity)
}

@Database(
    entities = [FavoriteWatchEntity::class, UserProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WatchDatabase : RoomDatabase() {
    abstract fun watchDao(): WatchDao
}
