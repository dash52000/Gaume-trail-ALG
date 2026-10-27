package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlgDatabase
import com.example.data.local.RaceWeatherEntity
import com.example.data.local.VillageInfoEntity
import com.example.data.repository.AlgRepository
import com.example.data.repository.UserDataBackup
import com.example.domain.SeasonInsightsCalculator
import com.example.ui.components.GaumePerfsCalculator
import com.example.ui.viewmodel.AlgUiState
import com.example.ui.viewmodel.AlgUiStateBuilder
import com.example.ui.viewmodel.ControlState
import com.example.ui.viewmodel.DataFlowsBundle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.lang.reflect.Modifier
import java.time.LocalDate

/**
 * Données de la version web (dossier web/data), produites par le robot GitHub (workflow web.yml).
 *
 * Ce n'est pas un test : il ne tourne que si la variable ALG_WEB_EXPORT_DIR est définie (sinon il est
 * ignoré, y compris dans le build de l'APK). Il fait exactement ce que fait l'appli Android : synchro MyChrono
 * complète, saisies restaurées depuis la sauvegarde (donnees/saisies.json si elle existe), puis le calcul des
 * écrans ([AlgUiStateBuilder]) pour chaque profil, saison et distance. La version web n'affiche que ces
 * résultats : les chiffres sont donc les mêmes que sur le téléphone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebDataExportTest {

    private val outDir: File? = System.getenv("ALG_WEB_EXPORT_DIR")?.takeIf { it.isNotBlank() }?.let { File(it) }
    /** Dossier des fichiers qui ne sont pas publiés : sauvegarde des saisies, cache météo et villages. */
    private val inputDir: File? = System.getenv("ALG_WEB_INPUT_DIR")?.takeIf { it.isNotBlank() }?.let { File(it) }

    @Test
    fun exportWebData() {
        assumeTrue("Export web : ALG_WEB_EXPORT_DIR non défini", outDir != null)
        val out = outDir!!
        val input = inputDir ?: File(out, "../../donnees")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AlgDatabase::class.java).allowMainThreadQueries().build()
        try {
            runBlocking { export(db, out, input) }
        } finally {
            db.close()
        }
    }

    private suspend fun export(db: AlgDatabase, out: File, input: File) {
        val dao = db.algDao()
        val repository = AlgRepository(algDao = dao, database = db)
        repository.ensureSeeded()

        val saisies = File(input, "saisies.json")
        val restoreMessage = if (saisies.isFile) {
            repository.restoreUserData(UserDataBackup.fromJson(saisies.readText(Charsets.UTF_8)))
        } else "aucune sauvegarde des saisies (profils de départ)"

        // Météo et fiches des villages déjà connues : gardées d'un passage du robot à l'autre.
        val cacheDir = File(input, "cache")
        readArray(File(cacheDir, "meteo.json"))?.forEachObject { o -> dao.upsertRaceWeather(weatherFrom(o)) }
        readArray(File(cacheDir, "villages.json"))?.forEachObject { o -> dao.upsertVillageInfo(villageFrom(o)) }

        val sync = repository.syncAll(force = false)
        val syncMessage = sync.getOrElse { throw IllegalStateException("Synchro MyChrono en échec : ${it.message}", it) }

        // Fiches Wikipédia des villages des manches (une seule fois par village).
        dao.getAllRacesSnapshot().map { it.village }.filter { it.isNotBlank() }.distinct().take(60).forEach { village ->
            runCatching { repository.loadVillageInfo(village) }
        }

        val bundle = DataFlowsBundle(
            profiles = dao.getRunnerProfilesSnapshot(),
            allRaces = dao.getAllRacesSnapshot(),
            allResults = dao.getAllResultsSnapshot(),
            allOverrides = dao.getAllBibOverridesSnapshot(),
            allMetrics = dao.getAllActivityMetricsSnapshot(),
            allTerrainWeather = dao.getAllTerrainWeatherSnapshot(),
            directory = emptyList(),
            standings = dao.getAllStandingsSnapshot(),
            seasonBibs = dao.getSeasonBibsSnapshot(),
            courseFactors = repository.courseFactorsFlow.first(),
            raceWeather = dao.getRaceWeatherSnapshot(),
            diagnostics = repository.diagnosticsFlow.value,
            seasonGoals = dao.getSeasonGoalsSnapshot(),
            villageInfos = repository.villageInfoFlow.first()
        )
        val baseSeasons = repository.getAvailableSeasons()

        // Saison ouverte par défaut, comme dans l'appli (la plus récente qui a déjà commencé).
        val defaultState = AlgUiStateBuilder.buildUiState(bundle, ControlState(), baseSeasons)
        val seasons = defaultState.availableSeasons
        val profileIds = bundle.profiles.map { it.profileId }

        val dataDir = out.apply { mkdirs() }
        listOf("etat", "bilans", "classements", "traces").forEach { File(dataDir, it).deleteRecursively() }

        var stateFiles = 0
        for (year in seasons) {
            for (profileId in profileIds) {
                for (dist in listOf(10, 5)) {
                    val state = AlgUiStateBuilder.buildUiState(
                        bundle,
                        ControlState(selectedSeasonYear = year, selectedProfileId = profileId, selectedChallengeDistanceKm = dist),
                        baseSeasons
                    )
                    if (dist == 10) {
                        // Bilan de saison du profil (indépendant de la distance affichée).
                        state.trioSeasonSummaries[profileId]?.let { write(File(dataDir, "bilans/$profileId-$year.json"), json(it)) }
                    }
                    write(File(dataDir, "etat/$profileId-$year-$dist.json"), screenState(state, dist))
                    stateFiles++
                }
            }
        }

        // Classement provisoire complet du Challenge, par saison et distance.
        bundle.standings.groupBy { it.seasonYear to it.distanceLabel }.forEach { (key, rows) ->
            val (year, label) = key
            val arr = JSONArray()
            rows.sortedBy { it.overallRank }.forEach { s ->
                arr.put(
                    JSONObject()
                        .put("rang", s.overallRank)
                        .put("nom", s.fullName)
                        .put("id", s.myChronoRunnerId)
                        .put("cat", s.category)
                        .put("rangCat", s.categoryRank)
                        .put("sexe", s.sex)
                        .put("dossard", s.bibNumber)
                        .put("points", s.totalPoints)
                        .put("comptees", s.racesCounted)
                        .put("courues", s.racesRun)
                        .put("plusFaible", s.lowestCountedPoints)
                )
            }
            write(File(dataDir, "classements/$year-${label.substringBefore(" ")}.json"), arr)
        }

        // Traces GPX, chargées seulement quand on ouvre la course.
        bundle.allMetrics.filter { it.serializedTrackPoints.isNotBlank() }.forEach { m ->
            write(
                File(dataDir, "traces/${m.raceId}__${m.profileId}.json"),
                JSONObject().put("titre", m.sourceTitle).put("points", m.serializedTrackPoints)
            )
        }

        write(
            File(dataDir, "meta.json"),
            JSONObject()
                .put("format", 1)
                .put("synchro", syncMessage)
                .put("saisies", restoreMessage)
                .put("saisonParDefaut", defaultState.selectedSeasonYear)
                .put("saisons", JSONArray(seasons))
                .put("profils", json(defaultState.profiles))
                .put("villages", json(defaultState.villageInfos))
                .put("diagnostic", json(bundle.diagnostics))
                .put("jour", LocalDate.now().toString())
        )

        // Cache pour le prochain passage (non publié).
        write(File(cacheDir, "meteo.json"), json(bundle.raceWeather))
        write(File(cacheDir, "villages.json"), json(bundle.villageInfos))

        println("::notice title=Export web::$syncMessage • $stateFiles écrans • ${bundle.standings.size} lignes de classement • $restoreMessage")
    }

    /** Ce qu'affichent les écrans pour un profil, une saison et une distance (sans les bilans, publiés à part). */
    private fun screenState(state: AlgUiState, dist: Int): JSONObject {
        val o = json(state) as JSONObject
        listOf(
            "activeRunnerSummary", "trioSeasonSummaries", "multiSeasonSummariesForActiveRunner", "profiles",
            "villageInfos", "diagnostics", "directorySearchResults", "isLoading", "isSyncingMyChrono", "selectedTab",
            "perfsSubTab", "preselectedShareRaceId", "raceFilterMode", "selectedRaceIdForDetail",
            "showTop15SimulatorModal", "directorySearchQuery", "statusBannerMessage",
            "bibOverridesByRaceAndProfile", "terrainWeatherByRaceAndProfile"
        ).forEach { o.remove(it) }
        val summary = state.activeRunnerSummary
        if (summary != null) {
            // Calculs faits par les écrans eux-mêmes dans l'appli Android.
            o.put("pronostics", json(GaumePerfsCalculator.computeSeasonPronostics(summary, dist)))
            o.put(
                "recordsPerso",
                json(GaumePerfsCalculator.computePersonalRecords(summary.summaryForDistance(dist), summary.racesWithContext))
            )
            val points = SeasonInsightsCalculator.formPoints(summary, dist)
            o.put("formePoints", json(points))
            o.put("formeAllure", json(SeasonInsightsCalculator.currentFormPace(points)))
        }
        return o
    }

    // ---------- Écriture JSON générique (champs des classes de données, par réflexion) ----------

    /** Champs recopiés ailleurs ou trop lourds : la trace GPX est publiée à part (fichier traces/). */
    private val skippedFields = setOf("myResult10km", "myResult5km", "myMetrics")

    private fun json(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is String, is Boolean, is Int, is Long, is Short, is Byte -> value
        is Double -> if (value.isNaN() || value.isInfinite()) JSONObject.NULL else value
        is Float -> if (value.isNaN() || value.isInfinite()) JSONObject.NULL else value.toDouble()
        is Char -> value.toString()
        is Enum<*> -> value.name
        is LocalDate -> value.toString()
        is Map<*, *> -> JSONObject().also { o -> value.forEach { (k, v) -> o.put(k.toString(), json(v)) } }
        is Iterable<*> -> JSONArray().also { a -> value.forEach { a.put(json(it)) } }
        is Array<*> -> JSONArray().also { a -> value.forEach { a.put(json(it)) } }
        is Pair<*, *> -> JSONObject().put("first", json(value.first)).put("second", json(value.second))
        else -> {
            val o = JSONObject()
            var c: Class<*>? = value.javaClass
            while (c != null && c != Any::class.java) {
                for (f in c.declaredFields) {
                    if (Modifier.isStatic(f.modifiers) || f.isSynthetic) continue
                    f.isAccessible = true
                    var name = f.name
                    var v = f.get(value)
                    if (name.endsWith("\$delegate") && v is Lazy<*>) {
                        name = name.removeSuffix("\$delegate")
                        v = v.value
                    }
                    if (name in skippedFields) continue
                    if (name == "serializedTrackPoints") {
                        o.put("hasTrack", (v as? String)?.isNotBlank() == true)
                        continue
                    }
                    o.put(name, json(v))
                }
                c = c.superclass
            }
            o
        }
    }

    private fun write(file: File, content: Any) {
        file.parentFile?.mkdirs()
        file.writeText(content.toString() + "\n", Charsets.UTF_8)
    }

    private fun readArray(file: File): JSONArray? =
        if (file.isFile) runCatching { JSONArray(file.readText(Charsets.UTF_8)) }.getOrNull() else null

    private suspend fun JSONArray.forEachObject(block: suspend (JSONObject) -> Unit) {
        for (i in 0 until length()) optJSONObject(i)?.let { block(it) }
    }

    private fun JSONObject.intOrNull(key: String): Int? = if (!has(key) || isNull(key)) null else optInt(key)
    private fun JSONObject.doubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }
    private fun JSONObject.stringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)

    private fun weatherFrom(o: JSONObject) = RaceWeatherEntity(
        raceId = o.getString("raceId"),
        startHourLocal = o.intOrNull("startHourLocal"),
        temperatureC = o.doubleOrNull("temperatureC"),
        precipitationDuringMm = o.doubleOrNull("precipitationDuringMm"),
        rainPrevious72hMm = o.doubleOrNull("rainPrevious72hMm"),
        windKmh = o.doubleOrNull("windKmh"),
        weatherCode = o.intOrNull("weatherCode"),
        dayMinC = o.doubleOrNull("dayMinC"),
        dayMaxC = o.doubleOrNull("dayMaxC"),
        source = o.optString("source"),
        fetchedAtEpochMillis = o.optLong("fetchedAtEpochMillis")
    )

    private fun villageFrom(o: JSONObject) = VillageInfoEntity(
        villageKey = o.getString("villageKey"),
        found = o.optBoolean("found"),
        title = o.stringOrNull("title"),
        extract = o.stringOrNull("extract"),
        pageUrl = o.stringOrNull("pageUrl"),
        fetchedAtEpochMillis = o.optLong("fetchedAtEpochMillis")
    )
}
