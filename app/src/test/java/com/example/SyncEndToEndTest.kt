package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlgDatabase
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.SeasonBibEntity
import com.example.data.remote.MyChronoApiClient
import com.example.data.remote.OpenMeteoClient
import com.example.data.repository.AlgRepository
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

/**
 * Synchro complète contre une API MyChrono simulée, avec les vraies valeurs de Sommethonne (manche 5,
 * 5 km de Damien, dossard 1150) et de Virton (manche 41, 10 km), plus une course hors challenge qui porte
 * le même numéro que la manche 5.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncEndToEndTest {

    private lateinit var db: AlgDatabase
    /** Appels à MyChrono (la météo n'est pas comptée). */
    var calls = 0

    private val calendar2026 = """
        {"races":[
          {"id":5,"competition":"Challenge ALG 2026","kind":"challenge","year":2026,"num":5,"date":"2026-02-22",
           "place":"Sommethonne","name":"A.L.de la Thonne","info":{"lat":49.5892,"lng":5.4519,"schedule":"10h00"},
           "status":"closed","published":true,"finishers":494,
           "distances":[{"id":4,"label":"10 km","kind":"adult","effective":10900},
                        {"id":5,"label":"5 km","kind":"adult","effective":5900},
                        {"id":6,"label":"Jeunes","kind":"kids","effective":1000,"noTime":true}]},
          {"id":111,"competition":"Hors challenge ALG 2026","kind":"single","year":2026,"num":5,"date":"2026-04-24",
           "place":"Sainte-Marie-sur-Semois","name":"A.L. de Sainte-Marie-sur-Semois","info":{},
           "status":"planned","published":false,"finishers":0,
           "distances":[{"id":90,"label":"12 km","kind":"adult","effective":12000},
                        {"id":91,"label":"6 km","kind":"adult","effective":6000}]},
          {"id":41,"competition":"Challenge ALG 2026","kind":"challenge","year":2026,"num":41,"date":"2026-10-18",
           "place":"Virton","name":"Le Jogging du TT Virton","info":{"lat":49.566,"lng":5.5376},
           "status":"closed","published":true,"finishers":401,
           "distances":[{"id":4,"label":"10 km","kind":"adult","effective":10400},
                        {"id":5,"label":"5 km","kind":"adult","effective":5500}]}
        ]}
    """.trimIndent()

    private val results5 = """
        {"race":{"id":5,"distances":[{"id":4,"label":"10 km","kind":"adult","effective":10900},
                                     {"id":5,"label":"5 km","kind":"adult","effective":5900}]},
         "results":[
           {"distanceId":4,"rows":[{"rank":1,"bib":700,"name":"AUTRE Coureur","cat":"S","time":2400001,"status":"finished","points":1200,"runnerId":9}]},
           {"distanceId":5,"rows":[{"rank":49,"bib":1150,"name":"COLLET Damien","sex":"M","cat":"S","catRank":20,
                                    "time":1821049,"status":"finished","points":1152,"runnerId":1171}]}]}
    """.trimIndent()

    private val results41 = """
        {"race":{"id":41,"distances":[{"id":4,"label":"10 km","kind":"adult","effective":10400},
                                      {"id":5,"label":"5 km","kind":"adult","effective":5500}]},
         "results":[
           {"distanceId":4,"rows":[{"rank":75,"bib":697,"name":"COLLET Damien","sex":"M","cat":"S","catRank":27,
                                    "time":3254075,"status":"finished","points":1126,"runnerId":1171}]},
           {"distanceId":5,"rows":[{"rank":84,"bib":1075,"name":"LAETITIA","sex":"F","cat":"F","catRank":13,
                                    "time":1866084,"status":"finished","points":1117,"runnerId":1202}]}]}
    """.trimIndent()

    /** Fiche coureur de Damien (vraies valeurs de Sommethonne). */
    private val runnerPage1171 = """
        {"runner":{"id":1171,"name":"COLLET Damien","club":"","city":"","sex":"M"},
         "competitions":[{"competition":{"id":3,"name":"Challenge ALG 2026","year":2026,"kind":"challenge"},
           "results":[
             {"raceId":5,"num":5,"date":"2026-02-22","place":"Sommethonne","raceName":"A.L.de la Thonne","distanceId":5,
              "distance":"5 km","len":5900,"lenReal":true,"status":"finished","time":1821049,"rank":49,"catRank":16,
              "cat":"S","points":1152,"pace":309,"speed":11.66,"finishers":206},
             {"raceId":41,"num":41,"date":"2026-10-18","place":"Virton","distanceId":4,"distance":"10 km","len":10400,
              "status":"finished","time":3254075,"rank":75,"catRank":27,"cat":"S","points":1126,"finishers":160}],
           "byDist":[]}]}
    """.trimIndent()

    /** Horloge des tests : avance d'une milliseconde à chaque lecture. */
    private val tick = AtomicLong(1_790_000_000_000L)

    var results5Body = results5
    var serveRunnerPage = true

    private val http = OkHttpClient.Builder().addInterceptor { chain ->
        val url = chain.request().url.toString()
        if (url.contains("mychrono")) calls++
        val body = when {
            url.contains("races?year=2026") -> calendar2026
            url.contains("races?year=") -> """{"races":[]}"""
            url.endsWith("/races/5/results") -> results5Body
            url.endsWith("/races/41/results") -> results41
            url.endsWith("/runners/1171") && serveRunnerPage -> runnerPage1171
            else -> null
        }
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(if (body != null) 200 else 404)
            .message(if (body != null) "OK" else "Not Found")
            .body((body ?: "").toResponseBody("application/json".toMediaType()))
            .build()
    }.build()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AlgDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun repository(prefs: SharedPreferences? = null) = AlgRepository(
        algDao = db.algDao(),
        myChronoApiClient = MyChronoApiClient(http),
        openMeteoClient = OpenMeteoClient(http),
        today = { LocalDate.of(2026, 10, 25) },
        database = db,
        syncPrefs = prefs,
        clock = { tick.incrementAndGet() } // Robolectric fige System.currentTimeMillis()
    )

    @Test
    fun fiveKmRaceIsFound_withOnlyThe10kmBibEntered_andWithAWrongPerRaceBib() = runBlocking {
        val dao = db.algDao()
        dao.upsertRunnerProfiles(GaumeInitialSeedData.buildInitialProfiles())
        dao.upsertSeasonBib(SeasonBibEntity("me", 2026, "697", ""))
        // Dossard 5 km tapé sur la fiche de la course alors qu'elle était sur le 10 km.
        dao.upsertBibOverride(RaceBibOverrideEntity("alg_2026_r05", "me", "1150", 10))

        val res = repository().syncAll()
        assertTrue(res.isSuccess)

        val races = dao.getAllRacesSnapshot().associateBy { it.raceId }
        assertEquals("Sommethonne", races.getValue("alg_2026_r05").village) // pas écrasée par le hors challenge
        assertEquals("Sainte-Marie-sur-Semois", races.getValue("alg_2026_h05").village)

        val mine = dao.getAllResultsSnapshot().filter { it.profileId == "me" }.associateBy { it.raceId }
        val sommethonne = mine.getValue("alg_2026_r05")
        assertEquals("5 km", sommethonne.distanceLabel)
        assertEquals("1150", sommethonne.bibNumber)
        assertEquals(1152, sommethonne.challengePoints)
        assertEquals("30:21", sommethonne.officialTimeFormatted)
        assertEquals("10 km", mine.getValue("alg_2026_r41").distanceLabel)

        // Deuxième synchro : les manches closes depuis plus de 3 semaines ne sont pas retéléchargées.
        calls = 0
        repository().syncAll()
        // 3 calendriers (2025, 2026, 2027) + Virton, close depuis moins de 3 semaines + 3 fiches coureur
        assertEquals(3 + 1 + 3, calls)
    }

    /**
     * Cas réel : le classement de Sommethonne gardé sur le téléphone a le 5 km de Damien sous un ancien
     * identifiant (fiches fusionnées depuis chez MyChrono) et la course, close depuis des mois, n'est plus
     * retéléchargée. Sans dossard 5 km saisi, rien ne le retrouvait. La fiche coureur le signale.
     */
    @Test
    fun staleRaceRowsAreRefetched_whenTheRunnerPageListsTheRace() = runBlocking {
        val dao = db.algDao()
        dao.upsertRunnerProfiles(GaumeInitialSeedData.buildInitialProfiles())
        dao.upsertSeasonBib(SeasonBibEntity("me", 2026, "697", ""))

        // Version précédente : classement gardé avec l'ancien identifiant, fiche coureur pas lue.
        results5Body = results5.replace("\"runnerId\":1171", "\"runnerId\":8888")
        serveRunnerPage = false
        repository().syncAll()
        assertTrue(dao.getAllResultsSnapshot().none { it.profileId == "me" && it.raceId == "alg_2026_r05" })

        // MyChrono publie maintenant le résultat sous 1171, et la fiche coureur est lue.
        results5Body = results5
        serveRunnerPage = true
        val repo = repository()
        val message = repo.syncAll().getOrThrow()
        assertTrue(message, message.contains("fiche MyChrono"))
        assertTrue(message, !message.contains("repris directement"))

        val sommethonne = dao.getAllResultsSnapshot().single { it.profileId == "me" && it.raceId == "alg_2026_r05" }
        assertEquals("5 km", sommethonne.distanceLabel)
        assertEquals(1152, sommethonne.challengePoints)
        assertTrue(dao.getAllStandingsSnapshot().any { it.distanceLabel == "5 km" && it.myChronoRunnerId == "1171" })
        val report = repo.diagnosticsFlow.value.profiles.single { it.profileId == "me" }
        assertEquals(1, report.onRunnerPageBySeason?.get(2026)?.get("5 km"))
    }

    /** Après la mise à jour de l'appli, tous les classements gardés sont retéléchargés une seule fois. */
    @Test
    fun appUpdate_refetchesEveryPublishedRaceOnce() = runBlocking {
        val dao = db.algDao()
        dao.upsertRunnerProfiles(GaumeInitialSeedData.buildInitialProfiles())
        repository().syncAll() // ancienne version

        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_sync_${System.nanoTime()}", Context.MODE_PRIVATE)
        calls = 0
        val message = repository(prefs).syncAll().getOrThrow()
        assertEquals(3 + 2 + 3, calls) // calendriers + Sommethonne et Virton + fiches coureur
        assertTrue(message, message.contains("2 classement(s) retéléchargé(s) (mise à jour)"))
        assertEquals(AlgRepository.CACHE_FORMAT, prefs.getInt("cache_format", 0))

        calls = 0
        repository(prefs).syncAll()
        assertEquals(3 + 1 + 3, calls) // retour au fonctionnement normal
    }

    /** Classement publié sans la ligne de Damien, alors que sa fiche a la course : résultat repris de la fiche. */
    @Test
    fun resultMissingFromRaceResults_isTakenFromTheRunnerPage() = runBlocking {
        val dao = db.algDao()
        dao.upsertRunnerProfiles(GaumeInitialSeedData.buildInitialProfiles())
        dao.upsertSeasonBib(SeasonBibEntity("me", 2026, "697", "1150"))
        results5Body = """
            {"race":{"id":5,"distances":[{"id":4,"label":"10 km","kind":"adult","effective":10900},
                                         {"id":5,"label":"5 km","kind":"adult","effective":5900}]},
             "results":[
               {"distanceId":4,"rows":[{"rank":1,"bib":700,"name":"AUTRE Coureur","cat":"S","time":2400001,"status":"finished","points":1200,"runnerId":9}]},
               {"distanceId":5,"rows":[{"rank":1,"bib":1001,"name":"RAPIDE Anne","sex":"F","cat":"F","time":1500001,"status":"finished","points":1200,"runnerId":10}]}]}
        """.trimIndent()

        val message = repository().syncAll().getOrThrow()
        assertTrue(message, message.contains("1 repris directement de la fiche"))

        val sommethonne = dao.getAllResultsSnapshot().single { it.profileId == "me" && it.raceId == "alg_2026_r05" }
        assertEquals("5 km", sommethonne.distanceLabel)
        assertEquals("1150", sommethonne.bibNumber)
        assertEquals(1152, sommethonne.challengePoints)
        assertEquals("30:21", sommethonne.officialTimeFormatted)
        assertEquals(49, sommethonne.overallRank)
        assertEquals(206, sommethonne.totalFinishers)
        assertEquals(16, sommethonne.categoryRank)
        val standing = dao.getAllStandingsSnapshot().single { it.distanceLabel == "5 km" && it.myChronoRunnerId == "1171" }
        assertEquals(1152, standing.totalPoints)

        // Synchro suivante : la ligne reprise de la fiche suffit, Sommethonne n'est pas retéléchargée.
        calls = 0
        repository().syncAll()
        assertEquals(3 + 1 + 3, calls)
    }
}
