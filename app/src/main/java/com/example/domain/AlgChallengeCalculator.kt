package com.example.domain

import com.example.data.local.ActivityMetricsEntity
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.GaumeVillageGeo
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.local.RunnerProfileEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Élément de course combiné pour une saison (>= 2026), avec séparation stricte
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
    val terrainWeather: RaceTerrainWeatherEntity?
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
    val simulatorSummary: Top15SimulationSummary,
    val conquestSummary: GaumeConquestSummary
) {
    val countingRaceIds: Set<String> get() = top15RaceIds
}

/**
 * Progression d'un des fiefs (territoires du jeu, découpés sur les lieux réels des manches).
 */
data class GaumeTerritoryConquestProgress(
    val territoryId: String,
    val title: String,
    val subtitle: String,
    val badgeTitle: String,
    val colorHex: Long,
    val conqueredRacesCount: Int,
    val totalRacesInTerritory: Int,
    val isTerritoryConquered: Boolean,
    val isFullyMastered: Boolean,
    val villagesConqueredNames: List<String>,
    val villagesRemainingNames: List<String>
)

/**
 * Badge de terroir ou trophée de gamification en Gaume.
 */
data class GaumeBadgeItem(
    val badgeId: String,
    val title: String,
    val description: String,
    val categoryLabel: String,
    val isUnlocked: Boolean,
    val progressText: String,
    val accentColorHex: Long
)

/**
 * 1) Série de régularité (Streak) et Bonus de fidélité Gaumaise.
 */
data class GaumeStreakSummary(
    val currentStreakRaces: Int,
    val bestStreakRaces: Int,
    val loyaltyMultiplier: Double,
    val loyaltyTierTitle: String,
    val bonusLoyaltyXp: Int,
    val nextMilestoneRaces: Int
)

/**
 * 2) Défi ou Quête du Trio en Gaume (Damien, Laetitia, Élodie).
 */
data class GaumeTrioQuestProgress(
    val questId: String,
    val title: String,
    val description: String,
    val categoryTag: String, // "TRIO", "RÉGULARITÉ", "TERROIR", "PASSEPORT"
    val currentProgress: Int,
    val targetProgress: Int,
    val rewardXp: Int,
    val isCompleted: Boolean,
    val accentColorHex: Long
)

/**
 * 3) Tampon de village dans le Passeport Collector de Gaume (élément du jeu).
 */
data class GaumeVillageStampItem(
    val raceId: String,
    val roundNumber: Int,
    val villageName: String,
    val raceTitle: String,
    val dateIso: String,
    val territoryId: String,
    val territoryName: String,
    val territoryColorHex: Long,
    val isStamped: Boolean,
    val stampedBib: String?,
    val stampedTimeFormatted: String?,
    val stampedPoints: Int?,
    val stampedRank: Int?,
    val stampedDistanceLabel: String?
)

/**
 * Résumé complet de la Conquête de Gaume, Série (Streak), Quêtes du Trio, Passeport Collector & Badges.
 */
data class GaumeConquestSummary(
    val conqueredVillagesCount: Int,
    val totalVillagesInSeason: Int,
    val territoriesConqueredCount: Int,
    val totalTerritoriesCount: Int,
    val xpPoints: Int,
    val runnerLevel: Int,
    val runnerRankTitle: String,
    val streakSummary: GaumeStreakSummary,
    val trioQuests: List<GaumeTrioQuestProgress>,
    val passportStamps: List<GaumeVillageStampItem>,
    val territories: List<GaumeTerritoryConquestProgress>,
    val badges: List<GaumeBadgeItem>
)

