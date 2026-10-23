package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.ActivityMetricsEntity
import com.example.data.local.ChallengeStandingEntity
import com.example.data.local.AlgDao
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.MyChronoDirectoryEntry
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.local.RunnerProfileEntity
import com.example.data.remote.MyChronoApiClient
import com.example.domain.ChallengeStandingsCalculator
import com.example.domain.GpxAndTrackUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AlgRepository(
    private val algDao: AlgDao,
    private val myChronoApiClient: MyChronoApiClient = MyChronoApiClient()
) {
    val runnerProfilesFlow: Flow<List<RunnerProfileEntity>> = algDao.observeRunnerProfiles()
    val allRacesFlow: Flow<List<RaceEntity>> = algDao.observeAllRaces()
    val allResultsFlow: Flow<List<RaceResultEntity>> = algDao.observeAllResults()
    val allBibOverridesFlow: Flow<List<RaceBibOverrideEntity>> = algDao.observeAllBibOverrides()
    val allActivityMetricsFlow: Flow<List<ActivityMetricsEntity>> = algDao.observeAllActivityMetrics()
    val allTerrainWeatherFlow: Flow<List<RaceTerrainWeatherEntity>> = algDao.observeAllTerrainWeather()
    val directoryFlow: Flow<List<MyChronoDirectoryEntry>> = algDao.observeDirectory()
    val standingsFlow: Flow<List<ChallengeStandingEntity>> = algDao.observeAllStandings()

    /**
     * Saisons disponibles à partir de 2026 (2026, 2027, et toute année >= 2027). Jamais 2025.
     */
    fun getAvailableSeasonsFrom2026(): List<Int> {
        val currentYear = LocalDate.now().year
        val maxYear = maxOf(2027, currentYear)
        return (2026..maxYear).toList()
    }

    /**
     * Crée les 3 profils du Trio au premier lancement et purge tout résultat sur une distance sans dossard.
     * Aucun calendrier n'est inventé : les manches viennent uniquement de MyChrono.
     */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        algDao.deletePre2026Races()
        algDao.deletePre2026Results()

        if (algDao.getRunnerProfilesSnapshot().isEmpty()) {
            algDao.upsertRunnerProfiles(GaumeInitialSeedData.buildInitialProfiles())
        }

        purgeResultsWithoutAssignedBib()
    }

    /**
     * Supprime immédiatement tout résultat (10 km ou 5 km) si le profil n'a pas de numéro de dossard
     * attribué pour ce challenge (ni sur le profil, ni sur la manche).
     */
    suspend fun purgeResultsWithoutAssignedBib() = withContext(Dispatchers.IO) {
        val profiles = algDao.getRunnerProfilesSnapshot()
        for (profile in profiles) {
            val overrides = algDao.getBibOverridesForProfile(profile.profileId)
            val has10kOverride = overrides.any { it.distanceKm == 10 && it.bibNumber.isNotBlank() }
            val has5kOverride = overrides.any { it.distanceKm == 5 && it.bibNumber.isNotBlank() }

            if (profile.defaultBib10km.isBlank() && !has10kOverride) {
                algDao.deleteResultsForProfileAndDistance(profile.profileId, "10 km")
            }
            if (profile.defaultBib5km.isBlank() && !has5kOverride) {
                algDao.deleteResultsForProfileAndDistance(profile.profileId, "5 km")
            }
        }
    }

    /**
     * Purge intégralement les résultats et relance une synchronisation propre basée uniquement sur les dossards attribués.
     */
    suspend fun resetAndResyncCleanResults(seasonYear: Int): Result<String> = withContext(Dispatchers.IO) {
        algDao.deleteAllRaceResults()
        syncSeasonFromMyChrono(maxOf(2026, seasonYear))
    }

    /**
     * Synchronise une saison depuis l'API publique MyChrono : calendrier, résultats du Trio
     * (10 km et 5 km strictement séparés) et classement provisoire de tous les coureurs.
     * Si MyChrono ne publie rien, les données locales sont laissées telles quelles.
     */
    suspend fun syncSeasonFromMyChrono(seasonYear: Int): Result<String> = withContext(Dispatchers.IO) {
        val targetYear = maxOf(2026, seasonYear)
        val profiles = algDao.getRunnerProfilesSnapshot()

        purgeResultsWithoutAssignedBib()

        myChronoApiClient.fetchSeasonFromMyChrono(targetYear).map { payload ->
            if (payload.updatedRaces.isNotEmpty()) {
                algDao.deleteRacesBySeason(targetYear)
                algDao.upsertRaces(payload.updatedRaces)
            }
            if (payload.discoveredDirectoryEntries.isNotEmpty()) {
                algDao.upsertDirectoryEntries(payload.discoveredDirectoryEntries)
            }

            val allMatchedResults = mutableListOf<RaceResultEntity>()
            for (profile in profiles) {
                val overrides = algDao.getBibOverridesForProfile(profile.profileId)
                    .associateBy { it.raceId }

                for (race in payload.updatedRaces) {
                    allMatchedResults.addAll(
                        myChronoApiClient.findMatchingResultsForProfile(
                            raceId = race.raceId,
                            seasonYear = targetYear,
                            profile = profile,
                            raceOverride = overrides[race.raceId],
                            rawRows = payload.rawRowsByRaceId[race.raceId].orEmpty()
                        )
                    )
                }
            }

            if (payload.rawRowsByRaceId.isNotEmpty()) {
                algDao.deleteResultsBySeason(targetYear)
                if (allMatchedResults.isNotEmpty()) {
                    algDao.upsertRaceResults(allMatchedResults)
                }
                val now = System.currentTimeMillis()
                algDao.deleteStandingsBySeason(targetYear)
                algDao.upsertStandings(
                    ChallengeStandingsCalculator.computeSeasonStandings(
                        seasonYear = targetYear,
                        rawRowsByRaceId = payload.rawRowsByRaceId,
                        computedAtEpochMillis = now
                    )
                )
            }

            purgeResultsWithoutAssignedBib()

            val now = System.currentTimeMillis()
            profiles.forEach { profile ->
                algDao.upsertRunnerProfile(profile.copy(lastSyncedEpochMillis = now))
            }

            val count10k = allMatchedResults.count { it.distanceLabel == "10 km" }
            val count5k = allMatchedResults.count { it.distanceLabel == "5 km" }
            if (payload.updatedRaces.isEmpty()) {
                payload.message
            } else {
                "${payload.message} ($count10k rés. 10 km • $count5k rés. 5 km)"
            }
        }
    }

    /**
     * Met à jour les dossards 10 km et 5 km et la distance par défaut d'un profil,
     * purge immédiatement toute distance sans dossard, puis synchronise MyChrono.
     */
    suspend fun updateProfileBibAndDistance(
        profileId: String,
        newDisplayName: String,
        newMyChronoId: String,
        defaultBib10km: String,
        defaultBib5km: String,
        preferredDistanceKm: Int,
        clubName: String,
        categoryLabel: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val existing = algDao.getRunnerProfile(profileId)
            ?: return@withContext Result.failure(IllegalArgumentException("Profil introuvable"))
        val cleanBib10 = defaultBib10km.trim().removePrefix("#")
        val cleanBib5 = defaultBib5km.trim().removePrefix("#")
        val resolvedPreferredDist = when {
            preferredDistanceKm == 5 && cleanBib5.isNotBlank() -> 5
            preferredDistanceKm == 10 && cleanBib10.isNotBlank() -> 10
            cleanBib5.isNotBlank() && cleanBib10.isBlank() -> 5
            cleanBib10.isNotBlank() && cleanBib5.isBlank() -> 10
            preferredDistanceKm == 5 -> 5
            else -> 10
        }

        val updated = existing.copy(
            displayName = newDisplayName.trim().ifBlank { existing.displayName },
            myChronoRunnerId = newMyChronoId.trim(),
            defaultBib10km = cleanBib10,
            defaultBib5km = cleanBib5,
            preferredDistanceKm = resolvedPreferredDist,
            clubName = clubName.trim().ifBlank { existing.clubName },
            categoryLabel = categoryLabel.trim().ifBlank { existing.categoryLabel },
            lastSyncedEpochMillis = System.currentTimeMillis()
        )
        algDao.upsertRunnerProfile(updated)
        algDao.deleteResultsForProfile(profileId)
        syncSeasonFromMyChrono(2026)
    }

    /**
     * Enregistre le numéro de dossard et la distance (5 km ou 10 km) pour une course précise
     * et interroge immédiatement l'API MyChrono pour cette course.
     */
    suspend fun setRaceBibAndDistanceAndSync(
        raceId: String,
        profileId: String,
        bibNumber: String,
        distanceKm: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanBib = bibNumber.trim().removePrefix("#")
        val cleanDist = if (distanceKm == 5) 5 else 10
        if (cleanBib.isBlank()) {
            algDao.deleteBibOverride(raceId, profileId)
            algDao.deleteResultForRaceProfileAndDistance(
                raceId = raceId,
                profileId = profileId,
                distanceLabel = if (cleanDist == 5) "5 km" else "10 km"
            )
            return@withContext Result.success("Dossard retiré sur cette manche (0 course comptabilisée).")
        }

        val overrideEntity = RaceBibOverrideEntity(
            raceId = raceId,
            profileId = profileId,
            bibNumber = cleanBib,
            distanceKm = cleanDist
        )
        algDao.upsertBibOverride(overrideEntity)

        val race = algDao.getRaceById(raceId)
            ?: return@withContext Result.failure(IllegalStateException("Course introuvable"))
        val profile = algDao.getRunnerProfile(profileId)
            ?: return@withContext Result.failure(IllegalStateException("Profil introuvable"))

        myChronoApiClient.fetchRaceRawResults(race.myChronoEventId).fold(
            onSuccess = { rows ->
                val matchedList = myChronoApiClient.findMatchingResultsForProfile(
                    raceId = race.raceId,
                    seasonYear = race.seasonYear,
                    profile = profile,
                    raceOverride = overrideEntity,
                    rawRows = rows
                )
                if (matchedList.isNotEmpty()) {
                    algDao.deleteResultForRaceAndProfile(race.raceId, profile.profileId)
                    algDao.upsertRaceResults(matchedList)
                    val first = matchedList.first()
                    Result.success(
                        "Résultat MyChrono (${first.distanceLabel}) trouvé pour le dossard #${first.bibNumber} : ${first.officialTimeFormatted} (${first.challengePoints} pts)"
                    )
                } else {
                    Result.failure(
                        IllegalStateException(
                            "Aucun résultat trouvé dans MyChrono (${race.title}) pour le dossard #$cleanBib sur $cleanDist km."
                        )
                    )
                }
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

    /**
     * Sauvegarde la météo, l'état du terrain (Sec, Souple, Boueux, Très gras) et/ou la sélection Simulateur Top 15.
     */
    suspend fun saveRaceTerrainWeather(
        raceId: String,
        profileId: String,
        seasonYear: Int,
        weatherCondition: String,
        temperatureC: Int?,
        terrainCondition: String,
        isPlannedForSimulator: Boolean,
        plannedDistanceKm: Int,
        simulatedTargetPoints: Int?,
        runnerNotes: String
    ) = withContext(Dispatchers.IO) {
        val entity = RaceTerrainWeatherEntity(
            raceId = raceId,
            profileId = profileId,
            weatherCondition = weatherCondition.ifBlank { "Nuageux" },
            temperatureC = temperatureC,
            terrainCondition = terrainCondition.ifBlank { "Sec" },
            isPlannedForSimulator = isPlannedForSimulator,
            plannedDistanceKm = if (plannedDistanceKm == 5) 5 else 10,
            simulatedTargetPoints = simulatedTargetPoints?.coerceIn(0, 1200),
            terrainNotes = runnerNotes.trim()
        )
        algDao.upsertTerrainWeather(entity)
    }

    /**
     * Bascule rapide d'une course à venir dans le Simulateur Top 15.
     */
    suspend fun togglePlannedRaceForSimulator(
        raceId: String,
        profileId: String,
        seasonYear: Int,
        distanceKm: Int,
        isPlanned: Boolean,
        defaultTargetPoints: Int
    ) = withContext(Dispatchers.IO) {
        val existing = algDao.getTerrainWeather(raceId, profileId)
        val updated = RaceTerrainWeatherEntity(
            raceId = raceId,
            profileId = profileId,
            weatherCondition = existing?.weatherCondition ?: "Nuageux",
            temperatureC = existing?.temperatureC,
            terrainCondition = existing?.terrainCondition ?: "Sec",
            isPlannedForSimulator = isPlanned,
            plannedDistanceKm = if (distanceKm == 5) 5 else 10,
            simulatedTargetPoints = existing?.simulatedTargetPoints ?: defaultTargetPoints.coerceIn(0, 1200),
            terrainNotes = existing?.terrainNotes ?: ""
        )
        algDao.upsertTerrainWeather(updated)
    }

    /**
     * Importe une trace GPX locale choisie par le coureur et l'associe à une course (>= 2026).
     */
    suspend fun importGpxTrackForRace(
        context: Context,
        raceId: String,
        profileId: String,
        uri: Uri
    ): Result<ActivityMetricsEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalArgumentException("Impossible de lire le fichier GPX")
            val race = algDao.getRaceById(raceId)
            val parsed = GpxAndTrackUtils.parseGpxXmlContent(
                xmlContent = content,
                fallbackName = race?.title ?: "Trace GPX Gaume"
            )
            val entity = ActivityMetricsEntity(
                raceId = raceId,
                profileId = profileId,
                sourceType = "GPX",
                sourceTitle = parsed.title.ifBlank { race?.title ?: "Trace GPX Gaume" },
                recordedDistanceKm = parsed.distanceKm,
                elevationGainMeters = parsed.elevationGainMeters,
                avgHeartRateBpm = parsed.avgHeartRateBpm,
                maxHeartRateBpm = parsed.maxHeartRateBpm,
                cadenceSpm = null,
                serializedTrackPoints = parsed.serializedCoordinates
            )
            algDao.upsertSingleActivityMetric(entity)
            entity
        }
    }
}
