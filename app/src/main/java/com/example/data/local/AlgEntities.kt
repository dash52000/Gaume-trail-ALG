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

@Entity(tableName = "races")
data class RaceEntity(
    @PrimaryKey val raceId: String, // e.g., "2026_round_01"
    val seasonYear: Int,
    val roundNumber: Int,
    val myChronoEventId: String,
    val title: String,
    val village: String,
    val dateIso: String, // YYYY-MM-DD
    val distanceKm: Double,
    val elevationGainMeters: Int,
    val isCompleted: Boolean,
    val isSeasonFinale: Boolean = false
)

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
    val categoryTotal: Int,
    val challengePoints: Int           // Points officiels du Challenge (ex. 1000 pts au 1er)
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
    val elevationGainMeters: Int,
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
    val temperatureC: Int? = 14,
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
