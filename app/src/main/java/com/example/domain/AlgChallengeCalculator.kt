package com.example.domain

import com.example.data.local.ActivityMetricsEntity
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.GaumeVillageGeo
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.local.RaceWeatherEntity
import com.example.data.local.RunnerProfileEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Élément de course combiné pour une saison, avec séparation stricte
 * entre le résultat 10 km et le résultat 5 km, la météo/terrain, la sélection Simulateur Top 15
 * et la géolocalisation GPS exacte en Gaume.
 */
data class RaceItemWithContext(
    val race: RaceEntity,
    val gaumeGeo: GaumeVillageGeo,
    val result10km: RaceResultEntity?,
    val isCountingInTop15For10km: Boolean,
    val rankAmongRunnerRaces10km: Int?,
    val pointsDeltaToEnterTop15For10km: Int?,
    val result5km: RaceResultEntity?,
    val isCountingInTop15For5km: Boolean,
    val rankAmongRunnerRaces5km: Int?,
    val pointsDeltaToEnterTop15For5km: Int?,
    val resultsByProfileAndDistance: Map<String, List<RaceResultEntity>>,
    val activityMetrics: ActivityMetricsEntity?,
    val terrainWeather: RaceTerrainWeatherEntity?,
    /** Indice de difficulté du parcours (calculé sur le peloton), par distance ; null si inconnu. */
    val courseFactor10km: Double? = null,
    val courseFactor5km: Double? = null,
    /** Même parcours la saison précédente (clé lieu + rang dans la saison). */
    val previousEditionRaceId: String? = null,
    val previousEditionFactor10km: Double? = null,
    val previousEditionFactor5km: Double? = null,
    /** Météo réelle au départ (Open-Meteo). */
    val weather: RaceWeatherEntity? = null
) {
    val myResult10km: RaceResultEntity? = result10km
    val myResult5km: RaceResultEntity? = result5km
    val myMetrics: ActivityMetricsEntity? = activityMetrics

    fun resultForDistance(distanceKm: Int): RaceResultEntity? =
        if (distanceKm == 5) result5km else result10km

    fun isCountingForDistance(distanceKm: Int): Boolean =
        if (distanceKm == 5) isCountingInTop15For5km else isCountingInTop15For10km

    fun rankForDistance(distanceKm: Int): Int? =
        if (distanceKm == 5) rankAmongRunnerRaces5km else rankAmongRunnerRaces10km

    fun pointsDeltaForDistance(distanceKm: Int): Int? =
        if (distanceKm == 5) pointsDeltaToEnterTop15For5km else pointsDeltaToEnterTop15For10km

    fun courseFactorForDistance(distanceKm: Int): Double? =
        if (distanceKm == 5) courseFactor5km else courseFactor10km

    fun previousEditionFactorForDistance(distanceKm: Int): Double? =
        if (distanceKm == 5) previousEditionFactor5km else previousEditionFactor10km

    /** Indice de ce parcours : celui de la course elle-même, sinon celui de son édition précédente. */
    fun bestKnownFactorForDistance(distanceKm: Int): Double? =
        courseFactorForDistance(distanceKm) ?: previousEditionFactorForDistance(distanceKm)
}

data class SimulatedRaceCandidate(
    val race: RaceEntity,
    val isPlanned: Boolean,
    val targetPoints: Int,
    val terrainCondition: String?
)

/**
 * Projection du Simulateur Top 15 pour une distance donnée (10 km ou 5 km).
 */
data class Top15SimulationSummary(
    val distanceKm: Int,
    val currentCountingRaces: Int,
    val currentTotalPoints: Int,
    val plannedRacesCount: Int,
    val projectedCountingRaces: Int,
    val projectedTotalPoints: Int,
    val netPointsGain: Int,
    val minimumScoreToBeatInTop15: Int,
    val willCompleteChallenge: Boolean,
    val defaultSuggestedTargetPoints: Int,
    val candidateRaces: List<SimulatedRaceCandidate>,
    val plannedRaceIdsCountingInProjectedTop15: Set<String>
) {
    val projectedCountingRacesCount: Int get() = projectedCountingRaces
    val projectedGainPoints: Int get() = netPointsGain
}

/**
 * Résumé du Challenge Top 15 pour une distance spécifique (10 km OU 5 km).
 * Si aucun dossard n'est attribué pour cette distance, completedRacesCount = 0 et totalChallengePoints = 0.
 */
