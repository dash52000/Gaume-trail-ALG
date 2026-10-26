package com.example.data.local

import androidx.room.ColumnInfo
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
    val avatarColorHex: Long
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
    val finishersCount: Int? = null,
    /**
     * Manche du Challenge (true) ou course « Hors challenge » publiée par MyChrono (false).
     * Une course hors challenge ne compte jamais pour le challenge (ni Top 15, ni classement).
     */
    @ColumnInfo(defaultValue = "1")
    val isChallenge: Boolean = true,
    /** Libellé de la compétition MyChrono (« Challenge ALG 2026 », « Hors challenge ALG 2026 »). */
    val competitionLabel: String? = null,
    /** Libellés réels des distances hors challenge (ex. « 14 km » et « 7 km ») ; null pour une manche. */
    val longDistanceLabel: String? = null,
    val shortDistanceLabel: String? = null
) {
    /** Libellé affiché pour la grande (10) ou la petite (5) distance de cette course. */
    fun distanceNameFor(challengeDistanceKm: Int): String =
        if (challengeDistanceKm == 5) shortDistanceLabel ?: "5 km" else longDistanceLabel ?: "10 km"

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
    val challengePoints: Int,          // Points officiels du Challenge (1200 pts au 1er) ; 0 hors challenge
    /** Libellé réel de la distance courue (« 10 km », ou « 14 km » sur une course hors challenge). */
    val distanceName: String? = null,
    /** Longueur réelle de la distance courue, en km. */
    val distanceKm: Double? = null
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
    /** Terrain noté par le coureur ("Sec", "Souple", "Boueux", "Très gras") ; "" si non renseigné. */
    val terrainCondition: String = "",
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

/** Dossards d'un profil pour une saison donnée (les dossards changent d'une saison à l'autre). */
@Entity(
    tableName = "season_bibs",
    primaryKeys = ["profileId", "seasonYear"]
)
data class SeasonBibEntity(
    val profileId: String,
    val seasonYear: Int,
    val bib10km: String = "",
    val bib5km: String = ""
)

/**
 * Résultat brut publié par MyChrono (tous les coureurs), gardé sur le téléphone :
 * une manche close ne change plus, il n'est donc pas nécessaire de la retélécharger.
 */
@Entity(
    tableName = "mychrono_rows",
    primaryKeys = ["raceId", "distanceLabel", "rowIndex"]
)
data class MyChronoRowEntity(
    val raceId: String,
    /** "10 km" (grande distance) ou "5 km" (petite distance), y compris hors challenge. */
    val distanceLabel: String,
    val rowIndex: Int,
    val seasonYear: Int,
    val isChallenge: Boolean,
    /** Libellé réel publié (« 10 km », « 14 km »…). */
    val distanceName: String,
    val distanceKm: Double,
    val runnerId: String,
    val bibNumber: String,
    val fullName: String,
    val club: String,
    val category: String,
    val sex: String,
    val timeSeconds: Int,
    val overallRank: Int,
    val totalFinishers: Int,
    val categoryRank: Int,
    val categoryTotal: Int,
    val points: Int
)

/** Dernier téléchargement des résultats d'une course. */
@Entity(tableName = "race_fetch_state")
data class RaceFetchStateEntity(
    @PrimaryKey val raceId: String,
    val seasonYear: Int,
    val raceDateIso: String,
    val fetchedAtEpochMillis: Long,
    val rowCount: Int
)

/**
 * Indice de difficulté d'un parcours, calculé sur les coureurs réguliers de la saison :
 * médiane, pour ces coureurs, de (allure sur ce parcours / allure habituelle).
 * 1,08 = parcours 8 % plus lent qu'un parcours moyen de la saison.
 */
@Entity(
    tableName = "course_factors",
    primaryKeys = ["raceId", "distanceLabel"]
)
data class CourseFactorEntity(
    val raceId: String,
    val distanceLabel: String,
    val seasonYear: Int,
    val factor: Double,
    val regularsCount: Int
)

/** Météo réelle au départ d'une course (Open-Meteo, modèle de réanalyse ou de prévision). */
@Entity(tableName = "race_weather")
data class RaceWeatherEntity(
    @PrimaryKey val raceId: String,
    /** Heure de départ lue dans l'horaire publié ; null si l'horaire n'en donne pas. */
    val startHourLocal: Int?,
    val temperatureC: Double?,
    val precipitationDuringMm: Double?,
    val rainPrevious72hMm: Double?,
    val windKmh: Double?,
    val weatherCode: Int?,
    val dayMinC: Double?,
    val dayMaxC: Double?,
    val source: String,
    val fetchedAtEpochMillis: Long
)

/** Objectif de saison fixé par le coureur (ex. 3 records de parcours battus en 2027). */
@Entity(
    tableName = "season_goals",
    primaryKeys = ["profileId", "seasonYear", "goalType"]
)
data class SeasonGoalEntity(
    val profileId: String,
    val seasonYear: Int,
    /** "challenge", "records", "villages", "races", "km". */
    val goalType: String,
    val target: Int
)

/** Fiche d'un village tirée de Wikipédia (résumé et lien), gardée sur le téléphone. */
@Entity(tableName = "village_info")
data class VillageInfoEntity(
    @PrimaryKey val villageKey: String,
    /** false = aucune page Wikipédia fiable trouvée (on ne réessaie pas à chaque fois). */
    val found: Boolean,
    val title: String?,
    val extract: String?,
    val pageUrl: String?,
    val fetchedAtEpochMillis: Long
)
