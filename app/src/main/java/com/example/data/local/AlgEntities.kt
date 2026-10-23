package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runner_profiles")
data class RunnerProfileEntity(
    @PrimaryKey val profileId: String, // "me", "laetitia", "elodie"
    val displayName: String,
    val myChronoRunnerId: String,
    val defaultBib10km: String = "",
    val defaultBib5km: String = "",
    val preferredDistanceKm: Int = 10, // 5 or 10
    val categoryLabel: String,
    val clubName: String,
    val avatarColorHex: Long,
    val lastSyncedEpochMillis: Long? = null
)

@Entity(
    tableName = "race_bib_overrides",
    primaryKeys = ["raceId", "profileId"]
)
data class RaceBibOverrideEntity(
    val raceId: String,
    val profileId: String,
    val bibNumber: String,
    val distanceKm: Int // 5 or 10
)

/**
 * Une manche du Challenge, telle que publiée par MyChrono.
 * Toute information absente de MyChrono reste à null (jamais de valeur par défaut inventée).
 */
@Entity(tableName = "races")
data class RaceEntity(
    @PrimaryKey val raceId: String, // "alg_2026_r41" (année + numéro de manche MyChrono)
    val seasonYear: Int,
    val roundNumber: Int,
    val myChronoEventId: String,
    val title: String,
    val village: String,
    val dateIso: String, // YYYY-MM-DD
    /** Longueur réelle de la grande distance (« 10 km » du challenge), en km. */
    val distanceKm: Double,
    /** D+ de la grande distance : inconnu (null) tant qu'aucune source fiable ne le donne. */
    val elevationGainMeters: Int?,
    val isCompleted: Boolean,
    val isSeasonFinale: Boolean = false,
    /** Longueur réelle de la petite distance (« 5 km » du challenge), en km. */
    val distance5Km: Double? = null,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val address: String? = null,
    val schedule: String? = null,
    val kidsSchedule: String? = null,
    val terrainDescription: String? = null,
    val hasGarderie: Boolean? = null,
    val hasDouche: Boolean? = null,
    val hasVestiaire: Boolean? = null,
    val hasResto: Boolean? = null,
    /** Tracés publiés par l'organisateur, une ligne par lien : "libellé\turl". */
    val routeLinks: String? = null,
    val sourceUrl: String? = null,
    val statusLabel: String? = null,
    val finishersCount: Int? = null
) {
    /** Longueur réelle de la distance du challenge demandée (10 ou 5), ou null si inconnue. */
    fun realDistanceKmFor(challengeDistanceKm: Int): Double? =
        if (challengeDistanceKm == 5) distance5Km else distanceKm.takeIf { it > 0.0 }

    fun routeLinkList(): List<Pair<String, String>> =
        routeLinks.orEmpty().lines().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size == 2 && parts[1].startsWith("http")) parts[0] to parts[1] else null
        }
}

@Entity(
    tableName = "race_results",
    primaryKeys = ["raceId", "profileId", "distanceLabel"]
)
data class RaceResultEntity(
    val raceId: String,
    val profileId: String,
    val seasonYear: Int,
    val bibNumber: String,
    val distanceLabel: String = "10 km", // Strictement "10 km" ou "5 km"
    val officialTimeSeconds: Int,
    val officialTimeFormatted: String, // e.g., "41:18"
    val pacePerKmFormatted: String,    // e.g., "4:08 /km"
    val speedKmh: Double,              // e.g., 14.53
    val overallRank: Int,
    val totalFinishers: Int,
    val categoryRank: Int,
    val categoryTotal: Int,            // Nombre réel d'arrivants de la même catégorie sur cette distance
    val challengePoints: Int           // Points officiels du Challenge (1200 pts au 1er de chaque distance)
)

@Entity(
    tableName = "activity_metrics",
    primaryKeys = ["raceId", "profileId"]
)
data class ActivityMetricsEntity(
    val raceId: String,
    val profileId: String,
    val sourceType: String, // "GPX"
    val sourceTitle: String,
    val recordedDistanceKm: Double,
    /** D+ mesuré sur la trace ; null si le fichier ne contient pas d'altitudes. */
    val elevationGainMeters: Int?,
    val avgHeartRateBpm: Int?,
    val maxHeartRateBpm: Int?,
    val cadenceSpm: Int?,
    // Encodage compact "lat,lon,ele;lat,lon,ele;..." pour affichage Hors-Ligne
    val serializedTrackPoints: String
)

@Entity(
    tableName = "race_terrain_weather",
    primaryKeys = ["raceId", "profileId"]
)
data class RaceTerrainWeatherEntity(
    val raceId: String,
    val profileId: String,
    val terrainCondition: String = "Sec", // "Sec", "Souple", "Boueux", "Très gras"
    val weatherCondition: String = "Nuageux", // "Soleil", "Nuageux", "Pluie", "Vent / Frais"
    val temperatureC: Int? = null,
    val terrainNotes: String = "",
    val isPlannedForSimulator: Boolean = false,
    val plannedDistanceKm: Int = 10, // 10 or 5
    val simulatedTargetPoints: Int? = null
)

@Entity(tableName = "mychrono_directory")
data class MyChronoDirectoryEntry(
    @PrimaryKey val myChronoRunnerId: String,
    val fullName: String,
    val defaultBib10km: String = "",
    val defaultBib5km: String = "",
    val club: String,
    val category: String,
    val sampleRaceCount: Int,
    val bestPoints: Int
)

/**
 * Classement provisoire du Challenge, recalculé à partir de tous les résultats MyChrono de la saison :
 * somme des 15 meilleurs résultats sur une même distance (règlement ALG).
 */
@Entity(
    tableName = "challenge_standings",
    primaryKeys = ["seasonYear", "distanceLabel", "runnerKey"]
)
data class ChallengeStandingEntity(
    val seasonYear: Int,
    val distanceLabel: String, // "10 km" ou "5 km"
    val runnerKey: String, // runnerId MyChrono, ou "nom|dossard" s'il manque
    val myChronoRunnerId: String,
    val fullName: String,
    val category: String,
    val sex: String,
    val bibNumber: String,
    val totalPoints: Int,
    val racesCounted: Int,
    val racesRun: Int,
    val lowestCountedPoints: Int,
    val overallRank: Int,
    val categoryRank: Int,
    val computedAtEpochMillis: Long
)