data class DistanceChallengeSummary(
    val distanceKm: Int, // 10 ou 5
    val distanceLabel: String, // "10 km" ou "5 km"
    val assignedBibNumber: String, // "" si aucun dossard attribué
    val hasAssignedBib: Boolean,
    val completedRacesCount: Int,
    val countingRacesCount: Int,
    val droppedRacesCount: Int,
    val remainingToComplete15: Int,
    val isChallengeComplete: Boolean,
    val totalChallengePoints: Int,
    val averagePointsPerRace: Int,
    val lowestCountingPoints: Int,
    val bestPoints: Int,
    val bestRaceTitle: String?,
    val bestRank: Int?,
    val averagePaceFormatted: String,
    val averageSpeedKmh: Double,
    val totalDistanceRunKm: Double,
    /** Somme des D+ connus (traces GPX importées) ; voir elevationKnownRacesCount. */
    val totalElevationGainMeters: Int,
    val elevationKnownRacesCount: Int,
    val current15thRaceThresholdPoints: Int?,
    val top15RaceIds: Set<String>,
    val simulatorSummary: Top15SimulationSummary
) {
    val countingRaceIds: Set<String> get() = top15RaceIds
}

/**
 * Résumé complet d'un coureur pour une saison avec les deux classements séparés :
 * - challenge10km : Challenge 10 km (Allure Libre Longue)
 * - challenge5km  : Challenge 5 km (Allure Libre Courte)
 */
data class RunnerChallengeSummary(
    val profile: RunnerProfileEntity,
    val seasonYear: Int,
    val challenge10km: DistanceChallengeSummary,
    val challenge5km: DistanceChallengeSummary,
    val totalDistanceRunKmAllDistances: Double,
    val totalElevationGainMAllDistances: Int,
    val racesWithContext: List<RaceItemWithContext>,
    /** Forme de fin de saison précédente (allure corrigée du parcours, s/km), pour les premiers pronostics. */
    val fallbackFormPace10km: Int? = null,
    val fallbackFormPace5km: Int? = null
) {
    fun fallbackFormPaceForDistance(distanceKm: Int): Int? =
        if (distanceKm == 5) fallbackFormPace5km else fallbackFormPace10km

    val simulator10km: Top15SimulationSummary get() = challenge10km.simulatorSummary
    val simulator5km: Top15SimulationSummary get() = challenge5km.simulatorSummary

    fun challengeForDistance(distanceKm: Int): DistanceChallengeSummary =
        if (distanceKm == 5) challenge5km else challenge10km

    fun summaryForDistance(distanceKm: Int): DistanceChallengeSummary =
        challengeForDistance(distanceKm)

    fun simulatorForDistance(distanceKm: Int): Top15SimulationSummary =
        challengeForDistance(distanceKm).simulatorSummary

}

/**
 * Comparaison inter-saisons (ex: 2025 vs 2026) sur un même parcours et une même distance.
 */
data class SameCourseYearComparison(
    val courseTitle: String,
    val village: String,
    val distanceLabel: String,
    val yearA: Int,
    val resultSeasonA: RaceResultEntity?,
    val yearB: Int,
    val resultSeasonB: RaceResultEntity?,
    val timeDeltaSeconds: Int?,
    val pointsDelta: Int?,
    val sortDateIso: String = ""
)

object AlgChallengeCalculator {
    const val MAX_COUNTING_RACES = 15

    fun formatSignedTimeDelta(deltaSeconds: Int): String {
        val sign = if (deltaSeconds < 0) "-" else "+"
        val absSec = abs(deltaSeconds)
        val m = absSec / 60
        val s = absSec % 60
        return if (m > 0) "$sign${m}m${"%02d".format(s)}s" else "$sign${s}s"
    }

