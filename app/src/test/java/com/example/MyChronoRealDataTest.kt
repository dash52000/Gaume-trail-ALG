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
          {"id":107,"competitionId":25,"competition":"Hors challenge ALG 2026","org":"ALG","orgId":1,"kind":"single",
           "year":2026,"num":1,"date":"2026-01-04","place":"Suxy","name":"Lu Raid Adjalé",
           "info":{"address":"Salle du Val d'Eau","schedule":"10h00 – 14 km · 10h15 – 7 km","terrain":"Prairie, forêt.","maps":[]},
           "status":"planned","published":false,"entries":0,"finishers":0,
           "distances":[
             {"id":75,"label":"14 km","kind":"adult","length":14000,"real":null,"effective":14000,"noTime":false},
             {"id":86,"label":"7 km","kind":"adult","length":7000,"real":null,"effective":7000,"noTime":false},
             {"id":87,"label":"Marche","kind":"adult","length":8000,"real":null,"effective":8000,"noTime":true}]},
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

    /** Extrait d'un classement hors challenge (format identique, distances propres à la course). */
    private val horsChallengeResultsJson = """
        {"race":{"id":107,"num":1,"place":"Suxy","status":"closed",
           "distances":[
             {"id":75,"label":"14 km","kind":"adult","effective":14000},
             {"id":86,"label":"7 km","kind":"adult","effective":7000}]},
         "results":[
           {"distanceId":75,"rows":[
             {"rank":40,"bib":12,"name":"COLLET Damien","sex":"M","cat":"S","catRank":15,"club":"","city":"",
              "time":4380040,"status":"finished","points":1161,"runnerId":1171}]},
           {"distanceId":86,"rows":[
             {"rank":20,"bib":13,"name":"LAETITIA","sex":"F","cat":"F","catRank":5,"club":"","city":"",
              "time":2700020,"status":"finished","points":1181,"runnerId":1202}]}
         ]}
    """.trimIndent()

    @Test
    fun horsChallenge_neverOverwritesAChallengeRound() {
        val races = client.parseRacesPayload(racesJson, 2026)
        assertEquals(4, races.size)
        assertEquals(4, races.map { it.raceId }.toSet().size) // aucun identifiant en double

        val manche1 = races.first { it.isChallenge && it.roundNumber == 1 }
        assertEquals("alg_2026_r01", manche1.raceId)
        assertEquals("Virton", manche1.village)

        val hc = races.single { !it.isChallenge }
        assertEquals("alg_2026_h01", hc.raceId)
        assertEquals("Suxy", hc.village)
        assertEquals("Hors challenge ALG 2026", hc.competitionLabel)
        assertEquals("14 km", hc.longDistanceLabel)
        assertEquals("7 km", hc.shortDistanceLabel)
        assertEquals(14.0, hc.distanceKm, 0.001)
        assertEquals(7.0, hc.distance5Km!!, 0.001)
        assertFalse(hc.isCompleted)
    }

    @Test
    fun horsChallenge_resultsAreFoundButNeverCount() {
        val rows = client.parseResultsPayload(horsChallengeResultsJson, isChallenge = false)
        assertEquals(2, rows.size)
        val d = client.findMatchingResultsForProfile("alg_2026_h01", 2026, damien, null, rows, isChallenge = false)
        assertEquals(1, d.size)
        assertEquals("14 km", d.single().distanceName)
        assertEquals(14.0, d.single().distanceKm!!, 0.001)
        assertEquals(0, d.single().challengePoints) // ne compte pas pour le challenge
        assertEquals("1:13:00", d.single().officialTimeFormatted)

        val l = client.findMatchingResultsForProfile("alg_2026_h01", 2026, laetitia, null, rows, isChallenge = false)
        assertEquals("7 km", l.single().distanceName)

        // Le dossard de la saison n'est jamais utilisé hors challenge (il n'y est pas valable).
        val sansId = damien.copy(myChronoRunnerId = "")
        assertTrue(client.findMatchingResultsForProfile("alg_2026_h01", 2026, sansId, null, rows, isChallenge = false).isEmpty())
    }

    @Test
    fun matching_usesTheMyChronoIdOnBothDistances_evenWithoutBib() {
        val rows = client.parseResultsPayload(race41ResultsJson)
        val noBibs = laetitia.copy(defaultBib10km = "", defaultBib5km = "")
        val l = client.findMatchingResultsForProfile("alg_2026_r41", 2026, noBibs, null, rows)
        assertEquals("5 km", l.single().distanceLabel)

        // Damien n'a saisi que son dossard 10 km : un 5 km couru sous son identifiant est quand même trouvé.
        val rows5 = rows.map { if (it.runnerId == "1171") it.copy(distanceLabel = "5 km", bibNumber = "1150") else it }
        val d = client.findMatchingResultsForProfile("alg_2026_r05", 2026, damien, null, rows5)
        assertEquals("5 km", d.single().distanceLabel)
        assertEquals("1150", d.single().bibNumber)

        // Dossard saisi pour la course sur la mauvaise distance : il ne bloque plus le résultat.
        val wrong = RaceBibOverrideEntity("alg_2026_r05", "me", "1150", 10)
        assertEquals("5 km", client.findMatchingResultsForProfile("alg_2026_r05", 2026, damien, wrong, rows5).single().distanceLabel)
    }

    @Test
    fun races_useOnlyPublishedData() {
        val races = client.parseRacesPayload(racesJson, 2026).filter { it.isChallenge }
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

    /** Extrait réel de https://mychrono.softalux.com/api/public/runners/1171 (saison 2026). */
    private val runnerPageJson = """
        {"runner":{"id":1171,"name":"COLLET Damien","club":"","city":"","sex":"M"},
         "competitions":[
           {"competition":{"id":3,"orgId":1,"name":"Challenge ALG 2026","year":2026,"kind":"challenge"},
            "results":[
              {"raceId":5,"num":5,"date":"2026-02-22","place":"Sommethonne","raceName":"A.L.de la Thonne","distanceId":5,
               "distance":"5 km","len":5900,"lenReal":true,"status":"finished","time":1821049,"rank":49,"catRank":16,
               "cat":"S","points":1152,"pace":309,"speed":11.66,"finishers":206},
              {"raceId":15,"num":15,"date":"2026-05-01","place":"Beckerich","distanceId":5,"distance":"5 km","len":5600,
               "status":"finished","time":1588033,"rank":33,"catRank":18,"cat":"S","points":1168,"finishers":239},
              {"raceId":27,"num":27,"date":"2026-07-12","place":"Houdemont","distanceId":5,"distance":"5 km","len":5900,
               "status":"finished","time":2403087,"rank":87,"catRank":23,"cat":"S","points":1114,"finishers":146},
              {"raceId":28,"num":28,"date":"2026-07-19","place":"Ailleurs","distanceId":4,"distance":"10 km","len":10000,
               "status":"dns","time":0,"rank":0,"points":0}],
            "byDist":[]}]}
    """.trimIndent()

    @Test
    fun runnerPage_listsEveryRaceWithItsDistance() {
        val page = MyChronoApiClient.parseRunnerPayload(runnerPageJson)!!
        assertEquals("1171", page.runnerId)
        assertEquals("COLLET Damien", page.name)
        assertEquals(3, page.results.size) // le non-partant est ignoré

        val sommethonne = page.results.first { it.raceEventId == "5" }
        assertEquals(1821, sommethonne.timeSeconds) // 30:21
        assertEquals(49, sommethonne.rank)
        assertEquals(16, sommethonne.categoryRank)
        assertEquals(1152, sommethonne.points)
        assertEquals(206, sommethonne.finishers)

        val races = client.parseRacesPayload(racesJson, 2026)
        val manche = races.first { it.roundNumber == 1 }
        assertEquals("5 km", MyChronoApiClient.labelForRunnerResult(manche, sommethonne))
        // Beckerich : la grande distance s'appelle « 10 km » mais fait 21,2 km — même règle que le classement.
        val semi = sommethonne.copy(distanceName = "10 km", lengthMeters = 21200)
        assertEquals("10 km", MyChronoApiClient.labelForRunnerResult(manche, semi))
        assertNull(MyChronoApiClient.labelForRunnerResult(manche, sommethonne.copy(distanceName = "Jeunes", lengthMeters = 1000)))

        // Hors challenge : distances propres à la course (14 km → place du 10 km, 7 km → place du 5 km).
        val hc = races.single { !it.isChallenge }
        assertEquals("10 km", MyChronoApiClient.labelForRunnerResult(hc, sommethonne.copy(distanceName = "14 km", lengthMeters = 14000)))
        assertEquals("5 km", MyChronoApiClient.labelForRunnerResult(hc, sommethonne.copy(distanceName = "7 km", lengthMeters = 7000)))
    }

    @Test
    fun matching_acceptsTheSameNameUnderAnOldId() {
        // Classement gardé sur le téléphone avant que MyChrono fusionne deux fiches de Damien.
        val rows = client.parseResultsPayload(race41ResultsJson)
            .map { if (it.runnerId == "1171") it.copy(runnerId = "8888", distanceLabel = "5 km", bibNumber = "1150") else it }
        val withBib = damien.copy(defaultBib5km = "1150")
        assertEquals("5 km", client.findMatchingResultsForProfile("alg_2026_r05", 2026, withBib, null, rows).single().distanceLabel)

        assertTrue(MyChronoApiClient.namesMatch("Damien Collet", "COLLET Damien"))
        assertTrue(MyChronoApiClient.namesMatch("Élodie", "ELODIE"))
        assertFalse(MyChronoApiClient.namesMatch("Damien Collet", "COLLET Sébastien"))
        assertFalse(MyChronoApiClient.namesMatch("", "COLLET Damien"))
    }

    @Test
    fun reglementPoints_followAlgRule() {
        assertEquals(1200, MyChronoApiClient.reglementPointsForRank(1))
        assertEquals(1126, MyChronoApiClient.reglementPointsForRank(75))
    }
}
