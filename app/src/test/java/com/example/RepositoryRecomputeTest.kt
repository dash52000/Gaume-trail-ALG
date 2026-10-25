package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlgDatabase
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.MyChronoRowEntity
import com.example.data.local.RaceEntity
import com.example.data.local.SeasonBibEntity
import com.example.data.repository.AlgRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Recalcul local d'une saison à partir des résultats bruts gardés sur le téléphone :
 * une course hors challenge donne un résultat au Trio mais ne compte jamais pour le challenge.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryRecomputeTest {

    private lateinit var db: AlgDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AlgDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun row(raceId: String, isChallenge: Boolean, idx: Int, runnerId: String, bib: String, name: String,
                    label: String, distanceName: String, km: Double, seconds: Int, rank: Int, points: Int) =
        MyChronoRowEntity(
            raceId = raceId, distanceLabel = label, rowIndex = idx, seasonYear = 2026, isChallenge = isChallenge,
            distanceName = distanceName, distanceKm = km, runnerId = runnerId, bibNumber = bib, fullName = name,
            club = "", category = "S", sex = "M", timeSeconds = seconds, overallRank = rank, totalFinishers = 2,
            categoryRank = rank, categoryTotal = 2, points = points
        )

    @Test
    fun horsChallengeResult_isKeptButNeverCountsInTheStandings() = runBlocking {
        val dao = db.algDao()
        dao.upsertRunnerProfiles(GaumeInitialSeedData.buildInitialProfiles())
        dao.upsertSeasonBib(SeasonBibEntity("me", 2026, "697", ""))
        dao.upsertRaces(
            listOf(
                RaceEntity(
                    raceId = "alg_2026_r41", seasonYear = 2026, roundNumber = 41, myChronoEventId = "41",
                    title = "Virton — Le Jogging du TT Virton", village = "Virton", dateIso = "2026-10-18",
                    distanceKm = 10.4, elevationGainMeters = null, isCompleted = true, distance5Km = 5.5
                ),
                RaceEntity(
                    raceId = "alg_2026_h01", seasonYear = 2026, roundNumber = 1, myChronoEventId = "107",
                    title = "Suxy — Lu Raid Adjalé", village = "Suxy", dateIso = "2026-01-04",
                    distanceKm = 14.0, elevationGainMeters = null, isCompleted = true, distance5Km = 7.0,
                    isChallenge = false, longDistanceLabel = "14 km", shortDistanceLabel = "7 km"
                )
            )
        )
        dao.insertRows(
            listOf(
                row("alg_2026_r41", true, 0, "1171", "697", "COLLET Damien", "10 km", "10 km", 10.4, 3254, 1, 1200),
                row("alg_2026_r41", true, 1, "9", "500", "AUTRE Coureur", "10 km", "10 km", 10.4, 3300, 2, 1199),
                row("alg_2026_h01", false, 0, "1171", "12", "COLLET Damien", "10 km", "14 km", 14.0, 4380, 1, 1200)
            )
        )

        AlgRepository(dao, database = db).recomputeSeason(2026)

        val results = dao.getAllResultsSnapshot().filter { it.profileId == "me" }.associateBy { it.raceId }
        assertEquals(1200, results.getValue("alg_2026_r41").challengePoints)
        val hc = results.getValue("alg_2026_h01")
        assertEquals(0, hc.challengePoints)
        assertEquals("14 km", hc.distanceName)
        assertEquals("1:13:00", hc.officialTimeFormatted)

        val standings = db.query("SELECT totalPoints, racesRun FROM challenge_standings WHERE myChronoRunnerId = '1171'", null)
        assertTrue(standings.moveToFirst())
        assertEquals(1200, standings.getInt(0)) // la course hors challenge n'ajoute rien
        assertEquals(1, standings.getInt(1))
        standings.close()
    }
}