    fun computeChallengeSummary(
        profile: RunnerProfileEntity,
        seasonYear: Int,
        racesInSeason: List<RaceEntity>,
        allSeasonResults: List<RaceResultEntity>,
        allActivityMetrics: List<ActivityMetricsEntity>,
        allTerrainWeather: List<RaceTerrainWeatherEntity> = emptyList(),
        allBibOverrides: List<RaceBibOverrideEntity> = emptyList(),
        allProfiles: List<RunnerProfileEntity> = emptyList(),
        /** raceId → ("10 km" / "5 km" → indice du parcours). */
        courseFactors: Map<String, Map<String, Double>> = emptyMap(),
        /** raceId → raceId de l'édition précédente du même parcours. */
        previousEditionByRaceId: Map<String, String> = emptyMap(),
        weatherByRaceId: Map<String, RaceWeatherEntity> = emptyMap()
    ): RunnerChallengeSummary {
        val profilesById = allProfiles.associateBy { it.profileId }
        val overridesByProfileAndRace = allBibOverrides.groupBy { it.profileId }
            .mapValues { (_, list) -> list.associateBy { it.raceId } }

        // Fonction utilitaire : un résultat n'est valide QUE si le coureur possède un dossard attribué pour cette distance
        fun isResultValidForProfileAndDistance(res: RaceResultEntity, targetProfile: RunnerProfileEntity?): Boolean {
            val p = targetProfile ?: return true
            val is5k = res.distanceLabel == "5 km"
            val defaultBib = if (is5k) p.defaultBib5km.trim() else p.defaultBib10km.trim()
            val override = overridesByProfileAndRace[p.profileId]?.get(res.raceId)
            val hasOverrideForDist = override != null &&
                override.distanceKm == (if (is5k) 5 else 10) &&
                override.bibNumber.isNotBlank()
            return defaultBib.isNotBlank() || hasOverrideForDist
        }

        val cleanAllSeasonResults = allSeasonResults.filter { res ->
            res.seasonYear == seasonYear &&
                isResultValidForProfileAndDistance(res, profilesById[res.profileId] ?: if (res.profileId == profile.profileId) profile else null)
        }

        val runnerResults = cleanAllSeasonResults.filter {
            it.profileId == profile.profileId
        }
        val overridesByRaceId = overridesByProfileAndRace[profile.profileId].orEmpty()

        val hasDefaultBib10 = profile.defaultBib10km.trim().isNotBlank()
        val hasAnyOverride10 = overridesByRaceId.values.any { it.distanceKm == 10 && it.bibNumber.isNotBlank() }
        val hasAssignedBib10 = hasDefaultBib10 || hasAnyOverride10
        val results10km = if (hasAssignedBib10) {
            runnerResults.filter { it.distanceLabel != "5 km" }
        } else {
            emptyList()
        }

        val hasDefaultBib5 = profile.defaultBib5km.trim().isNotBlank()
        val hasAnyOverride5 = overridesByRaceId.values.any { it.distanceKm == 5 && it.bibNumber.isNotBlank() }
        val hasAssignedBib5 = hasDefaultBib5 || hasAnyOverride5
        val results5km = if (hasAssignedBib5) {
            runnerResults.filter { it.distanceLabel == "5 km" }
        } else {
            emptyList()
        }

        val seasonRaceIds = racesInSeason.map { it.raceId }.toSet()
        val terrainWeatherByRaceId = allTerrainWeather
            .filter { it.profileId == profile.profileId && it.raceId in seasonRaceIds }
            .associateBy { it.raceId }

        val metricsByRaceId = allActivityMetrics
            .filter { it.profileId == profile.profileId }
            .associateBy { it.raceId }

        val result10kmByRaceId = results10km.associateBy { it.raceId }
        val result5kmByRaceId = results5km.associateBy { it.raceId }

        val sorted10kmRaceIds = results10km
            .sortedWith(compareByDescending<RaceResultEntity> { it.challengePoints }.thenBy { it.overallRank })
            .mapIndexed { idx, r -> r.raceId to (idx + 1) }
            .toMap()

        val sorted5kmRaceIds = results5km
            .sortedWith(compareByDescending<RaceResultEntity> { it.challengePoints }.thenBy { it.overallRank })
            .mapIndexed { idx, r -> r.raceId to (idx + 1) }
            .toMap()

        val racesWithGeo = racesInSeason
            .sortedBy { it.roundNumber }
            .map { race -> race to GaumeInitialSeedData.resolveGaumeGeoForRace(race) }

        val summary10km = computeDistanceChallenge(
            distanceKm = 10,
            distanceLabel = "10 km",
            assignedBibNumber = profile.defaultBib10km.trim(),
            hasAssignedBib = hasAssignedBib10,
            racesWithGeo = racesWithGeo,
            distanceResults = results10km,
            terrainWeatherByRaceId = terrainWeatherByRaceId,
            metricsByRaceId = metricsByRaceId
        )

        val summary5km = computeDistanceChallenge(
            distanceKm = 5,
            distanceLabel = "5 km",
            assignedBibNumber = profile.defaultBib5km.trim(),
            hasAssignedBib = hasAssignedBib5,
            racesWithGeo = racesWithGeo,
            distanceResults = results5km,
            terrainWeatherByRaceId = terrainWeatherByRaceId,
            metricsByRaceId = metricsByRaceId
        )

        val allResultsByRaceAndProfile = cleanAllSeasonResults
            .groupBy { it.raceId }
            .mapValues { (_, list) -> list.groupBy { it.profileId } }

        val racesWithContext = racesWithGeo.map { (race, geo) ->
            val res10 = result10kmByRaceId[race.raceId]
            val isTop15For10 = res10 != null && summary10km.top15RaceIds.contains(race.raceId)
            val delta10 = if (res10 != null && !isTop15For10 && summary10km.current15thRaceThresholdPoints != null) {
                max(0, summary10km.current15thRaceThresholdPoints - res10.challengePoints + 1)
            } else null

            val res5 = result5kmByRaceId[race.raceId]
            val isTop15For5 = res5 != null && summary5km.top15RaceIds.contains(race.raceId)
            val delta5 = if (res5 != null && !isTop15For5 && summary5km.current15thRaceThresholdPoints != null) {
                max(0, summary5km.current15thRaceThresholdPoints - res5.challengePoints + 1)
            } else null

            RaceItemWithContext(
                race = race,
                gaumeGeo = geo,
                result10km = res10,
                isCountingInTop15For10km = isTop15For10,
                rankAmongRunnerRaces10km = sorted10kmRaceIds[race.raceId],
                pointsDeltaToEnterTop15For10km = delta10,
                result5km = res5,
                isCountingInTop15For5km = isTop15For5,
                rankAmongRunnerRaces5km = sorted5kmRaceIds[race.raceId],
                pointsDeltaToEnterTop15For5km = delta5,
                resultsByProfileAndDistance = allResultsByRaceAndProfile[race.raceId].orEmpty(),
                activityMetrics = metricsByRaceId[race.raceId],
                terrainWeather = terrainWeatherByRaceId[race.raceId],
                courseFactor10km = courseFactors[race.raceId]?.get("10 km"),
                courseFactor5km = courseFactors[race.raceId]?.get("5 km"),
                previousEditionRaceId = previousEditionByRaceId[race.raceId],
                previousEditionFactor10km = previousEditionByRaceId[race.raceId]?.let { courseFactors[it]?.get("10 km") },
                previousEditionFactor5km = previousEditionByRaceId[race.raceId]?.let { courseFactors[it]?.get("5 km") },
                weather = weatherByRaceId[race.raceId]
            )
        }

        val raceByIdForTotals = racesInSeason.associateBy { it.raceId }
        val totalKm = results10km.sumOf { realKmOfResult(it, raceByIdForTotals[it.raceId], 10) } +
            results5km.sumOf { realKmOfResult(it, raceByIdForTotals[it.raceId], 5) }
        val totalElev = summary10km.totalElevationGainMeters + summary5km.totalElevationGainMeters

        return RunnerChallengeSummary(
            profile = profile,
            seasonYear = seasonYear,
            challenge10km = summary10km,
            challenge5km = summary5km,
            totalDistanceRunKmAllDistances = (totalKm * 10.0).roundToInt() / 10.0,
            totalElevationGainMAllDistances = totalElev,
            racesWithContext = racesWithContext
        )
    }

