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
    // Le schéma de chaque version est enregistré dans app/schemas (commité par le build GitHub).
    // Pour une future version 7 : passer version = 7 et ajouter
    //   autoMigrations = [AutoMigration(from = 6, to = 7)]
    // afin que les données saisies (dossards, résultats, profils) soient conservées.
    exportSchema = true
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
                    // Plus d'effacement automatique lors d'une mise à jour : une montée de version
                    // sans migration bloque l'appli au lieu de vider la base (les données restent
                    // intactes, il suffit d'ajouter la migration). Seul un retour à une version
                    // plus ancienne repart de zéro.
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
