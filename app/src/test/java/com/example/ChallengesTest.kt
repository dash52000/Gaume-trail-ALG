package com.example

import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceWeatherEntity
import com.example.data.local.SeasonGoalEntity
import com.example.data.remote.WikipediaClient
import com.example.domain.ChallengesCalculator
import com.example.domain.GoalsCalculator
import com.example.domain.RecordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChallengesTest {

    private fun race(id: String, year: Int, num: Int, place: String, date: String, km: Double = 10.0, completed: Boolean = true,
                     challenge: Boolean = true) = RaceEntity(
        raceId = id, seasonYear = year, roundNumber = num, myChronoEventId = id, title = "$place — course", village = place,
        dateIso = date, distanceKm = km, elevationGainMeters = null, isCompleted = completed, distance5Km = 5.0,
        isChallenge = challenge
    )

    private fun res(raceId: String, year: Int, seconds: Int, profile: String = "me", label: String = "10 km", km: Double? = null) =
        RaceResultEntity(
            raceId = raceId, profileId = profile, seasonYear = year, bibNumber = "697", distanceLabel = label,
            officialTimeSeconds = seconds, officialTimeFormatted = "%d:%02d".format(seconds / 60, seconds % 60),
            pacePerKmFormatted = "", speedKmh = 0.0, overallRank = 10, totalFinishers = 100, categoryRank = 3,
            categoryTotal = 30, challengePoints = 1100, distanceKm = km
        )

    // Vraies valeurs MyChrono de Damien à Rulles et Mussy-la-Ville (2025 et 2026).
    private val races = listOf(
        race("a40", 2025, 40, "Rulles", "2025-10-12"),
        race("a34", 2025, 34, "Mussy-la-Ville", "2025-08-31"),
        race("a10", 2025, 10, "Chiny", "2025-03-30", km = 10.0),
        race("b40", 2026, 40, "Rulles", "2026-10-11"),
        race("b34", 2026, 34, "Mussy-la-Ville", "2026-08-30"),
        race("b10", 2026, 10, "Chiny", "2026-03-29", km = 10.9),
        race("c40", 2027, 40, "Rulles", "2027-10-10", completed = false)
    )
    private val results = listOf(
        res("a40", 2025, 2928), res("b40", 2026, 3323),            // 48:48 → 55:23
        res("a34", 2025, 3062), res("b34", 2026, 3288),            // 51:02 → 54:48
        res("a10", 2025, 3000, km = 10.0), res("b10", 2026, 3200, km = 10.9) // plus lent, mais parcours plus long
    )

    @Test
    fun records_duelAgainstPreviousEditions() {
        val r2026 = ChallengesCalculator.records("me", "10 km", 2026, races, results)
        val rulles = r2026.byRaceId.getValue("b40")
        assertEquals(RecordStatus.NOT_BEATEN, rulles.status)
        assertEquals(395, rulles.deltaSeconds) // 55:23 − 48:48
        assertEquals("+6:35", GoalsCalculator.formatDelta(rulles.deltaSeconds!!))
        assertEquals(2025, rulles.best.seasonYear)

        // Longueur changée (10,0 → 10,9 km) : comparaison à l'allure, record battu malgré un temps plus long.
        val chiny = r2026.byRaceId.getValue("b10")
        assertEquals(RecordStatus.BEATEN, chiny.status)
        assertTrue(chiny.deltaSeconds!! < 0)
        assertEquals(1, r2026.beaten)
        assertEquals(2, r2026.notBeaten)

        // Vu depuis 2027 : Rulles est à battre, avec la meilleure édition (2025).
        val r2027 = ChallengesCalculator.records("me", "10 km", 2027, races, results)
        val toBeat = r2027.byRaceId.getValue("c40")
        assertEquals(RecordStatus.TO_BEAT, toBeat.status)
        assertEquals("48:48", toBeat.best.timeFormatted)
    }

    @Test
    fun badges_comeFromFacts() {
        val virton = listOf(
            race("v1", 2026, 1, "Virton", "2026-01-25"),
            race("v41", 2026, 41, "Virton", "2026-10-18"),
            race("hc1", 2026, 1, "Suxy", "2026-01-04", challenge = false)
        )
        val allRaces = races + virton
        val all = results + listOf(
            res("v1", 2026, 2700), res("v41", 2026, 3254),
            res("b34", 2026, 1800, label = "5 km"),
            res("v41", 2026, 1900, profile = "laetitia", label = "5 km"), res("v41", 2026, 1950, profile = "elodie", label = "5 km"),
            res("hc1", 2026, 4380)
        )
        val weather = mapOf(
            "v1" to RaceWeatherEntity("v1", 10, 1.5, 0.0, 3.0, 10.0, 3, 0.0, 3.0, "test", 0),
            "v41" to RaceWeatherEntity("v41", 10, 9.4, 2.4, 25.0, 12.0, 61, 7.0, 11.0, "test", 0)
        )
        val badges = ChallengesCalculator.badges(
            profileId = "me", trioProfileIds = listOf("me", "laetitia", "elodie"), allRaces = allRaces, allResults = all,
            courseFactors = emptyMap(), weatherByRaceId = weather, recordsBeatenEver = 1, villagesVisited = 6
        ).associateBy { it.id }

        assertTrue(badges.getValue("tout_virton").unlocked)
        assertTrue(badges.getValue("double").unlocked)
        assertTrue(badges.getValue("sous_la_flotte").unlocked)
        assertTrue(badges.getValue("apres_deluge").unlocked) // 25 mm sur les 3 jours avant Virton
        assertTrue(badges.getValue("froid_de_canard").unlocked) // 1,5 °C au départ
        assertFalse(badges.getValue("coup_de_chaud").unlocked)
        assertTrue(badges.getValue("fidele").unlocked)
        assertTrue(badges.getValue("hors_piste").unlocked)
        assertTrue(badges.getValue("trio_complet").unlocked)
        assertFalse(badges.getValue("villages_10").unlocked)
        assertEquals(6, badges.getValue("villages_10").progress)
        assertFalse(badges.getValue("challenge_valide").unlocked)
    }

    @Test
    fun trioChallenges_areCooperative() {
        val season = listOf(
            race("m1", 2026, 1, "Virton", "2026-01-25"),
            race("m2", 2026, 2, "Ethe", "2026-02-01"),
            race("m3", 2026, 3, "Florenville", "2026-02-08", completed = false)
        )
        val all = listOf(
            res("m1", 2026, 3000), res("m1", 2026, 1800, profile = "laetitia", label = "5 km"),
            res("m1", 2026, 1850, profile = "elodie", label = "5 km"),
            res("m2", 2026, 1700, profile = "elodie", label = "5 km")
        )
        val c = ChallengesCalculator.trioChallenges(2026, listOf("me", "laetitia", "elodie"), season, all).associateBy { it.id }
        assertEquals(2, c.getValue("couverture").progress)
        assertEquals(3, c.getValue("couverture").target)
        assertTrue(c.getValue("couverture").detail!!.contains("Florenville"))
        assertEquals(1, c.getValue("a_trois").progress)
        assertEquals(25, c.getValue("km_trio").progress) // 10 + 5 + 5 + 5
    }

    @Test
    fun goals_trackWhatTheRunnerChose() {
        val goals = listOf(SeasonGoalEntity("me", 2026, GoalsCalculator.RACES, 20))
        val p = GoalsCalculator.progress(
            profileId = "me", goalSeason = 2026, preferredDistanceLabel = "10 km", goals = goals, allRaces = races,
            allResults = results, recordsBeatenInSeason = 1, newVillagesInSeason = 0, coursesRunTwiceAvailable = 3
        ).associateBy { it.type }
        assertEquals(20, p.getValue(GoalsCalculator.RACES).target)
        assertEquals(3, p.getValue(GoalsCalculator.RACES).progress)
        assertNull(p.getValue(GoalsCalculator.KM).target)
        assertEquals(31, p.getValue(GoalsCalculator.KM).progress) // 10 + 10 + 10,9
        assertEquals(1, p.getValue(GoalsCalculator.RECORDS).progress)
        assertEquals(3, p.getValue(GoalsCalculator.CHALLENGE).progress)
    }

    @Test
    fun wikipedia_onlyKeepsBelgianOrLuxembourgPlaces() {
        val virton = """{"type":"standard","title":"Virton","description":"ville de Belgique",
            "extract":"Virton est une ville francophone de Belgique située en Région wallonne.",
            "content_urls":{"mobile":{"page":"https://fr.m.wikipedia.org/wiki/Virton"}}}"""
        val parsed = WikipediaClient.parseSummary(virton)
        assertNotNull(parsed)
        assertEquals("https://fr.m.wikipedia.org/wiki/Virton", parsed!!.pageUrl)

        val ambiguous = """{"type":"disambiguation","title":"Pin","extract":"Pin peut désigner…"}"""
        assertNull(WikipediaClient.parseSummary(ambiguous))

        val elsewhere = """{"type":"standard","title":"Termes","description":"commune française",
            "extract":"Termes est une commune française située dans le département de l'Aude."}"""
        assertNull(WikipediaClient.parseSummary(elsewhere))
        assertEquals(listOf("Rabais", "Rabais (Belgique)", "Rabais (Luxembourg)"), WikipediaClient.candidateTitles("Rabais (Virton)"))
    }
}
