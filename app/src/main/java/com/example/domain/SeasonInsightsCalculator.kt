package com.example.domain

import com.example.data.local.ChallengeStandingEntity
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import kotlin.math.roundToInt

/** Une course courue, avec son allure brute et son allure corrigée du parcours. */
data class FormPoint(
    val raceId: String,
    val dateIso: String,
    val village: String,
    val raceTitle: String,
    val roundNumber: Int,
    val distanceKm: Double,
    val timeFormatted: String,
    val paceSecPerKm: Int,
    /** Indice du parcours (1,08 = 8 % plus lent qu'un parcours moyen) ; null si pas assez de réguliers. */
    val factor: Double?,
    /** Allure ramenée à un parcours moyen : allure / indice. Null si l'indice est inconnu. */
    val correctedPaceSecPerKm: Int?,
    val points: Int
)

/** Une visite d'un village (une course courue), toutes saisons confondues. */
data class PassportVisit(
    val seasonYear: Int,
    val raceId: String,
    val raceTitle: String,
    val dateIso: String,
    val distanceName: String,
    val timeFormatted: String,
    val isChallenge: Boolean
)

data class PassportVillage(
    val village: String,
    val territoryName: String,
    val visits: List<PassportVisit>
) {
    val firstSeason: Int get() = visits.minOf { it.seasonYear }
}

data class VillagePassport(
    val visited: List<PassportVillage>,
    /** Villages du dernier calendrier publié où le coureur n'a encore jamais couru. */
    val notYetVisited: List<String>,
    val latestCalendarSeason: Int?
) {
    val totalVisits: Int get() = visited.sumOf { it.visits.size }
}

/** Bilan d'une saison pour un coureur, sur une distance du challenge. */
data class SeasonRecap(
    val profileId: String,
    val profileName: String,
    val seasonYear: Int,
    val distanceKm: Int,
    val distanceLabel: String,
    val isSeasonOver: Boolean,
    val racesRun: Int,
    val countingRaces: Int,
    val challengeComplete: Boolean,
    val totalPoints: Int,
    val standing: ChallengeStandingEntity?,
    val runnersInDistance: Int?,
    val runnersInCategory: Int?,
    val bestPointsRace: FormPoint?,
    val fastestRace: FormPoint?,
    val bestCorrectedRace: FormPoint?,
    val totalKm: Double,
    val totalTimeSeconds: Int,
    val villagesThisSeason: Int,
    val newVillagesThisSeason: Int,
    val horsChallengeRaces: Int,
    /** Médiane des allures corrigées des 3 premières et des 3 dernières courses de la saison. */
    val formStartPaceSecPerKm: Int?,
    val formEndPaceSecPerKm: Int?
)

object SeasonInsightsCalculator {

    fun formatPace(secPerKm: Int?): String =
        secPerKm?.let { "%d:%02d /km".format(it / 60, it % 60) } ?: "--:-- /km"

    fun formatDuration(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        return if (h > 0) "${h} h %02d".format(m) else "$m min"
    }

    /** Courses courues sur la distance, dans l'ordre du calendrier. */
    fun formPoints(summary: RunnerChallengeSummary, distanceKm: Int): List<FormPoint> =
        summary.racesWithContext
            .sortedWith(compareBy<RaceItemWithContext>({ it.race.dateIso }, { it.race.roundNumber }))
            .mapNotNull { item ->
                val res = item.resultForDistance(distanceKm) ?: return@mapNotNull null
                val km = AlgChallengeCalculator.realKmOfResult(res, item.race, distanceKm)
                if (km <= 0.5) return@mapNotNull null
                val pace = (res.officialTimeSeconds / km).roundToInt()
                val factor = item.courseFactorForDistance(distanceKm)
                FormPoint(
                    raceId = item.race.raceId,
                    dateIso = item.race.dateIso,
                    village = item.gaumeGeo.canonicalVillage,
                    raceTitle = item.race.title,
                    roundNumber = item.race.roundNumber,
                    distanceKm = km,
                    timeFormatted = res.officialTimeFormatted,
                    paceSecPerKm = pace,
                    factor = factor,
                    correctedPaceSecPerKm = factor?.let { (pace / it).roundToInt() },
                    points = res.challengePoints
                )
            }

    /**
     * Forme actuelle : médiane des allures corrigées des 3 dernières courses
     * (allure brute pour une course sans indice).
     */
    fun currentFormPace(points: List<FormPoint>): Int? =
        medianPace(points.takeLast(3))