    /**
     * Distance réelle couverte pour un résultat : longueur publiée par MyChrono pour cette distance,
     * sinon déduite du temps et de la vitesse officiels (eux-mêmes calculés sur la longueur réelle).
     */
    fun realKmOfResult(result: RaceResultEntity, race: RaceEntity?, distanceKm: Int): Double =
        result.distanceKm?.takeIf { it > 0.0 }
            ?: race?.realDistanceKmFor(distanceKm)
            ?: (result.speedKmh * result.officialTimeSeconds / 3600.0)

    /** D+ connu d'une manche courue : trace GPX importée, sinon D+ publié pour la manche (10 km). */
    fun knownElevationForRace(race: RaceEntity, metrics: ActivityMetricsEntity?, distanceKm: Int): Int? =
        metrics?.elevationGainMeters ?: if (distanceKm == 10) race.elevationGainMeters else null

    private fun computeDistanceChallenge(
        distanceKm: Int,
        distanceLabel: String,
        assignedBibNumber: String,
        hasAssignedBib: Boolean,
        racesWithGeo: List<Pair<RaceEntity, GaumeVillageGeo>>,
        distanceResults: List<RaceResultEntity>,
        terrainWeatherByRaceId: Map<String, RaceTerrainWeatherEntity>,
        metricsByRaceId: Map<String, ActivityMetricsEntity>
    ): DistanceChallengeSummary {
        val validResults = if (hasAssignedBib) distanceResults else emptyList()
        val raceById = racesWithGeo.associate { it.first.raceId to it.first }

        val sortedByPointsDesc = validResults.sortedWith(
            compareByDescending<RaceResultEntity> { it.challengePoints }
                .thenBy { it.overallRank }
                .thenBy { it.officialTimeSeconds }
        )

        val top15Results = sortedByPointsDesc.take(MAX_COUNTING_RACES)
        val top15RaceIds = top15Results.map { it.raceId }.toSet()

        val completedCount = validResults.size
        val countingCount = top15Results.size
        val droppedCount = max(0, completedCount - MAX_COUNTING_RACES)
        val remainingTo15 = max(0, MAX_COUNTING_RACES - countingCount)
        val isComplete = countingCount >= MAX_COUNTING_RACES

        val totalPoints = top15Results.sumOf { it.challengePoints }
        val avgPointsInt = if (countingCount > 0) {
            (totalPoints.toDouble() / countingCount).roundToInt()
        } else {
            0
        }

        val lowestCounting = top15Results.lastOrNull()?.challengePoints ?: 0
        val threshold15th = if (isComplete) lowestCounting else null

        val bestResult = sortedByPointsDesc.firstOrNull()
        val bestPoints = bestResult?.challengePoints ?: 0
        val bestRaceTitle = bestResult?.let { raceById[it.raceId]?.title }
        val bestRank = validResults.minOfOrNull { it.overallRank }

        val avgPaceFormatted = if (validResults.isNotEmpty()) {
            // Moyenne des allures réelles (temps / longueur réelle de chaque parcours).
            val paceSecPerKm = validResults.map { res ->
                res.officialTimeSeconds / realKmOfResult(res, raceById[res.raceId], distanceKm)
            }.average().roundToInt()
            "%d:%02d /km".format(paceSecPerKm / 60, paceSecPerKm % 60)
        } else {
            "--:-- /km"
        }

        val avgSpeedKmh = if (validResults.isNotEmpty()) {
            ((validResults.map { it.speedKmh }.average()) * 10.0).roundToInt() / 10.0
        } else {
            0.0
        }

        val totalDistKm = (validResults.sumOf { realKmOfResult(it, raceById[it.raceId], distanceKm) } * 10.0)
            .roundToInt() / 10.0
        val completedRaceIds = validResults.map { it.raceId }.toSet()
        val knownElevations = racesWithGeo
            .filter { it.first.raceId in completedRaceIds }
            .mapNotNull { knownElevationForRace(it.first, metricsByRaceId[it.first.raceId], distanceKm) }
        val totalElevMeters = knownElevations.sum()

        // Objectif par défaut du simulateur : ta moyenne réelle de points (0 tant qu'aucune course n'est courue).
        val defaultTargetPts = avgPointsInt

        // Candidats du Simulateur Top 15 : manches à venir (une manche passée et close n'est plus courable).
        val candidateRaces = racesWithGeo
            .filter { it.first.raceId !in completedRaceIds && !it.first.isCompleted }
            .map { (race, _) ->
                val tw = terrainWeatherByRaceId[race.raceId]
                val isPlanned = tw?.isPlannedForSimulator == true && tw.plannedDistanceKm == distanceKm
                val targetPts = (tw?.simulatedTargetPoints ?: defaultTargetPts).coerceIn(0, 1200)
                SimulatedRaceCandidate(
                    race = race,
                    isPlanned = isPlanned,
                    targetPoints = targetPts,
                    terrainCondition = tw?.terrainCondition?.ifBlank { null }
                )
            }

        val plannedCandidates = candidateRaces.filter { it.isPlanned }

        data class SimScore(val raceId: String, val points: Int, val isPlanned: Boolean)

        val combinedScores = buildList {
            validResults.forEach { add(SimScore(it.raceId, it.challengePoints, isPlanned = false)) }
            plannedCandidates.forEach { add(SimScore(it.race.raceId, it.targetPoints, isPlanned = true)) }
        }.sortedByDescending { it.points }

        val top15Sim = combinedScores.take(MAX_COUNTING_RACES)
        val projectedTotal = top15Sim.sumOf { it.points }
        val netGain = max(0, projectedTotal - totalPoints)
        val minScoreInProjectedTop15 = top15Sim.lastOrNull()?.points ?: 0

        val simulatorSummary = Top15SimulationSummary(
            distanceKm = distanceKm,
            currentCountingRaces = countingCount,
            currentTotalPoints = totalPoints,
            plannedRacesCount = plannedCandidates.size,
            projectedCountingRaces = top15Sim.size,
            projectedTotalPoints = projectedTotal,
            netPointsGain = netGain,
            minimumScoreToBeatInTop15 = minScoreInProjectedTop15,
            willCompleteChallenge = top15Sim.size >= MAX_COUNTING_RACES,
            defaultSuggestedTargetPoints = defaultTargetPts,
            candidateRaces = candidateRaces,
            plannedRaceIdsCountingInProjectedTop15 = top15Sim.filter { it.isPlanned }.map { it.raceId }.toSet()
        )

        return DistanceChallengeSummary(
            distanceKm = distanceKm,
            distanceLabel = distanceLabel,
            assignedBibNumber = assignedBibNumber,
            hasAssignedBib = hasAssignedBib,
            completedRacesCount = completedCount,
            countingRacesCount = countingCount,
            droppedRacesCount = droppedCount,
            remainingToComplete15 = remainingTo15,
            isChallengeComplete = isComplete,
            totalChallengePoints = totalPoints,
            averagePointsPerRace = avgPointsInt,
            lowestCountingPoints = lowestCounting,
            bestPoints = bestPoints,
            bestRaceTitle = bestRaceTitle,
            bestRank = bestRank,
            averagePaceFormatted = avgPaceFormatted,
            averageSpeedKmh = avgSpeedKmh,
            totalDistanceRunKm = totalDistKm,
            totalElevationGainMeters = totalElevMeters,
            elevationKnownRacesCount = knownElevations.size,
            current15thRaceThresholdPoints = threshold15th,
            top15RaceIds = top15RaceIds,
            simulatorSummary = simulatorSummary
        )
    }

