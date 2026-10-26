package com.example.data.repository

import com.example.data.local.ActivityMetricsEntity
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.local.RunnerProfileEntity
import com.example.data.local.SeasonBibEntity
import com.example.data.local.SeasonGoalEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ce que le coureur a saisi dans l'appli. Les classements MyChrono, la météo et les fiches villages
 * n'en font pas partie : ils se retéléchargent.
 */
data class UserDataSnapshot(
    val profiles: List<RunnerProfileEntity> = emptyList(),
    val seasonBibs: List<SeasonBibEntity> = emptyList(),
    val bibOverrides: List<RaceBibOverrideEntity> = emptyList(),
    val terrainNotes: List<RaceTerrainWeatherEntity> = emptyList(),
    val goals: List<SeasonGoalEntity> = emptyList(),
    val activities: List<ActivityMetricsEntity> = emptyList()
) {
    /** « 3 profils • 4 dossards de saison • … » */
    fun summary(): String = listOf(
        profiles.size to "profil(s)",
        seasonBibs.size to "dossards de saison",
        bibOverrides.size to "dossard(s) de course",
        terrainNotes.size to "note(s) de course",
        goals.size to "objectif(s)",
        activities.size to "trace(s) GPX"
    ).filter { it.first > 0 }.joinToString(" • ") { "${it.first} ${it.second}" }.ifBlank { "aucune saisie" }
}

/** Fichier de sauvegarde (JSON) des saisies, lisible et stable d'une version de l'appli à l'autre. */
object UserDataBackup {
    const val FORMAT = "gaume-trail-alg-sauvegarde"
    const val VERSION = 1

    private fun JSONObject.optIntOrNull(key: String): Int? = if (!has(key) || isNull(key)) null else optInt(key)
    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
    private fun <T> JSONArray?.mapObjects(block: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.let(block) }

