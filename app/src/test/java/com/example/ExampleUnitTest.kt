package com.example

import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.remote.MyChronoApiClient
import com.example.domain.AlgChallengeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun separate10kmAnd5kmChallenges_neverMixResults() {
        val profile = GaumeInitialSeedData.buildInitialProfiles().first { it.profileId == "me" }
            .copy(defaultBib5km = "1150")
        val races = (1..18).map { round ->
            RaceEntity(
                raceId = "2026_r$round",
                seasonYear = 2026,
                roundNumber = round,
                myChronoEventId = "ev_$round",
                title = "Manche #$round",
                village = "Virton",
                dateIso = "2026-05-%02d".format(round),
                distanceKm = 10.0,
                elevationGainMeters = 180,
                isCompleted = true,
                isSeasonFinale = round == 18
            )
        }

        val results10km = (1..16).map { round ->
            RaceResultEntity(
                raceId = "2026_r$round",
                profileId = "me",
                distanceLabel = "10 km",
                seasonYear = 2026,
                bibNumber = "697",
                officialTimeSeconds = 2400 + round * 10,
                officialTimeFormatted = "40:00",
                speedKmh = 15.0,
                pacePerKmFormatted = "4:00 /km",
                overallRank = round,
                totalFinishers = 200,
                categoryRank = round,
                categoryTotal = 50,
                challengePoints = 900 + round
            )
        }

        val results5km = listOf(1, 2, 17).map { round ->
            RaceResultEntity(
                raceId = "2026_r$round",
                profileId = "me",
                distanceLabel = "5 km",
                seasonYear = 2026,
                bibNumber = "1150",
                officialTimeSeconds = 1150 + round * 5,
                officialTimeFormatted = "19:15",
                speedKmh = 15.6,
                pacePerKmFormatted = "3:51 /km",
                overallRank = 3,
                totalFinishers = 150,
                categoryRank = 2,
                categoryTotal = 40,
                challengePoints = 980 + round
            )
        }

        val summary = AlgChallengeCalculator.computeChallengeSummary(
            profile = profile,
            seasonYear = 2026,
            racesInSeason = races,
            allSeasonResults = results10km + results5km,
            allActivityMetrics = emptyList()
        )

        assertEquals(16, summary.challenge10km.completedRacesCount)
        assertEquals(15, summary.challenge10km.countingRacesCount)
        assertTrue(summary.challenge10km.isChallengeComplete)
        assertFalse(summary.challenge10km.countingRaceIds.contains("2026_r1"))

        val expected10kmTotal = (2..16).sumOf { 900 + it }
        assertEquals(expected10kmTotal, summary.challenge10km.totalChallengePoints)

        assertEquals(3, summary.challenge5km.completedRacesCount)
        assertEquals(3, summary.challenge5km.countingRacesCount)
        assertEquals(12, summary.challenge5km.remainingToComplete15)
        assertFalse(summary.challenge5km.isChallengeComplete)
    }

    @Test
    fun top15Simulator_projectsNetPointsGainForPlannedRaces() {
        val profile = GaumeInitialSeedData.buildInitialProfiles().first { it.profileId == "me" }
        val races = (1..5).map { round ->
            RaceEntity(
                raceId = "2026_r$round",
                seasonYear = 2026,
                roundNumber = round,
                myChronoEventId = "ev_$round",
                title = "Allure Libre #$round",
                village = "Pin",
                dateIso = "2026-04-%02d".format(round),
                distanceKm = 10.0,
                elevationGainMeters = 150,
                isCompleted = round <= 2,
                isSeasonFinale = false
            )
        }

        val existing10km = listOf(1, 2).map { round ->
            RaceResultEntity(
                raceId = "2026_r$round",
                profileId = "me",
                distanceLabel = "10 km",
                seasonYear = 2026,
                bibNumber = "697",
                officialTimeSeconds = 2400,
                officialTimeFormatted = "40:00",
                speedKmh = 15.0,
                pacePerKmFormatted = "4:00 /km",
                overallRank = 10,
                totalFinishers = 200,
                categoryRank = 4,
                categoryTotal = 50,
                challengePoints = 940
            )
        }

        val plannedTw = listOf(
            RaceTerrainWeatherEntity(
                raceId = "2026_r3",
                profileId = "me",
                terrainCondition = "Boueux",
                weatherCondition = "Pluie",
                temperatureC = 11,
                terrainNotes = "Forêt de Pin humide",
                isPlannedForSimulator = true,
                plannedDistanceKm = 10,
                simulatedTargetPoints = 960
            )
        )

        val summary = AlgChallengeCalculator.computeChallengeSummary(
            profile = profile,
            seasonYear = 2026,
            racesInSeason = races,
            allSeasonResults = existing10km,
            allActivityMetrics = emptyList(),
            allTerrainWeather = plannedTw
        )

        assertEquals(2, summary.simulator10km.currentCountingRaces)
        assertEquals(1, summary.simulator10km.plannedRacesCount)
        assertEquals(3, summary.simulator10km.projectedCountingRaces)
        assertEquals(1880, summary.simulator10km.currentTotalPoints)
        assertEquals(2840, summary.simulator10km.projectedTotalPoints)
        assertEquals(960, summary.simulator10km.netPointsGain)
    }

    @Test
    fun gaumeExactGpsAndTerritoryConquest_resolvesVillagesAndUnlocksTerroirBadges() {
        val profile = GaumeInitialSeedData.buildInitialProfiles().first { it.profileId == "me" }
        val villages = listOf(
            "Florenville",
            "Pin",
            "Chiny",
            "Rulles",
            "Étalle",
            "Virton",
            "Meix-devant-Virton",
            "Saint-Léger"
        )
        val races = villages.mapIndexed { idx, villageName ->
            RaceEntity(
                raceId = "2026_r${idx + 1}",
                seasonYear = 2026,
                roundNumber = idx + 1,
                myChronoEventId = "ev_${idx + 1}",
                title = "Allure Libre de $villageName",
                village = villageName,
                dateIso = "2026-06-0${idx + 1}",
                distanceKm = 10.0,
                elevationGainMeters = 180,
                isCompleted = true,
                isSeasonFinale = false
            )
        }

        val florenvilleGeo = GaumeInitialSeedData.resolveGaumeGeoForRace(races[0])
        assertEquals("Florenville", florenvilleGeo.canonicalVillage)
        assertEquals("orval_semois", florenvilleGeo.territoryId)
        assertEquals(49.6992, florenvilleGeo.latitude, 0.01)
        assertEquals(5.3103, florenvilleGeo.longitude, 0.01)

        val meixGeo = GaumeInitialSeedData.resolveGaumeGeoForRace(races[6])
        assertEquals("Meix-devant-Virton", meixGeo.canonicalVillage)
        assertEquals(49.6056, meixGeo.latitude, 0.01)
        assertEquals(5.4806, meixGeo.longitude, 0.01)

        val results10km = races.mapIndexed { idx, race ->
            RaceResultEntity(
                raceId = race.raceId,
                profileId = "me",
                distanceLabel = "10 km",
                seasonYear = 2026,
                bibNumber = "697",
                officialTimeSeconds = 2400 + idx * 15,
                officialTimeFormatted = "40:15",
                speedKmh = 14.9,
                pacePerKmFormatted = "4:01 /km",
                overallRank = 12,
                totalFinishers = 210,
                categoryRank = 5,
                categoryTotal = 55,
                challengePoints = 945
            )
        }

        val mudEntry = listOf(
            RaceTerrainWeatherEntity(
                raceId = "2026_r1",
                profileId = "me",
                terrainCondition = "Boueux",
                weatherCondition = "Pluie",
                temperatureC = 10,
                terrainNotes = "Boue en forêt d'Orval",
                isPlannedForSimulator = false,
                plannedDistanceKm = 10,
                simulatedTargetPoints = null
            )
        )

        val summary = AlgChallengeCalculator.computeChallengeSummary(
            profile = profile,
            seasonYear = 2026,
            racesInSeason = races,
            allSeasonResults = results10km,
            allActivityMetrics = emptyList(),
            allTerrainWeather = mudEntry
        )

        val conquest = summary.conquest10km
        assertEquals(8, conquest.conqueredVillagesCount)
        assertEquals(6, conquest.territoriesConqueredCount)
        assertTrue(conquest.xpPoints >= 2500)
        assertTrue(conquest.badges.any { it.badgeId == "badge_orval_semois" && it.isUnlocked })
        assertTrue(conquest.badges.any { it.badgeId == "badge_grand_duc_gaume" && it.isUnlocked })
        assertTrue(conquest.badges.any { it.badgeId == "badge_sanglier_gaumais" && it.isUnlocked })
    }

    @Test
    fun pronosticPrevuVsReel_computesUpcomingAndPostRaceVerdict() {
        val profile = GaumeInitialSeedData.buildInitialProfiles().first { it.profileId == "me" }
        val races = listOf(
            RaceEntity("2026_r1", 2026, 1, "e1", "Manche 1", "Florenville", "2026-03-15", 10.0, 120, true, false),
            RaceEntity("2026_r2", 2026, 2, "e2", "Manche 2", "Pin", "2026-03-29", 10.0, 150, true, false),
            RaceEntity("2026_r3", 2026, 3, "e3", "Manche 3", "Chiny", "2026-04-12", 10.0, 210, false, false)
        )
        val results = listOf(
            RaceResultEntity(
                raceId = "2026_r1",
                profileId = "me",
                distanceLabel = "10 km",
                seasonYear = 2026,
                bibNumber = "697",
                officialTimeSeconds = 2520,
                officialTimeFormatted = "42:00",
                speedKmh = 14.3,
                pacePerKmFormatted = "4:12 /km",
                overallRank = 15,
                totalFinishers = 200,
                categoryRank = 5,
                categoryTotal = 45,
                challengePoints = 935
            ),
            RaceResultEntity(
                raceId = "2026_r2",
                profileId = "me",
                distanceLabel = "10 km",
                seasonYear = 2026,
                bibNumber = "697",
                officialTimeSeconds = 2480,
                officialTimeFormatted = "41:20",
                speedKmh = 14.5,
                pacePerKmFormatted = "4:08 /km",
                overallRank = 12,
                totalFinishers = 200,
                categoryRank = 4,
                categoryTotal = 45,
                challengePoints = 950
            )
        )
        val summary = AlgChallengeCalculator.computeChallengeSummary(
            profile = profile,
            seasonYear = 2026,
            racesInSeason = races,
            allSeasonResults = results,
            allActivityMetrics = emptyList()
        )
        val pronostics = com.example.ui.components.GaumePerfsCalculator.computeSeasonPronostics(summary, 10)
        assertEquals(0, pronostics.completedWithVerdict.size)
        assertEquals(2, pronostics.recentReferenceRaces.size)
        assertEquals(1, pronostics.upcomingPronostics.size)
        assertEquals("2026_r3", pronostics.nextUpcomingPronostic?.raceId)
        assertEquals(null, pronostics.allPronosticsByRaceId["2026_r1"])
        assertEquals(null, pronostics.allPronosticsByRaceId["2026_r2"])
        val r3Prono = pronostics.allPronosticsByRaceId["2026_r3"]!!
        assertEquals(com.example.ui.components.GaumeRacePronosticVerdict.UPCOMING, r3Prono.verdict)
        assertTrue(r3Prono.predictedTimeSeconds > 2480)
    }

    @Test
    fun myChronoParser_extractsBoth5kmAnd10kmDistances() {
        val sampleJson = """
            [
              {
                "id": "1171",
                "full_name": "Damien Collet",
                "dossard": "697",
                "distance_km": 10.2,
                "time": "41:12",
                "place": 18,
                "points": 946
              },
              {
                "id": "1075",
                "full_name": "Laetitia",
                "dossard": "1075",
                "distance_km": 5.2,
                "time": "25:30",
                "place": 14,
                "points": 918
              }
            ]
        """.trimIndent()

        val client = MyChronoApiClient()
        val rawRows = client.parseResultsPayload(sampleJson)
        assertEquals(2, rawRows.size)

        val profiles = GaumeInitialSeedData.buildInitialProfiles()
        val damienProfile = profiles.first { it.profileId == "me" }
        val laetitiaProfile = profiles.first { it.profileId == "laetitia" }

        val damienMatches = client.findMatchingResultsForProfile(
            raceId = "2026_round_01",
            seasonYear = 2026,
            profile = damienProfile,
            raceOverride = null,
            rawRows = rawRows
        )
        val laetitiaMatches = client.findMatchingResultsForProfile(
            raceId = "2026_round_01",
            seasonYear = 2026,
            profile = laetitiaProfile,
            raceOverride = null,
            rawRows = rawRows
        )

        assertEquals(1, damienMatches.size)
        assertEquals("10 km", damienMatches.first().distanceLabel)
        assertEquals("697", damienMatches.first().bibNumber)

        assertEquals(1, laetitiaMatches.size)
        assertEquals("5 km", laetitiaMatches.first().distanceLabel)
        assertEquals("1075", laetitiaMatches.first().bibNumber)
    }
}
