package com.applimit.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One installed app the parent has categorised. Apps not present in this table
 * are unmanaged (always allowed, counted to nothing).
 */
@Entity(tableName = "managed_apps")
data class ManagedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: AppCategory,
    /** Individual daily limit in minutes — only used for [AppCategory.LIMIT]. */
    val individualLimitMinutes: Int = 30,
    /** Only for [AppCategory.PLUS]: does this app's time count to the GLOBAL limit? */
    val plusCountsToGlobal: Boolean = false,
)
