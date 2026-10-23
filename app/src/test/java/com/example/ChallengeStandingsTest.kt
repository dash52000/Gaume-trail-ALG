package com.example

import com.example.data.local.GaumeInitialSeedData
import com.example.data.remote.MyChronoApiClient
import com.example.data.remote.MyChronoRawResultRow
import com.example.domain.ChallengeStandingsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChallengeStandingsTest {

    private fun row(runnerId: String, name: String, cat: String, label: String, rank: Int, bib: String = "") =
        MyChronoRawResultRow(
            runnerId = runnerId,
            bibNumber = bib,
            fullName = name,
            club = "",
            category = cat,
            sex = "",
            distanceLabel = label,
            distanceKm = if (label == "5 km") 5.2 else 10.3,
            timeSeconds = 2400,
            timeFormatted = "40:00",
            overallRank = rank,
            totalFinishers = 100,
            categoryRank = 1,
            categoryTotal = 10,
            points = MyChronoApiClient.reglementPointsForRank(rank)
        )

    private fun raceId(n: Int) = MyChronoApiClient.raceIdFor(2026, n)

    @Test
    fun onlyBest15ResultsCount_andRanksArePerDistanceAndCategory() {
        // A : 16 courses (la plus faible ne compte pas). B : 3 courses. C : 5 km seulement.
        val rows = buildMap {
            for (n in 1..16) {
                put(
                    raceId(n),
                    listOfNotNull(
                        row("1", "A", "S", "10 km", rank = n),
                        if (n <= 3) row("2", "B", "V1", "10 km", rank = 50) else null,
                        if (n == 1) row("3", "C", "F", "5 km", rank = 1) else null
                    )
                )
            }
        }

        val standings = ChallengeStandingsCalculator.computeSeasonStandings(2026, rows, computedAtEpochMillis = 0)

        val a = standings.first { it.runnerKey == "1" }
        assertEquals(15, a.racesCounted)
        assertEquals(16, a.racesRun)
        assertEquals((1..15).sumOf { 1201 - it }, a.totalPoints)
        assertEquals(1201 - 15, a.lowestCountedPoints)
        assertEquals(1, a.overallRank)

        val b = standings.first { it.runnerKey == "2" }
        assertEquals(2, b.overallRank)
        assertEquals(1, b.categoryRank) // seul V1

        val c = standings.first { it.runnerKey == "3" }
        assertEquals("5 km", c.distanceLabel)
        assertEquals(1, c.overallRank) // classement séparé du 5 km
    }

    @Test
    fun equalTotalsShareTheSameRank() {
        val rows = mapOf(raceId(1) to listOf(row("1", "A", "S", "10 km", 10), row("2", "B", "S", "10 km", 20)),
            raceId(2) to listOf(row("1", "A", "S", "10 km", 20), row("2", "B", "S", "10 km", 10)),
            raceId(3) to listOf(row("3", "C", "S", "10 km", 5)))
        val standings = ChallengeStandingsCalculator.computeSeasonStandings(2026, rows, 0)
        assertEquals(1, standings.first { it.runnerKey == "1" }.overallRank)
        assertEquals(1, standings.first { it.runnerKey == "2" }.overallRank)
        assertEquals(3, standings.first { it.runnerKey == "3" }.overallRank)
    }

    @Test
    fun profileStanding_givesGapAndPointsNeededToPassNext() {
        val damien = GaumeInitialSeedData.buildInitialProfiles().first { it.profileId == "me" }
        val rows = (1..15).associate { n ->
            raceId(n) to listOf(
                row("1171", "COLLET Damien", "S", "10 km", rank = 80, bib = "697"),
                row("9", "DEVANT Coureur", "S", "10 km", rank = 78)
            )
        }
        val standings = ChallengeStandingsCalculator.computeSeasonStandings(2026, rows, 0)
        val mine = ChallengeStandingsCalculator.standingForProfile(damien, 2026, 10, standings)!!

        assertEquals(2, mine.standing.overallRank)
        assertEquals("DEVANT Coureur", mine.nextAbove?.fullName)
        val diff = 15 * 2 // 2 points d'écart par course
        assertEquals(diff + 1, mine.pointsGapToNextAbove)
        // 15 courses comptées : une nouvelle course remplace la plus faible (1121 pts).
        assertEquals(1121 + diff + 1, mine.racePointsNeededToPassNext)

        // Sans dossard 5 km, pas de classement 5 km.
        assertNull(ChallengeStandingsCalculator.standingForProfile(damien, 2026, 5, standings))
    }
}
