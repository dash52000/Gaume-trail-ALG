package com.example

import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.remote.MyChronoApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Format réel de l'API MyChrono (relevé sur https://mychrono.softalux.com/api/public, saison 2026).
 * Valeurs reprises des manches 1, 41 et 42 ; les noms des autres coureurs sont anonymisés.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MyChronoRealDataTest {

    private val client = MyChronoApiClient()

    private val racesJson = """
        {"races":[
          {"id":1,"competitionId":2,"competition":"Challenge ALG 2026","org":"ALG","orgId":1,"kind":"challenge",
           "year":2026,"num":1,"date":"2026-01-25","place":"Virton","name":"La Doyenne",
           "info":{"address":"Institut de la Ste-Famille, rue de Montmédy.","lat":49.56678530838471,"lng":5.524626965247194,
             "schedule":"10h00 – 4,8 et 10 km","kids":"09h45 – 1 km pour 7/12 ans (Challenge Jeunes)",
             "terrain":"Route et chemins campagnards.",
             "contact":{"name":"X","phone":"0","email":"x@example.org"},
             "facilities":{"garderie":true,"douche":true,"vestiaire":true},
             "maps":[{"label":"10 km","url":"https://www.openrunner.com/embed/aaa?unit=metric"},
                     {"label":"5 km","url":"https://www.openrunner.com/embed/bbb?unit=metric"}],
             "source":"https://allures-libres-de-gaume.be/?p=77"},
           "status":"closed","published":true,"entries":767,"finishers":767,
           "distances":[
             {"id":4,"label":"10 km","kind":"adult","length":9800,"real":9800,"effective":9800},
             {"id":5,"label":"5 km","kind":"adult","length":4800,"real":4800,"effective":4800},
             {"id":6,"label":"Jeunes","kind":"kids","length":1000,"real":null,"effective":1000}]},
          {"id":41,"year":2026,"num":41,"date":"2026-10-18","place":"Virton","name":"Le Jogging du TT Virton",
           "info":{"lat":49.56597079010033,"lng":5.537617074928778},
           "status":"closed","published":true,
           "distances":[
             {"id":4,"label":"10 km","kind":"adult","length":10000,"real":10400,"effective":10400},
             {"id":5,"label":"5 km","kind":"adult","length":5000,"real":5500,"effective":5500},
             {"id":6,"label":"Jeunes","kind":"kids","length":1000,"real":null,"effective":1000}]},
          {"id":42,"year":2026,"num":42,"date":"2026-10-25","place":"Dampicourt","name":"L’A.L. Jacques MARISCHAL",
           "info":{"lat":49.555213759342415,"lng":5.49476561600375},
           "status":"planned","published":false,
           "distances":[
             {"id":4,"label":"10 km","kind":"adult","length":10000,"real":null,"effective":10000},
             {"id":5,"label":"5 km","kind":"adult","length":5000,"real":null,"effective":5000}]}
        ]}
    """.trimIndent()

    /** Extrait réel des résultats de la manche 41 (Virton, 18/10/2026). */
    private val race41ResultsJson = """
        {"race":{"id":41,"num":41,"place":"Virton","status":"closed",
           "distances":[
             {"id":4,"label":"10 km","kind":"adult","effective":10400},
             {"id":5,"label":"5 km","kind":"adult","effective":5500},
             {"id":6,"label":"Jeunes","kind":"kids","effective":1000}]},
         "results":[
           {"distanceId":4,"rows":[
             {"rank":75,"bib":697,"name":"COLLET Damien","sex":"M","cat":"S","catRank":27,"sexRank":67,"club":"","city":"",
              "time":3254075,"status":"finished","points":1126,"pace":313,"runnerId":1171},
             {"rank":107,"bib":1075,"name":"AUTRE Coureur","sex":"M","cat":"S","catRank":38,"sexRank":92,"club":"","city":"",
              "time":3517107,"status":"finished","points":1094,"pace":338,"runnerId":1833}]},
           {"distanceId":5,"rows":[
             {"rank":65,"bib":1076,"name":"ELODIE","sex":"F","cat":"F","catRank":8,"sexRank":15,"club":"","city":"",
              "time":1774065,"status":"finished","points":1134,"pace":322,"runnerId":1189},
             {"rank":84,"bib":1075,"name":"LAETITIA","sex":"F","cat":"F","catRank":13,"sexRank":24,"club":"","city":"",
              "time":1866084,"status":"finished","points":1117,"pace":339,"runnerId":1202}]},
           {"distanceId":6,"rows":[
             {"rank":1,"bib":0,"name":"ENFANT","sex":"M","cat":"J","catRank":1,"time":0,"status":"finished","points":0,"runnerId":5}]}
         ]}
    """.trimIndent()

    private val profiles = GaumeInitialSeedData.buildInitialProfiles()
    private val damien = profiles.first { it.profileId == "me" }
    private val laetitia = profiles.first { it.profileId == "laetitia" }
    private val elodie = profiles.first { it.profileId == "elodie" }

    @Test
    fun races_useOnlyPublishedData() {
        val races = client.parseRacesPayload(racesJson, 2026)
        assertEquals(3, races.size)

        val r1 = races.first { it.roundNumber == 1 }
        assertEquals("alg_2026_r01", r1.raceId)
        assertEquals("1", r1.myChronoEventId)
        assertEquals("Virton — La Doyenne", r1.title)
        assertEquals(9.8, r1.distanceKm, 0.001)
        assertEquals(4.8, r1.distance5Km!!, 0.001)
        assertEquals(49.5668, r1.startLatitude!!, 0.0001)
        assertEquals("Institut de la Ste-Famille, rue de Montmédy.", r1.address)
        assertEquals(true, r1.hasDouche)
        assertNull(r1.hasResto) // non publié => inconnu, pas « non »
        assertEquals(2, r1.routeLinkList().size)
        assertNull(r1.elevationGainMeters) // MyChrono ne publie pas de D+
        assertTrue(r1.isCompleted)

        val r41 = races.first { it.roundNumber == 41 }
        assertEquals(10.4, r41.distanceKm, 0.001) // longueur réelle, pas la longueur annoncée
        assertEquals(5.5, r41.distance5Km!!, 0.001)
        assertNull(r41.address)
        assertNull(r41.hasDouche)

        val r42 = races.first { it.roundNumber == 42 }
        assertFalse(r42.isCompleted)
        assertEquals("planned", r42.statusLabel)
    }

    @Test
    fun results_parseRealTimeEncodingAndIgnoreKids() {
        val rows = client.parseResultsPayload(race41ResultsJson)
        assertEquals(4, rows.size) // les jeunes sont ignorés

        val damienRow = rows.first { it.runnerId == "1171" }
        assertEquals("10 km", damienRow.distanceLabel)
        assertEquals(10.4, damienRow.distanceKm, 0.001)
        assertEquals(3254, damienRow.timeSeconds) // 3254075 = 3254 s × 1000 + rang 75
        assertEquals("54:14", damienRow.timeFormatted)
        assertEquals(1126, damienRow.points)
        assertEquals(27, damienRow.categoryRank)
        assertEquals(2, damienRow.categoryTotal) // effectif réel de la catégorie dans les lignes lues

        val laetitiaRow = rows.first { it.runnerId == "1202" }
        assertEquals("5 km", laetitiaRow.distanceLabel)
        assertEquals(1866, laetitiaRow.timeSeconds)
    }

    @Test
    fun matching_findsEachTrioMemberOnTheRightDistance() {
        val rows = client.parseResultsPayload(race41ResultsJson)

        val d = client.findMatchingResultsForProfile("alg_2026_r41", 2026, damien, null, rows)
        assertEquals(1, d.size)
        assertEquals("10 km", d.first().distanceLabel)
        assertEquals("54:14", d.first().officialTimeFormatted)
        assertEquals("5:13 /km", d.first().pacePerKmFormatted) // 3254 s / 10,4 km

        val l = client.findMatchingResultsForProfile("alg_2026_r41", 2026, laetitia, null, rows)
        assertEquals(1, l.size)
        assertEquals("5 km", l.first().distanceLabel)
        assertEquals(1117, l.first().challengePoints)

        val e = client.findMatchingResultsForProfile("alg_2026_r41", 2026, elodie, null, rows)
        assertEquals(1134, e.single().challengePoints)
    }

    @Test
    fun matching_neverTakesTheSameBibFromTheOtherDistance() {
        // Laetitia absente du 5 km : son dossard 1075 existe au 10 km chez un autre coureur.
        val rowsWithoutLaetitia = client.parseResultsPayload(race41ResultsJson).filterNot { it.runnerId == "1202" }

        assertTrue(client.findMatchingResultsForProfile("alg_2026_r41", 2026, laetitia, null, rowsWithoutLaetitia).isEmpty())

        val override5km = RaceBibOverrideEntity("alg_2026_r41", "laetitia", "1075", 5)
        assertTrue(
            client.findMatchingResultsForProfile("alg_2026_r41", 2026, laetitia, override5km, rowsWithoutLaetitia).isEmpty()
        )
    }

    @Test
    fun matching_rejectsABibWornBySomeoneElse() {
        // Même distance, même dossard, mais identifiant MyChrono différent : pas attribué par défaut.
        val rows = client.parseResultsPayload(race41ResultsJson)
            .filterNot { it.runnerId == "1202" }
            .map { if (it.runnerId == "1189") it.copy(bibNumber = "1075") else it }

        assertTrue(client.findMatchingResultsForProfile("alg_2026_r41", 2026, laetitia, null, rows).isEmpty())

        // Dossard saisi explicitement pour cette manche (ex. dossard prêté) : attribué.
        val override = RaceBibOverrideEntity("alg_2026_r41", "laetitia", "1075", 5)
        assertEquals(1, client.findMatchingResultsForProfile("alg_2026_r41", 2026, laetitia, override, rows).size)
    }

    @Test
    fun results_unknownFormatGivesNothing() {
        val invented = """[{"id":"1171","full_name":"X","dossard":"697","time":"41:12","place":18}]"""
        assertTrue(client.parseResultsPayload(invented).isEmpty())
    }

    @Test
    fun reglementPoints_followAlgRule() {
        assertEquals(1200, MyChronoApiClient.reglementPointsForRank(1))
        assertEquals(1126, MyChronoApiClient.reglementPointsForRank(75))
    }
}
