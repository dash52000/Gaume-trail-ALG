package com.example.domain

import com.example.data.local.ChallengeStandingEntity
import com.example.data.local.RunnerProfileEntity
import com.example.data.remote.MyChronoApiClient
import com.example.data.remote.MyChronoRawResultRow

/**
 * Classement provisoire d'un profil du Trio sur une distance du Challenge.
 * « Provisoire » : recalculé à partir des résultats MyChrono, en attendant le classement officiel
 * (les départages officiels peuvent différer en cas d'égalité de points).
 */
data class ProfileChallengeStanding(
    val profileId: String,
    val profileName: String,
    val distanceLabel: String,
    val standing: ChallengeStandingEntity,
    val runnersInDistance: Int,
    val runnersInCategory: Int,
    val nextAbove: ChallengeStandingEntity?,
    /** Points supplémentaires nécessaires pour dépasser le coureur juste devant. */
    val pointsGapToNextAbove: Int?,
    /**
     * Score minimal d'une prochaine course pour dépasser le coureur juste devant :
     * avec 15 courses déjà comptées, une nouvelle course ne remplace que la plus faible.
     */
    val racePointsNeededToPassNext: Int?
)

object ChallengeStandingsCalculator {
    const val MAX_COUNTING_RACES = AlgChallengeCalculator.MAX_COUNTING_RACES

    private fun runnerKey(row: MyChronoRawResultRow): String =
        row.runnerId.ifBlank { "${row.fullName.trim().lowercase()}|${row.bibNumber}" }

    /**
     * Recalcule le classement de tous les coureurs : somme des 15 meilleurs résultats
     * sur une même distance (règlement ALG), puis rang au général et dans la catégorie.
     */
    fun computeSeasonStandings(
        seasonYear: Int,
        rawRowsByRaceId: Map<String, List<MyChronoRawResultRow>>,
        computedAtEpochMillis: Long
    ): List<ChallengeStandingEntity> {
        // Les raceId "alg_AAAA_rNN" se trient dans l'ordre du calendrier.
        val rowsInCalendarOrder = rawRowsByRaceId.toSortedMap().values.flatten()

        return listOf(MyChronoApiClient.LABEL_10KM, MyChronoApiClient.LABEL_5KM).flatMap { label ->
            val byRunner = rowsInCalendarOrder
                .filter { it.distanceLabel == label }
                .groupBy { runnerKey(it) }

            data class Agg(val key: String, val latest: MyChronoRawResultRow, val counted: List<Int>, val runs: Int)

            val aggregates = byRunner.map { (key, rows) ->
                val counted = rows.map { it.points }.sortedDescending().take(MAX_COUNTING_RACES)
                Agg(key, rows.last(), counted, rows.size)
            }

            fun competitionRanks(list: List<Agg>): Map<String, Int> {
                val sorted = list.sortedByDescending { it.counted.sum() }
                val ranks = HashMap<String, Int>()
                var previousTotal: Int? = null
                var previousRank = 0
                sorted.forEachIndexed { index, agg ->
                    val total = agg.counted.sum()
                    val rank = if (total == previousTotal) previousRank else index + 1
                    ranks[agg.key] = rank
                    previousTotal = total
                    previousRank = rank
                }
                return ranks
            }

            val overallRanks = competitionRanks(aggregates)
            val categoryRanks = aggregates.groupBy { it.latest.category }
                .values
                .flatMap { competitionRanks(it).entries }
                .associate { it.key to it.value }

            aggregates.map { agg ->
                ChallengeStandingEntity(
                    seasonYear = seasonYear,
                    distanceLabel = label,
                    runnerKey = agg.key,
                    myChronoRunnerId = agg.latest.runnerId,
                    fullName = agg.latest.fullName,
                    category = agg.latest.category,
                    sex = agg.latest.sex,
                    bibNumber = agg.latest.bibNumber,
                    totalPoints = agg.counted.sum(),
                    racesCounted = agg.counted.size,
                    racesRun = agg.runs,
                    lowestCountedPoints = agg.counted.lastOrNull() ?: 0,
                    overallRank = overallRanks[agg.key] ?: 0,
                    categoryRank = categoryRanks[agg.key] ?: 0,
                    computedAtEpochMillis = computedAtEpochMillis
                )
            }
        }
    }

    /** Ligne du classement correspondant au profil : par identifiant MyChrono, sinon par dossard de la distance. */
    fun standingForProfile(
        profile: RunnerProfileEntity,
        seasonYear: Int,
        distanceKm: Int,
        allStandings: List<ChallengeStandingEntity>
    ): ProfileChallengeStanding? {
        val label = if (distanceKm == 5) MyChronoApiClient.LABEL_5KM else MyChronoApiClient.LABEL_10KM
        val bib = (if (distanceKm == 5) profile.defaultBib5km else profile.defaultBib10km).trim()
        if (bib.isBlank()) return null

        val inDistance = allStandings.filter { it.seasonYear == seasonYear && it.distanceLabel == label }
        if (inDistance.isEmpty()) return null

        val runnerId = profile.myChronoRunnerId.trim()
        val mine = (if (runnerId.isNotBlank()) inDistance.firstOrNull { it.myChronoRunnerId == runnerId } else null)
            ?: inDistance.firstOrNull {
                (runnerId.isBlank() || it.myChronoRunnerId.isBlank()) && it.bibNumber.trimStart('0') == bib.trimStart('0')
            }
            ?: return null

        val nextAbove = inDistance
            .filter { it.totalPoints > mine.totalPoints }
            .minByOrNull { it.totalPoints }
        val gap = nextAbove?.let { it.totalPoints - mine.totalPoints + 1 }
        val racePointsNeeded = gap?.let {
            if (mine.racesCounted >= MAX_COUNTING_RACES) mine.lowestCountedPoints + it else it
        }

        return ProfileChallengeStanding(
            profileId = profile.profileId,
            profileName = profile.displayName,
            distanceLabel = label,
            standing = mine,
            runnersInDistance = inDistance.size,
            runnersInCategory = inDistance.count { it.category == mine.category },
            nextAbove = nextAbove,
            pointsGapToNextAbove = gap,
            racePointsNeededToPassNext = racePointsNeeded
        )
    }
}
