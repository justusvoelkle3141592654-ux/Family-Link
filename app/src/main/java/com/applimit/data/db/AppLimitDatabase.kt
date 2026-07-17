package com.applimit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun toCategory(value: String): AppCategory = AppCategory.valueOf(value)

    @TypeConverter
    fun fromCategory(category: AppCategory): String = category.name
}

@Database(entities = [ManagedApp::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppLimitDatabase : RoomDatabase() {
    abstract fun managedAppDao(): ManagedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppLimitDatabase? = null

        fun get(context: Context): AppLimitDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppLimitDatabase::class.java,
                    "app_limit.db",
                ).build().also { INSTANCE = it }
            }
    }
}
