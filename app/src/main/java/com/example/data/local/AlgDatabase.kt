package com.example.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RunnerProfileEntity::class,
        RaceEntity::class,
        RaceResultEntity::class,
        RaceBibOverrideEntity::class,
        ActivityMetricsEntity::class,
        RaceTerrainWeatherEntity::class,
        MyChronoDirectoryEntry::class,
        ChallengeStandingEntity::class
    ],
    version = 7,
    // Le schéma de chaque version est enregistré dans app/schemas (commité par le build GitHub).
    // Chaque montée de version passe par une migration qui conserve dossards, résultats, GPX et notes.
    autoMigrations = [
        AutoMigration(from = 6, to = 7, spec = AlgDatabase.CleanInventedDataSpec::class)
    ],
    exportSchema = true
)
abstract class AlgDatabase : RoomDatabase() {
    abstract fun algDao(): AlgDao

    /**
     * Migration 6 → 7 : garde toutes les saisies, et retire uniquement les valeurs inventées
     * par l'ancienne version (faux calendrier, D+ par défaut, identifiants MyChrono recopiés des dossards).
     */
    class CleanInventedDataSpec : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            AlgDatabase.cleanInventedData(db)
        }
    }

    companion object {
        const val DATABASE_NAME = "gaume_trail_alg_2026_v6.db"

        @Volatile
        private var INSTANCE: AlgDatabase? = null

        fun getInstance(context: Context): AlgDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlgDatabase::class.java,
                    DATABASE_NAME
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

        fun cleanInventedData(db: SupportSQLiteDatabase) {
            // Vrais identifiants MyChrono (les anciens étaient les numéros de dossard recopiés).
            db.execSQL(
                "UPDATE runner_profiles SET myChronoRunnerId = '1202' " +
                    "WHERE profileId = 'laetitia' AND myChronoRunnerId IN ('1075', '')"
            )
            db.execSQL(
                "UPDATE runner_profiles SET myChronoRunnerId = '1189' " +
                    "WHERE profileId = 'elodie' AND myChronoRunnerId IN ('1076', '')"
            )
            db.execSQL(
                "DELETE FROM mychrono_directory WHERE " +
                    "(myChronoRunnerId = '1075' AND fullName = 'Laetitia') OR " +
                    "(myChronoRunnerId = '1076' AND fullName = 'Élodie')"
            )
            // Manches du faux calendrier d'origine : leur identifiant MyChrono était "année_numéro".
            // Les vraies manches synchronisées ont un identifiant MyChrono numérique.
            db.execSQL("DELETE FROM races WHERE myChronoEventId LIKE '%\\_%' ESCAPE '\\'")
            // Aucun D+ de manche ne venait d'une source réelle (150 m par défaut ou valeur inventée).
            db.execSQL("UPDATE races SET elevationGainMeters = NULL")
            // Cadence fixée à 174 par l'ancien import GPX (jamais lue dans le fichier).
            db.execSQL("UPDATE activity_metrics SET cadenceSpm = NULL WHERE cadenceSpm = 174")
            // Température pré-remplie à 14 °C sans saisie : on ne peut pas distinguer une vraie saisie de 14 °C,
            // on la conserve donc telle quelle.
        }
    }
}