    /**
     * Comparaison d'une saison à l'autre, parcours par parcours : un parcours est identifié par son lieu
     * et son rang dans la saison (la 1re course de Florenville de 2026 correspond à la 1re de 2025).
     */
    fun buildSameCourseComparisons(
        profileId: String,
        distanceKm: Int,
        seasonA: Int,
        seasonB: Int,
        allRaces: List<RaceEntity>,
        allResults: List<RaceResultEntity>
    ): List<SameCourseYearComparison> {
        val targetDistanceLabel = if (distanceKm == 5) "5 km" else "10 km"
        val races = allRaces.filter { it.isChallenge && (it.seasonYear == seasonA || it.seasonYear == seasonB) }
        if (races.isEmpty()) return emptyList()
        val keys = CourseDifficultyCalculator.courseKeys(races)

        val resultsByRace = allResults
            .filter { it.profileId == profileId && it.distanceLabel == targetDistanceLabel }
            .associateBy { it.raceId }

        return races.groupBy { keys[it.raceId] }
            .mapNotNull { (key, list) ->
                if (key == null) return@mapNotNull null
                val raceA = list.firstOrNull { it.seasonYear == seasonA }
                val raceB = list.firstOrNull { it.seasonYear == seasonB }
                val resA = raceA?.let { resultsByRace[it.raceId] }
                val resB = raceB?.let { resultsByRace[it.raceId] }
                if (resA == null && resB == null) return@mapNotNull null

                SameCourseYearComparison(
                    courseTitle = raceB?.title ?: raceA?.title ?: key,
                    village = raceB?.village ?: raceA?.village.orEmpty(),
                    distanceLabel = targetDistanceLabel,
                    yearA = seasonA,
                    resultSeasonA = resA,
                    yearB = seasonB,
                    resultSeasonB = resB,
                    timeDeltaSeconds = if (resA != null && resB != null) resB.officialTimeSeconds - resA.officialTimeSeconds else null,
                    pointsDelta = if (resA != null && resB != null) resB.challengePoints - resA.challengePoints else null,
                    sortDateIso = (raceB ?: raceA)?.dateIso.orEmpty()
                )
            }
            .sortedBy { it.sortDateIso.substring(minOf(5, it.sortDateIso.length)) }
    }
}
