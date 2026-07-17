package com.applimit.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedAppDao {

    @Query("SELECT * FROM managed_apps")
    fun observeAll(): Flow<List<ManagedApp>>

    @Query("SELECT * FROM managed_apps")
    suspend fun getAll(): List<ManagedApp>

    @Query("SELECT * FROM managed_apps WHERE category = :category")
    suspend fun getByCategory(category: AppCategory): List<ManagedApp>

    @Query("SELECT * FROM managed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): ManagedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: ManagedApp)

    @Query("DELETE FROM managed_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