/**
 * Résumé complet d'un coureur pour une saison (>= 2026) avec les deux classements séparés :
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
    val racesWithContext: List<RaceItemWithContext>
) {
    val simulator10km: Top15SimulationSummary get() = challenge10km.simulatorSummary
    val simulator5km: Top15SimulationSummary get() = challenge5km.simulatorSummary
    val conquest10km: GaumeConquestSummary get() = challenge10km.conquestSummary
    val conquest5km: GaumeConquestSummary get() = challenge5km.conquestSummary

    fun challengeForDistance(distanceKm: Int): DistanceChallengeSummary =
        if (distanceKm == 5) challenge5km else challenge10km

    fun summaryForDistance(distanceKm: Int): DistanceChallengeSummary =
        challengeForDistance(distanceKm)

    fun simulatorForDistance(distanceKm: Int): Top15SimulationSummary =
        challengeForDistance(distanceKm).simulatorSummary

    fun conquestForDistance(distanceKm: Int): GaumeConquestSummary =
        challengeForDistance(distanceKm).conquestSummary
}

/**
 * Comparaison inter-saisons (ex: 2026 vs 2027) sur une même course et une même distance.
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
    val pointsDelta: Int?
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
        allProfiles: List<RunnerProfileEntity> = emptyList()
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
            metricsByRaceId = metricsByRaceId,
            allSeasonResultsAllRunners = cleanAllSeasonResults
        )

        val summary5km = computeDistanceChallenge(
            distanceKm = 5,
            distanceLabel = "5 km",
            assignedBibNumber = profile.defaultBib5km.trim(),
            hasAssignedBib = hasAssignedBib5,
            racesWithGeo = racesWithGeo,
            distanceResults = results5km,
            terrainWeatherByRaceId = terrainWeatherByRaceId,
            metricsByRaceId = metricsByRaceId,
            allSeasonResultsAllRunners = cleanAllSeasonResults
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
                terrainWeather = terrainWeatherByRaceId[race.raceId]
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
        race?.realDistanceKmFor(distanceKm)
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
        metricsByRaceId: Map<String, ActivityMetricsEntity>,
        allSeasonResultsAllRunners: List<RaceResultEntity>
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

        // Candidats du Simulateur Top 15 (courses non encore courues sur cette distance)
        val candidateRaces = racesWithGeo
            .filter { it.first.raceId !in completedRaceIds }
            .map { (race, _) ->
                val tw = terrainWeatherByRaceId[race.raceId]
                val isPlanned = tw?.isPlannedForSimulator == true && tw.plannedDistanceKm == distanceKm
                val targetPts = (tw?.simulatedTargetPoints ?: defaultTargetPts).coerceIn(0, 1200)
                SimulatedRaceCandidate(
                    race = race,
                    isPlanned = isPlanned,
                    targetPoints = targetPts,
                    terrainCondition = tw?.terrainCondition
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

        val conquestSummary = computeGaumeConquestSummary(
            racesWithGeo = racesWithGeo,
            completedRaceIds = completedRaceIds,
            resultsByRaceId = validResults.associateBy { it.raceId },
            countingCount = countingCount,
            terrainWeatherByRaceId = terrainWeatherByRaceId,
            allSeasonResultsAllRunners = allSeasonResultsAllRunners
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
            simulatorSummary = simulatorSummary,
            conquestSummary = conquestSummary
        )
    }

    private fun computeGaumeConquestSummary(
        racesWithGeo: List<Pair<RaceEntity, GaumeVillageGeo>>,
        completedRaceIds: Set<String>,
        resultsByRaceId: Map<String, RaceResultEntity>,
        countingCount: Int,
        terrainWeatherByRaceId: Map<String, RaceTerrainWeatherEntity>,
        allSeasonResultsAllRunners: List<RaceResultEntity>
    ): GaumeConquestSummary {
        val territoryColorById = GaumeInitialSeedData.gaumeTerritories.associate { it.territoryId to it.colorHex }

        val territoriesProgress = GaumeInitialSeedData.gaumeTerritories.map { def ->
            val racesInTerritory = racesWithGeo.filter { it.second.territoryId == def.territoryId }
            val conqueredInTerritory = racesInTerritory.filter { it.first.raceId in completedRaceIds }
            val remainingInTerritory = racesInTerritory.filter { it.first.raceId !in completedRaceIds }

            val conqueredVillages = conqueredInTerritory.map { it.second.canonicalVillage }.distinct()
            val remainingVillages = remainingInTerritory.map { it.second.canonicalVillage }.distinct()

            GaumeTerritoryConquestProgress(
                territoryId = def.territoryId,
                title = def.title,
                subtitle = def.subtitle,
                badgeTitle = def.badgeTitle,
                colorHex = def.colorHex,
                conqueredRacesCount = conqueredInTerritory.size,
                totalRacesInTerritory = racesInTerritory.size,
                isTerritoryConquered = conqueredInTerritory.isNotEmpty(),
                isFullyMastered = racesInTerritory.isNotEmpty() && conqueredInTerritory.size == racesInTerritory.size,
                villagesConqueredNames = conqueredVillages,
                villagesRemainingNames = remainingVillages
            )
        }

        val conqueredVillagesTotal = racesWithGeo
            .filter { it.first.raceId in completedRaceIds }
            .map { it.second.canonicalVillage }
            .distinct()
            .size
        val totalVillagesSeason = racesWithGeo
            .map { it.second.canonicalVillage }
            .distinct()
            .size

        val conqueredTerritoriesCount = territoriesProgress.count { it.isTerritoryConquered }
        val totalTerritoriesCount = territoriesProgress.size

        // 1) Série de régularité (Streak) & Bonus de fidélité
        var currentStreak = 0
        var bestStreak = 0
        var runningStreak = 0
        val sortedRaces = racesWithGeo.map { it.first }.sortedBy { it.roundNumber }
        for (race in sortedRaces) {
            if (race.raceId in completedRaceIds) {
                runningStreak += 1
                if (runningStreak > bestStreak) bestStreak = runningStreak
            } else {
                runningStreak = 0
            }
        }
        val completedRoundsSorted = sortedRaces.filter { it.raceId in completedRaceIds }.map { it.roundNumber }
        if (completedRoundsSorted.isNotEmpty()) {
            currentStreak = 1
            for (idx in completedRoundsSorted.size - 1 downTo 1) {
                if (completedRoundsSorted[idx] - completedRoundsSorted[idx - 1] == 1) {
                    currentStreak += 1
                } else {
                    break
                }
            }
            if (currentStreak > bestStreak) bestStreak = currentStreak
        }

        val completedTotal = completedRaceIds.size
        val (loyaltyMultiplier, loyaltyTitle, nextMilestone) = when {
            bestStreak >= 5 || completedTotal >= 10 -> Triple(2.0, "Légende d'Orval (x2.0)", 15)
            bestStreak >= 3 || completedTotal >= 5 -> Triple(1.5, "Pilier de la Semois (x1.5)", 10)
            bestStreak >= 2 || completedTotal >= 2 -> Triple(1.2, "Fidèle des Sentiers (x1.2)", 5)
            else -> Triple(1.0, "Éclaireur de Gaume (x1.0)", 2)
        }

        val muddyRacesCompleted = completedRaceIds.count { raceId ->
            val cond = terrainWeatherByRaceId[raceId]?.terrainCondition.orEmpty()
            cond.equals("Boueux", ignoreCase = true) || cond.equals("Très gras", ignoreCase = true)
        }

        // 2) Passeport Collector de Gaume (Tampons Officiels par village)
        val passportStamps = racesWithGeo.map { (race, geo) ->
            val res = resultsByRaceId[race.raceId]
            GaumeVillageStampItem(
                raceId = race.raceId,
                roundNumber = race.roundNumber,
                villageName = geo.canonicalVillage,
                raceTitle = race.title,
                dateIso = race.dateIso,
                territoryId = geo.territoryId,
                territoryName = geo.territoryName,
                territoryColorHex = territoryColorById[geo.territoryId] ?: 0xFF1B5E3A,
                isStamped = res != null,
                stampedBib = res?.bibNumber,
                stampedTimeFormatted = res?.officialTimeFormatted,
                stampedPoints = res?.challengePoints,
                stampedRank = res?.overallRank,
                stampedDistanceLabel = res?.distanceLabel
            )
        }

        // 3) Défis et Quêtes du Trio en Gaume
        val sharedTrioRacesCount = racesWithGeo.count { (race, _) ->
            val profilesInRace = allSeasonResultsAllRunners
                .filter { it.raceId == race.raceId }
                .map { it.profileId }
                .distinct()
            profilesInRace.size >= 2
        }

        val trioQuests = listOf(
            GaumeTrioQuestProgress(
                questId = "quest_trio_shared",
                title = "Escapade du Trio en Gaume",
                description = "Courir une même manche du Challenge à au moins 2 ou 3 membres du Trio (10 km ou 5 km).",
                categoryTag = "TRIO",
                currentProgress = sharedTrioRacesCount.coerceAtMost(3),
                targetProgress = 3,
                rewardXp = 250,
                isCompleted = sharedTrioRacesCount >= 3,
                accentColorHex = 0xFF006D77
            ),
            GaumeTrioQuestProgress(
                questId = "quest_streak_3",
                title = "Série Gaumaise (Streak x3)",
                description = "Enchaîner 3 manches consécutives du calendrier ALG sans interruption.",
                categoryTag = "RÉGULARITÉ",
                currentProgress = bestStreak.coerceAtMost(3),
                targetProgress = 3,
                rewardXp = 200,
                isCompleted = bestStreak >= 3,
                accentColorHex = 0xFFE07A2E
            ),
            GaumeTrioQuestProgress(
                questId = "quest_passport_5",
                title = "Passeport des 5 Villages",
                description = "Obtenir 5 tampons de villages différents dans le Passeport Collector.",
                categoryTag = "PASSEPORT",
                currentProgress = conqueredVillagesTotal.coerceAtMost(5),
                targetProgress = 5,
                rewardXp = 180,
                isCompleted = conqueredVillagesTotal >= 5,
                accentColorHex = 0xFF1E7E48
            ),
            GaumeTrioQuestProgress(
                questId = "quest_fiefs_3",
                title = "Conquérant des Terroirs",
                description = "Tamponner au moins un village dans 3 Fiefs différents de Gaume.",
                categoryTag = "TERROIR",
                currentProgress = conqueredTerritoriesCount.coerceAtMost(3),
                targetProgress = 3,
                rewardXp = 220,
                isCompleted = conqueredTerritoriesCount >= 3,
                accentColorHex = 0xFFD49B1B
            ),
            GaumeTrioQuestProgress(
                questId = "quest_fiefs_all",
                title = "Unificateur des $totalTerritoriesCount Fiefs",
                description = "Courir dans chacun des $totalTerritoriesCount fiefs du jeu (" +
                    GaumeInitialSeedData.gaumeTerritories.joinToString(", ") { it.title.substringBefore(" &").substringBefore(",") } + ").",
                categoryTag = "TERROIR",
                currentProgress = conqueredTerritoriesCount.coerceAtMost(totalTerritoriesCount),
                targetProgress = totalTerritoriesCount,
                rewardXp = 400,
                isCompleted = conqueredTerritoriesCount >= totalTerritoriesCount && totalTerritoriesCount > 0,
                accentColorHex = 0xFFC85A32
            ),
            GaumeTrioQuestProgress(
                questId = "quest_top15_crown",
                title = "Couronne Top 15 Validée",
                description = "Atteindre les 15 courses comptabilisées du Challenge des Allures Libres de Gaume.",
                categoryTag = "RÉGULARITÉ",
                currentProgress = countingCount.coerceAtMost(MAX_COUNTING_RACES),
                targetProgress = MAX_COUNTING_RACES,
                rewardXp = 500,
                isCompleted = countingCount >= MAX_COUNTING_RACES,
                accentColorHex = 0xFF2B59C3
            )
        )

        val baseXp = completedTotal * 120 +
            conqueredTerritoriesCount * 150 +
            (if (countingCount >= MAX_COUNTING_RACES) 350 else 0) +
            trioQuests.filter { it.isCompleted }.sumOf { it.rewardXp }
        val bonusLoyaltyXp = (completedTotal * 40 * (loyaltyMultiplier - 1.0)).roundToInt()
        val totalXp = baseXp + bonusLoyaltyXp

        val streakSummary = GaumeStreakSummary(
            currentStreakRaces = currentStreak,
            bestStreakRaces = bestStreak,
            loyaltyMultiplier = loyaltyMultiplier,
            loyaltyTierTitle = loyaltyTitle,
            bonusLoyaltyXp = bonusLoyaltyXp,
            nextMilestoneRaces = nextMilestone
        )

        val runnerLevel = 1 + (totalXp / 300)
        val runnerRankTitle = when {
            countingCount >= MAX_COUNTING_RACES && conqueredTerritoriesCount >= 5 -> "Grand Maître du Terroir Gaumais"
            conqueredTerritoriesCount >= 4 || completedTotal >= 10 -> "Seigneur des Cuestes & Forêts"
            conqueredTerritoriesCount >= 2 || completedTotal >= 4 -> "Pisteur de la Semois"
            completedTotal >= 1 -> "Éclaireur des Sentiers de Gaume"
            else -> "Aspirant du Challenge de Gaume"
        }

        val territoryBadges = territoriesProgress.map { tp ->
            GaumeBadgeItem(
                badgeId = "badge_${tp.territoryId}",
                title = tp.badgeTitle,
                description = "Conquérir au moins un village dans le fief « ${tp.title} »",
                categoryLabel = "Fief de Gaume",
                isUnlocked = tp.isTerritoryConquered,
                progressText = "${tp.conqueredRacesCount}/${tp.totalRacesInTerritory} village(s)",
                accentColorHex = tp.colorHex
            )
        }

        val specialBadges = listOf(
            GaumeBadgeItem(
                badgeId = "badge_grand_duc_gaume",
                title = "Unificateur des $totalTerritoriesCount Fiefs",
                description = "Avoir couru au moins une manche dans chacun des $totalTerritoriesCount fiefs du jeu",
                categoryLabel = "Légende",
                isUnlocked = conqueredTerritoriesCount == totalTerritoriesCount && totalTerritoriesCount > 0,
                progressText = "$conqueredTerritoriesCount/$totalTerritoriesCount fiefs",
                accentColorHex = 0xFFD49B1B
            ),
            GaumeBadgeItem(
                badgeId = "badge_sanglier_gaumais",
                title = "Sanglier des Bois Gaumais",
                description = "Terminer une manche sur terrain Boueux ou Très gras en Gaume",
                categoryLabel = "Terrain",
                isUnlocked = muddyRacesCompleted >= 1,
                progressText = if (muddyRacesCompleted >= 1) "$muddyRacesCompleted course(s) boue" else "0/1 terrain gras",
                accentColorHex = 0xFF8D5524
            ),
            GaumeBadgeItem(
                badgeId = "badge_top15_complet",
                title = "Couronne Top 15 ALG",
                description = "Valider les 15 courses comptabilisées du Challenge des Allures Libres de Gaume",
                categoryLabel = "Challenge",
                isUnlocked = countingCount >= MAX_COUNTING_RACES,
                progressText = "$countingCount/$MAX_COUNTING_RACES courses",
                accentColorHex = 0xFF1E7E48
            )
        )

        return GaumeConquestSummary(
            conqueredVillagesCount = conqueredVillagesTotal,
            totalVillagesInSeason = totalVillagesSeason,
            territoriesConqueredCount = conqueredTerritoriesCount,
            totalTerritoriesCount = totalTerritoriesCount,
            xpPoints = totalXp,
            runnerLevel = runnerLevel,
            runnerRankTitle = runnerRankTitle,
            streakSummary = streakSummary,
            trioQuests = trioQuests,
            passportStamps = passportStamps,
            territories = territoriesProgress,
            badges = territoryBadges + specialBadges
        )
    }

    fun buildSameCourseComparisons(
        profileId: String,
        distanceKm: Int,
        seasonA: Int,
        seasonB: Int,
        allRaces: List<RaceEntity>,
        allResults: List<RaceResultEntity>
    ): List<SameCourseYearComparison> {
        val targetDistanceLabel = if (distanceKm == 5) "5 km" else "10 km"
        val racesA = allRaces.filter { it.seasonYear == seasonA }
        val racesB = allRaces.filter { it.seasonYear == seasonB }
        if (racesA.isEmpty() && racesB.isEmpty()) return emptyList()

        val resultsA = allResults
            .filter { it.profileId == profileId && it.seasonYear == seasonA && it.distanceLabel == targetDistanceLabel }
            .associateBy { it.raceId }
        val resultsB = allResults
            .filter { it.profileId == profileId && it.seasonYear == seasonB && it.distanceLabel == targetDistanceLabel }
            .associateBy { it.raceId }

        val villages = (racesA.map { it.village } + racesB.map { it.village }).distinct()

        return villages.mapNotNull { village ->
            val raceA = racesA.find { it.village.equals(village, ignoreCase = true) }
            val raceB = racesB.find { it.village.equals(village, ignoreCase = true) }
            val resA = raceA?.let { resultsA[it.raceId] }
            val resB = raceB?.let { resultsB[it.raceId] }

            if (resA == null && resB == null) return@mapNotNull null

            val timeDelta = if (resA != null && resB != null) {
                resB.officialTimeSeconds - resA.officialTimeSeconds
            } else null

            val pointsDelta = if (resA != null && resB != null) {
                resB.challengePoints - resA.challengePoints
            } else null

            SameCourseYearComparison(
                courseTitle = raceB?.title ?: raceA?.title ?: village,
                village = village,
                distanceLabel = targetDistanceLabel,
                yearA = seasonA,
                resultSeasonA = resA,
                yearB = seasonB,
                resultSeasonB = resB,
                timeDeltaSeconds = timeDelta,
                pointsDelta = pointsDelta
            )
        }
    }
}
