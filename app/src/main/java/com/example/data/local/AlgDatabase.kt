package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RunnerProfileEntity::class,
        RaceEntity::class,
        RaceResultEntity::class,
        RaceBibOverrideEntity::class,
        ActivityMetricsEntity::class,
        RaceTerrainWeatherEntity::class,
        MyChronoDirectoryEntry::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AlgDatabase : RoomDatabase() {
    abstract fun algDao(): AlgDao

    companion object {
        @Volatile
        private var INSTANCE: AlgDatabase? = null

        fun getInstance(context: Context): AlgDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlgDatabase::class.java,
                    "gaume_trail_alg_2026_v6.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