    private fun medianPace(points: List<FormPoint>): Int? {
        val values = points.map { (it.correctedPaceSecPerKm ?: it.paceSecPerKm).toDouble() }
        return CourseDifficultyCalculator.median(values)?.roundToInt()
    }

    /** Passeport des villages : toutes les courses courues par le profil, toutes saisons confondues. */
    fun buildPassport(
        profileId: String,
        allRaces: List<RaceEntity>,
        allResults: List<RaceResultEntity>
    ): VillagePassport {
        val raceById = allRaces.associateBy { it.raceId }
        val visits = allResults
            .filter { it.profileId == profileId }
            .mapNotNull { res ->
                val race = raceById[res.raceId] ?: return@mapNotNull null
                val geo = GaumeInitialSeedData.resolveGaumeGeoForRace(race)
                Triple(geo.canonicalVillage, geo.territoryName, PassportVisit(
                    seasonYear = race.seasonYear,
                    raceId = race.raceId,
                    raceTitle = race.title,
                    dateIso = race.dateIso,
                    distanceName = res.distanceName ?: res.distanceLabel,
                    timeFormatted = res.officialTimeFormatted,
                    isChallenge = race.isChallenge
                ))
            }

        val visited = visits.groupBy { GaumeInitialSeedData.normalizeKey(it.first) }
            .map { (_, list) ->
                PassportVillage(
                    village = list.first().first,
                    territoryName = list.first().second,
                    visits = list.map { it.third }.sortedByDescending { it.dateIso }
                )
            }
            .sortedWith(compareByDescending<PassportVillage> { it.visits.size }.thenBy { it.village })

        val visitedKeys = visited.map { GaumeInitialSeedData.normalizeKey(it.village) }.toSet()
        val latestSeason = allRaces.filter { it.isChallenge }.maxOfOrNull { it.seasonYear }
        val notYet = allRaces
            .filter { it.seasonYear == latestSeason }
            .map { GaumeInitialSeedData.resolveGaumeGeoForRace(it).canonicalVillage }
            .distinctBy { GaumeInitialSeedData.normalizeKey(it) }
            .filter { GaumeInitialSeedData.normalizeKey(it) !in visitedKeys }
            .sorted()

        return VillagePassport(visited = visited, notYetVisited = notYet, latestCalendarSeason = latestSeason)
    }

    /** La saison est terminée quand toutes ses manches sont closes. */
    fun isSeasonOver(summary: RunnerChallengeSummary): Boolean =
        summary.racesWithContext.isNotEmpty() && summary.racesWithContext.all { it.race.isCompleted }

    fun buildRecap(
        summary: RunnerChallengeSummary,
        distanceKm: Int,
        standing: ProfileChallengeStanding?,
        passport: VillagePassport,
        horsChallengeRaces: Int
    ): SeasonRecap {
        val dist = summary.challengeForDistance(distanceKm)
        val points = formPoints(summary, distanceKm)
        val season = summary.seasonYear

        val villagesThisSeason = passport.visited.count { v -> v.visits.any { it.seasonYear == season } }
        val newVillages = passport.visited.count { it.firstSeason == season }

        return SeasonRecap(
            profileId = summary.profile.profileId,
            profileName = summary.profile.displayName,
            seasonYear = season,
            distanceKm = distanceKm,
            distanceLabel = dist.distanceLabel,
            isSeasonOver = isSeasonOver(summary),
            racesRun = dist.completedRacesCount,
            countingRaces = dist.countingRacesCount,
            challengeComplete = dist.isChallengeComplete,
            totalPoints = dist.totalChallengePoints,
            standing = standing?.standing,
            runnersInDistance = standing?.runnersInDistance,
            runnersInCategory = standing?.runnersInCategory,
            bestPointsRace = points.maxByOrNull { it.points },
            fastestRace = points.minByOrNull { it.paceSecPerKm },
            bestCorrectedRace = points.filter { it.correctedPaceSecPerKm != null }.minByOrNull { it.correctedPaceSecPerKm!! },
            totalKm = (points.sumOf { it.distanceKm } * 10.0).roundToInt() / 10.0,
            totalTimeSeconds = summary.racesWithContext.sumOf { it.resultForDistance(distanceKm)?.officialTimeSeconds ?: 0 },
            villagesThisSeason = villagesThisSeason,
            newVillagesThisSeason = newVillages,
            horsChallengeRaces = horsChallengeRaces,
            formStartPaceSecPerKm = if (points.size >= 6) medianPace(points.take(3)) else null,
            formEndPaceSecPerKm = if (points.size >= 6) medianPace(points.takeLast(3)) else null
        )
    }
}
