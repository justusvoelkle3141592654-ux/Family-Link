package com.applimit.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One installed app the parent has categorised. Apps that are not present in
 * this table are treated as PLUS (unmanaged / free) by default.
 */
@Entity(tableName = "managed_apps")
data class ManagedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: AppCategory,
)
