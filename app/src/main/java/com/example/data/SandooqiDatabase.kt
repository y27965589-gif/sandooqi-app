package com.example.data

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sandooqi_state")
data class SandooqiStateEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface SandooqiDao {
    @Query("SELECT * FROM sandooqi_state WHERE id = 1")
    fun observeState(): Flow<SandooqiStateEntity?>

    @Query("SELECT * FROM sandooqi_state WHERE id = 1")
    suspend fun getStateOnce(): SandooqiStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(entity: SandooqiStateEntity)
}

@Database(entities = [SandooqiStateEntity::class], version = 1, exportSchema = false)
abstract class SandooqiDatabase : RoomDatabase() {
    abstract fun sandooqiDao(): SandooqiDao

    companion object {
        @Volatile
        private var INSTANCE: SandooqiDatabase? = null

        fun getInstance(context: Context): SandooqiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SandooqiDatabase::class.java,
                    "sandooqi_room.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
