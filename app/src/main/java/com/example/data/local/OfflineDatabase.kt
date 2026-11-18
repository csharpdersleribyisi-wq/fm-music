package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.OfflineDownloadStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "offline_tracks")
data class OfflineTrackEntity(
    @PrimaryKey val trackId: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val statusName: String,
    val progressPercent: Int,
    val downloadedAt: Long
)

val OfflineTrackEntity.status: OfflineDownloadStatus
    get() = OfflineDownloadStatus.entries.firstOrNull { it.name == statusName }
        ?: OfflineDownloadStatus.NONE

@Dao
interface OfflineTrackDao {
    @Query("SELECT * FROM offline_tracks ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<OfflineTrackEntity>>

    @Query("SELECT * FROM offline_tracks WHERE trackId = :trackId LIMIT 1")
    suspend fun getById(trackId: String): OfflineTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfflineTrackEntity)

    @Query("DELETE FROM offline_tracks WHERE trackId = :trackId")
    suspend fun deleteById(trackId: String)

    @Query("DELETE FROM offline_tracks")
    suspend fun deleteAll()
}

@Database(entities = [OfflineTrackEntity::class], version = 1, exportSchema = false)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun offlineTrackDao(): OfflineTrackDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineDatabase? = null

        fun getInstance(context: Context): OfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "sonora_offline_vault.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