    fun toJson(data: UserDataSnapshot, exportedAtEpochMillis: Long, appVersion: String): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("appVersion", appVersion)
            .put("exportedAt", exportedAtEpochMillis)
        root.put("profiles", JSONArray(data.profiles.map { p ->
            JSONObject()
                .put("profileId", p.profileId)
                .put("displayName", p.displayName)
                .put("myChronoRunnerId", p.myChronoRunnerId)
                .put("defaultBib10km", p.defaultBib10km)
                .put("defaultBib5km", p.defaultBib5km)
                .put("preferredDistanceKm", p.preferredDistanceKm)
                .put("categoryLabel", p.categoryLabel)
                .put("clubName", p.clubName)
                .put("avatarColorHex", p.avatarColorHex)
        }))
        root.put("seasonBibs", JSONArray(data.seasonBibs.map { b ->
            JSONObject()
                .put("profileId", b.profileId)
                .put("seasonYear", b.seasonYear)
                .put("bib10km", b.bib10km)
                .put("bib5km", b.bib5km)
        }))
        root.put("bibOverrides", JSONArray(data.bibOverrides.map { o ->
            JSONObject()
                .put("raceId", o.raceId)
                .put("profileId", o.profileId)
                .put("bibNumber", o.bibNumber)
                .put("distanceKm", o.distanceKm)
        }))
        root.put("terrainNotes", JSONArray(data.terrainNotes.map { t ->
            JSONObject()
                .put("raceId", t.raceId)
                .put("profileId", t.profileId)
                .put("terrainCondition", t.terrainCondition)
                .put("terrainNotes", t.terrainNotes)
                .put("isPlannedForSimulator", t.isPlannedForSimulator)
                .put("plannedDistanceKm", t.plannedDistanceKm)
                .putNullable("simulatedTargetPoints", t.simulatedTargetPoints)
        }))
        root.put("goals", JSONArray(data.goals.map { g ->
            JSONObject()
                .put("profileId", g.profileId)
                .put("seasonYear", g.seasonYear)
                .put("goalType", g.goalType)
                .put("target", g.target)
        }))
        root.put("activities", JSONArray(data.activities.map { a ->
            JSONObject()
                .put("raceId", a.raceId)
                .put("profileId", a.profileId)
                .put("sourceType", a.sourceType)
                .put("sourceTitle", a.sourceTitle)
                .put("recordedDistanceKm", a.recordedDistanceKm)
                .putNullable("elevationGainMeters", a.elevationGainMeters)
                .putNullable("avgHeartRateBpm", a.avgHeartRateBpm)
                .putNullable("maxHeartRateBpm", a.maxHeartRateBpm)
                .putNullable("cadenceSpm", a.cadenceSpm)
                .put("serializedTrackPoints", a.serializedTrackPoints)
        }))
        return root.toString(1)
    }

    /** Lit une sauvegarde ; un autre fichier est refusé (IllegalArgumentException), rien n'est deviné. */
    fun fromJson(text: String): UserDataSnapshot {
        val root = runCatching { JSONObject(text.trim()) }.getOrNull()
            ?: throw IllegalArgumentException("Ce fichier n'est pas une sauvegarde de l'appli.")
        if (root.optString("format") != FORMAT) throw IllegalArgumentException("Ce fichier n'est pas une sauvegarde de l'appli.")
        if (root.optInt("version", 0) > VERSION) {
            throw IllegalArgumentException("Sauvegarde faite par une version plus récente de l'appli : mets l'appli à jour.")
        }
        return UserDataSnapshot(
            profiles = root.optJSONArray("profiles").mapObjects { o ->
                RunnerProfileEntity(
                    profileId = o.getString("profileId"),
                    displayName = o.optString("displayName"),
                    myChronoRunnerId = o.optString("myChronoRunnerId"),
                    defaultBib10km = o.optString("defaultBib10km"),
                    defaultBib5km = o.optString("defaultBib5km"),
                    preferredDistanceKm = if (o.optInt("preferredDistanceKm", 10) == 5) 5 else 10,
                    categoryLabel = o.optString("categoryLabel"),
                    clubName = o.optString("clubName"),
                    avatarColorHex = o.optLong("avatarColorHex", 0xFF1B5E3A)
                )
            },
            seasonBibs = root.optJSONArray("seasonBibs").mapObjects { o ->
                SeasonBibEntity(o.getString("profileId"), o.getInt("seasonYear"), o.optString("bib10km"), o.optString("bib5km"))
            },
            bibOverrides = root.optJSONArray("bibOverrides").mapObjects { o ->
                RaceBibOverrideEntity(o.getString("raceId"), o.getString("profileId"), o.optString("bibNumber"), if (o.optInt("distanceKm", 10) == 5) 5 else 10)
            },
            terrainNotes = root.optJSONArray("terrainNotes").mapObjects { o ->
                RaceTerrainWeatherEntity(
                    raceId = o.getString("raceId"),
                    profileId = o.getString("profileId"),
                    terrainCondition = o.optString("terrainCondition"),
                    terrainNotes = o.optString("terrainNotes"),
                    isPlannedForSimulator = o.optBoolean("isPlannedForSimulator", false),
                    plannedDistanceKm = if (o.optInt("plannedDistanceKm", 10) == 5) 5 else 10,
                    simulatedTargetPoints = o.optIntOrNull("simulatedTargetPoints")
                )
            },
            goals = root.optJSONArray("goals").mapObjects { o ->
                SeasonGoalEntity(o.getString("profileId"), o.getInt("seasonYear"), o.getString("goalType"), o.getInt("target"))
            },
            activities = root.optJSONArray("activities").mapObjects { o ->
                ActivityMetricsEntity(
                    raceId = o.getString("raceId"),
                    profileId = o.getString("profileId"),
                    sourceType = o.optString("sourceType", "GPX"),
                    sourceTitle = o.optString("sourceTitle"),
                    recordedDistanceKm = o.optDouble("recordedDistanceKm", 0.0),
                    elevationGainMeters = o.optIntOrNull("elevationGainMeters"),
                    avgHeartRateBpm = o.optIntOrNull("avgHeartRateBpm"),
                    maxHeartRateBpm = o.optIntOrNull("maxHeartRateBpm"),
                    cadenceSpm = o.optIntOrNull("cadenceSpm"),
                    serializedTrackPoints = o.optString("serializedTrackPoints")
                )
            }
        )
    }
}
