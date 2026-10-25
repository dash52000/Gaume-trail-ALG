package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.ActivityMetricsEntity
import androidx.room.withTransaction
import com.example.data.local.AlgDao
import com.example.data.local.AlgDatabase
import com.example.data.local.ChallengeStandingEntity
import com.example.data.local.CourseFactorEntity
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.MyChronoDirectoryEntry
import com.example.data.local.MyChronoRowEntity
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceEntity
import com.example.data.local.RaceFetchStateEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.local.RaceWeatherEntity
import com.example.data.local.RunnerProfileEntity
import com.example.data.local.SeasonBibEntity
import com.example.data.remote.MyChronoApiClient
import com.example.data.remote.MyChronoRawResultRow
import com.example.data.remote.MyChronoRunnerResult
import com.example.data.remote.OpenMeteoClient
import com.example.data.remote.WikipediaClient
import com.example.data.local.SeasonGoalEntity
import com.example.data.local.VillageInfoEntity
import com.example.domain.ChallengeStandingsCalculator
import com.example.domain.CourseDifficultyCalculator
import com.example.domain.GpxAndTrackUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt

/** Ce que MyChrono publie sous l'identifiant d'un profil, pour vérifier qu'il est le bon. */
data class ProfileIdentityReport(
    val profileId: String,
    val runnerId: String,
    val myChronoName: String?,
    /** Saison → distance ("10 km" / "5 km") → nombre de manches publiées sous cet identifiant. */
    val publishedBySeason: Map<Int, Map<String, Int>>,
    /** Saison → distance → nombre de courses sur la fiche coureur MyChrono (null : fiche pas encore lue). */
    val onRunnerPageBySeason: Map<Int, Map<String, Int>>? = null
)

data class SyncDiagnostics(
    val profiles: List<ProfileIdentityReport> = emptyList(),
    /** Saison → nombre de courses dont le classement est gardé sur le téléphone. */
    val cachedRacesBySeason: Map<Int, Int> = emptyMap(),
    /** Saison → nombre de courses dont MyChrono a publié le classement. */
    val publishedRacesBySeason: Map<Int, Int> = emptyMap(),
    /** Corrections d'identifiant faites à la dernière synchro. */
    val repairs: List<String> = emptyList()
)

class AlgRepository(
    private val algDao: AlgDao,
    private val myChronoApiClient: MyChronoApiClient = MyChronoApiClient(),
    private val openMeteoClient: OpenMeteoClient = OpenMeteoClient(),
    private val today: () -> LocalDate = { LocalDate.now() },
    /** Pour regrouper les écritures (l'écran ne voit jamais une saison à moitié recalculée). */
    private val database: AlgDatabase? = null,
    private val wikipediaClient: WikipediaClient = WikipediaClient()
) {
    companion object {
        /** Première saison publiée par MyChrono pour le Challenge des Allures Libres de Gaume. */
        const val FIRST_SEASON = 2025

        /** Une manche close est retéléchargée pendant ces jours-là (corrections de classement). */
        private const val REFRESH_WINDOW_DAYS = 21L

        /** Nombre maximal de courses dont la météo est cherchée à chaque synchro. */
        private const val MAX_WEATHER_PER_SYNC = 40

        /**
         * Ligne ajoutée au classement d'une course à partir de la fiche coureur (rowIndex = base + identifiant) :
         * hors de la plage des lignes téléchargées, et une seule par coureur et par distance.
         */
        const val RUNNER_PAGE_ROW_INDEX = 1_000_000
    }

    private val recomputeMutex = Mutex()

    /** Identifiant MyChrono → saison → distance → nombre de courses sur sa fiche (dernière lecture). */
    @Volatile
    private var runnerPageCounts: Map<String, Map<Int, Map<String, Int>>> = emptyMap()

    private val diagnosticsState = MutableStateFlow(SyncDiagnostics())
    val diagnosticsFlow: StateFlow<SyncDiagnostics> = diagnosticsState.asStateFlow()

    private suspend fun <T> inTransaction(block: suspend () -> T): T {
        val db = database ?: return block()
        return db.withTransaction { block() }
    }

    val runnerProfilesFlow: Flow<List<RunnerProfileEntity>> = algDao.observeRunnerProfiles()
    val allRacesFlow: Flow<List<RaceEntity>> = algDao.observeAllRaces()
    val allResultsFlow: Flow<List<RaceResultEntity>> = algDao.observeAllResults()
    val allBibOverridesFlow: Flow<List<RaceBibOverrideEntity>> = algDao.observeAllBibOverrides()
    val allActivityMetricsFlow: Flow<List<ActivityMetricsEntity>> = algDao.observeAllActivityMetrics()
    val allTerrainWeatherFlow: Flow<List<RaceTerrainWeatherEntity>> = algDao.observeAllTerrainWeather()
    val directoryFlow: Flow<List<MyChronoDirectoryEntry>> = algDao.observeDirectory()
    val standingsFlow: Flow<List<ChallengeStandingEntity>> = algDao.observeAllStandings()
    val seasonBibsFlow: Flow<List<SeasonBibEntity>> = algDao.observeSeasonBibs()
    val courseFactorsFlow: Flow<List<CourseFactorEntity>> = algDao.observeCourseFactors()
    val raceWeatherFlow: Flow<List<RaceWeatherEntity>> = algDao.observeRaceWeather()
    val seasonGoalsFlow: Flow<List<SeasonGoalEntity>> = algDao.observeSeasonGoals()
    val villageInfoFlow: Flow<List<VillageInfoEntity>> = algDao.observeVillageInfo()

    /** Fixe (ou retire, si target ≤ 0) un objectif de saison. */
    suspend fun setSeasonGoal(profileId: String, seasonYear: Int, goalType: String, target: Int) = withContext(Dispatchers.IO) {
        if (target <= 0) algDao.deleteSeasonGoal(profileId, seasonYear, goalType)
        else algDao.upsertSeasonGoal(SeasonGoalEntity(profileId, seasonYear, goalType, target))
    }

    /**
     * Fiche Wikipédia d'un village, chargée à la demande puis gardée sur le téléphone.
     * Un village sans page fiable n'est réessayé qu'au bout de 30 jours.
     */
    suspend fun loadVillageInfo(village: String) = withContext(Dispatchers.IO) {
        val key = GaumeInitialSeedData.normalizeKey(village)
        val existing = algDao.getVillageInfo(key)
        val monthAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        if (existing != null && (existing.found || existing.fetchedAtEpochMillis > monthAgo)) return@withContext
        wikipediaClient.fetchVillageSummary(village).onSuccess { summary ->
            algDao.upsertVillageInfo(
                VillageInfoEntity(
                    villageKey = key,
                    found = summary != null,
                    title = summary?.title,
                    extract = summary?.extract,
                    pageUrl = summary?.pageUrl,
                    fetchedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    /** Saisons proposées : de 2025 (saison de référence) à l'année en cours. */
    fun getAvailableSeasons(): List<Int> = (FIRST_SEASON..maxOf(FIRST_SEASON, today().year)).toList()

    /**
     * Crée les 3 profils du Trio au premier lancement (avec leurs dossards 2026).
     * Aucun calendrier n'est inventé : les courses viennent uniquement de MyChrono.
     */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        algDao.deletePre2025Races()
        algDao.deletePre2025Results()

        if (algDao.getRunnerProfilesSnapshot().isEmpty()) {
            val profiles = GaumeInitialSeedData.buildInitialProfiles()
            algDao.upsertRunnerProfiles(profiles)
            if (algDao.getSeasonBibsSnapshot().isEmpty()) {
                profiles.forEach { p ->
                    algDao.upsertSeasonBib(SeasonBibEntity(p.profileId, 2026, p.defaultBib10km, p.defaultBib5km))
                }
            }
        }
    }

    private fun MyChronoRowEntity.toRaw() = MyChronoRawResultRow(
        runnerId = runnerId,
        bibNumber = bibNumber,
        fullName = fullName,
        club = club,
        category = category,
        sex = sex,
        distanceLabel = distanceLabel,
        distanceKm = distanceKm,
        timeSeconds = timeSeconds,
        timeFormatted = formatTime(timeSeconds),
        overallRank = overallRank,
        totalFinishers = totalFinishers,
        categoryRank = categoryRank,
        categoryTotal = categoryTotal,
        points = points,
        distanceName = distanceName
    )

    private fun formatTime(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val sec = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }

    private fun hasPublishedResults(race: RaceEntity, now: LocalDate): Boolean {
        if (race.dateIso > now.toString()) return false
        return race.statusLabel != "planned" || (race.finishersCount ?: 0) > 0
    }

    private fun needsFetch(race: RaceEntity, state: RaceFetchStateEntity?, now: LocalDate, force: Boolean): Boolean {
        if (!hasPublishedResults(race, now)) return false
        if (force || state == null || state.raceDateIso != race.dateIso) return true
        // Classement publié vide : on réessaie pendant deux mois, pas indéfiniment.
        if (state.rowCount == 0) return race.dateIso >= now.minusDays(60).toString()
        return race.dateIso >= now.minusDays(REFRESH_WINDOW_DAYS).toString()
    }

    private suspend fun storeRows(race: RaceEntity, rows: List<MyChronoRawResultRow>) = inTransaction {
        algDao.deleteRowsForRace(race.raceId)
        algDao.insertRows(rows.mapIndexed { idx, r ->
            MyChronoRowEntity(
                raceId = race.raceId,
                distanceLabel = r.distanceLabel,
                rowIndex = idx,
                seasonYear = race.seasonYear,
                isChallenge = race.isChallenge,
                distanceName = r.distanceName.ifBlank { r.distanceLabel },
                distanceKm = r.distanceKm,
                runnerId = r.runnerId,
                bibNumber = r.bibNumber,
                fullName = r.fullName,
                club = r.club,
                category = r.category,
                sex = r.sex,
                timeSeconds = r.timeSeconds,
                overallRank = r.overallRank,
                totalFinishers = r.totalFinishers,
                categoryRank = r.categoryRank,
                categoryTotal = r.categoryTotal,
                points = r.points
            )
        })
        algDao.upsertFetchState(
            RaceFetchStateEntity(
                raceId = race.raceId,
                seasonYear = race.seasonYear,
                raceDateIso = race.dateIso,
                fetchedAtEpochMillis = System.currentTimeMillis(),
                rowCount = rows.size
            )
        )
    }

    /**
     * Synchronise toutes les saisons depuis l'API publique MyChrono :
     * calendriers (1 appel par saison), puis résultats des seules courses nouvelles ou récentes
     * (une course close depuis plus de 3 semaines est gardée sur le téléphone), puis recalcul local
     * des résultats du Trio, du classement provisoire et des indices de parcours, et enfin la météo.
     */
    suspend fun syncAll(force: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        val now = today()
        val seasons = (FIRST_SEASON..now.year + 1).toList()

        val calendars = seasons.associateWith { myChronoApiClient.fetchSeasonCalendar(it) }
        if (calendars.values.all { it.isFailure }) {
            return@withContext Result.failure(
                calendars.values.first().exceptionOrNull() ?: IllegalStateException("MyChrono injoignable")
            )
        }
        calendars.forEach { (year, res) ->
            val races = res.getOrNull().orEmpty()
            if (races.isNotEmpty()) {
                inTransaction {
                    algDao.deleteRacesBySeason(year)
                    algDao.upsertRaces(races)
                }
            }
        }

        val allRaces = algDao.getAllRacesSnapshot()
        val states = algDao.getFetchStates().associateBy { it.raceId }
        val toFetch = allRaces.filter { needsFetch(it, states[it.raceId], now, force) }
        val fetched = myChronoApiClient.fetchManyRaceResults(toFetch)
        val raceById = allRaces.associateBy { it.raceId }
        fetched.forEach { (raceId, rows) -> raceById[raceId]?.let { storeRows(it, rows) } }

        val repairs = repairRunnerIds() + runCatching { crossCheckWithRunnerPages() }.getOrDefault(emptyList())
        recomputeAllSeasons()
        refreshDiagnostics(repairs)
        val weatherCount = runCatching { fetchMissingWeather(now) }.getOrDefault(0)

        val stamp = System.currentTimeMillis()
        algDao.getRunnerProfilesSnapshot().forEach { algDao.upsertRunnerProfile(it.copy(lastSyncedEpochMillis = stamp)) }

        val current = allRaces.filter { it.seasonYear == now.year }
        val newResults = fetched.count { (raceId, rows) -> rows.isNotEmpty() && states[raceId]?.rowCount != rows.size }
        Result.success(
            buildString {
                append("MyChrono ${now.year} : ${current.count { it.isChallenge }} manches")
                val hc = current.count { !it.isChallenge }
                if (hc > 0) append(" + $hc hors challenge")
                append(" • ")
                append(
                    when (newResults) {
                        0 -> "aucun nouveau classement"
                        1 -> "1 classement mis à jour"
                        else -> "$newResults classements mis à jour"
                    }
                )
                if (weatherCount > 0) append(" • météo de $weatherCount course(s)")
                repairs.forEach { append(" • $it") }
                val failed = toFetch.size - fetched.size
                if (failed > 0) append(" • $failed course(s) injoignable(s)")
            }
        )
    }

    /** Ancien point d'entrée : synchronise tout. */
    suspend fun syncSeasonFromMyChrono(@Suppress("UNUSED_PARAMETER") seasonYear: Int): Result<String> = syncAll()

    /** Retélécharge tous les classements publiés puis recalcule tout. */
    suspend fun resetAndResyncCleanResults(): Result<String> = syncAll(force = true)

    private suspend fun recomputeAllSeasons() {
        algDao.getSeasonsWithRows().sorted().forEach { recomputeSeason(it) }
    }

    /**
     * Recalcule une saison à partir des résultats bruts gardés sur le téléphone (aucun appel réseau) :
     * résultats du Trio, classement provisoire (manches seulement), indices de parcours et annuaire.
     */
    suspend fun recomputeSeason(seasonYear: Int) = withContext(Dispatchers.IO) {
        recomputeMutex.withLock {
            val races = algDao.getRacesBySeasonSnapshot(seasonYear)
            val rowsByRace = algDao.getRowsForSeason(seasonYear)
                .groupBy { it.raceId }
                .mapValues { (_, rows) -> rows.sortedBy { it.rowIndex }.map { it.toRaw() } }
                .filterKeys { id -> races.any { it.raceId == id } }
            if (rowsByRace.isEmpty()) return@withLock

            val challengeIds = races.filter { it.isChallenge }.map { it.raceId }.toSet()
            val profiles = algDao.getRunnerProfilesSnapshot()
            val seasonBibs = algDao.getSeasonBibsSnapshot().filter { it.seasonYear == seasonYear }
                .associateBy { it.profileId }
            val overrides = algDao.getAllBibOverridesSnapshot().groupBy { it.profileId }
                .mapValues { (_, list) -> list.associateBy { it.raceId } }

            val results = mutableListOf<RaceResultEntity>()
            for (profile in profiles) {
                val sb = seasonBibs[profile.profileId]
                val declared = sb != null && (sb.bib10km.isNotBlank() || sb.bib5km.isNotBlank())
                val seasonProfile = profile.copy(
                    defaultBib10km = sb?.bib10km.orEmpty(),
                    defaultBib5km = sb?.bib5km.orEmpty()
                )
                for (race in races) {
                    val rows = rowsByRace[race.raceId] ?: continue
                    results += myChronoApiClient.findMatchingResultsForProfile(
                        raceId = race.raceId,
                        seasonYear = seasonYear,
                        profile = seasonProfile,
                        raceOverride = overrides[profile.profileId]?.get(race.raceId),
                        rawRows = rows,
                        isChallenge = race.isChallenge,
                        seasonBibsDeclared = declared
                    )
                }
            }
            val stamp = System.currentTimeMillis()
            val standings = ChallengeStandingsCalculator.computeSeasonStandings(
                seasonYear = seasonYear,
                rawRowsByRaceId = rowsByRace.filterKeys { it in challengeIds },
                computedAtEpochMillis = stamp
            )
            val factors = CourseDifficultyCalculator.computeSeasonFactors(rowsByRace, challengeIds).map {
                CourseFactorEntity(it.raceId, it.distanceLabel, seasonYear, it.factor, it.regularsCount)
            }
            val directory = myChronoApiClient.buildDirectoryFromRows(rowsByRace.values.flatten())

            inTransaction {
                algDao.deleteResultsBySeason(seasonYear)
                if (results.isNotEmpty()) {
                    algDao.upsertRaceResults(results.distinctBy { Triple(it.raceId, it.profileId, it.distanceLabel) })
                }
                algDao.deleteStandingsBySeason(seasonYear)
                algDao.upsertStandings(standings)
                algDao.deleteCourseFactorsBySeason(seasonYear)
                algDao.upsertCourseFactors(factors)
                algDao.upsertDirectoryEntries(directory)
            }
        }
    }

    private fun nameTokens(name: String): Set<String> =
        GaumeInitialSeedData.normalizeKey(name).split(" ").filter { it.length >= 2 }.toSet()

    /**
     * Vérifie l'identifiant MyChrono de chaque profil à partir des résultats gardés sur le téléphone.
     * Il est corrigé s'il est vide, inconnu de MyChrono, ou porté par un coureur d'un autre nom
     * (profil avec prénom et nom). Le bon identifiant est celui qui porte les dossards de la saison
     * et/ou dont le nom correspond au profil, sur au moins deux courses.
     */
    suspend fun repairRunnerIds(): List<String> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<String>()
        val seasonBibs = algDao.getSeasonBibsSnapshot()
        for (p in algDao.getRunnerProfilesSnapshot()) {
            val tokens = nameTokens(p.displayName)
            val id = p.myChronoRunnerId.trim()
            val idRows = if (id.isBlank()) emptyList() else algDao.countRowsForRunners(listOf(id))
            val idName = idRows.maxByOrNull { it.n }?.fullName
            val idLooksRight = idRows.isNotEmpty() &&
                (tokens.size < 2 || idName == null || nameTokens(idName).intersect(tokens).isNotEmpty())
            if (idLooksRight) continue

            val byBib = seasonBibs.filter { it.profileId == p.profileId }.flatMap { sb ->
                listOf("10 km" to sb.bib10km, "5 km" to sb.bib5km)
                    .map { (label, bib) -> label to bib.trim().removePrefix("#") }
                    .filter { it.second.isNotBlank() }
                    .flatMap { (label, bib) -> algDao.runnersWearingBib(sb.seasonYear, label, bib) }
            }.filter { tokens.size < 2 || nameTokens(it.fullName).intersect(tokens).isNotEmpty() }

            val byName = if (tokens.size >= 2) {
                algDao.runnersNamedLike("%" + tokens.maxByOrNull { it.length }!! + "%")
                    .filter { nameTokens(it.fullName).containsAll(tokens) }
            } else emptyList()

            val best = (byBib + byName)
                .groupBy { it.runnerId }
                .mapValues { (_, list) -> list.distinctBy { it.seasonYear to it.distanceLabel }.sumOf { it.n } }
                .maxByOrNull { it.value }
                ?.takeIf { it.value >= 2 }
                ?.key ?: continue
            if (best == id) continue

            algDao.upsertRunnerProfile(p.copy(myChronoRunnerId = best))
            messages += "identifiant MyChrono de ${p.displayName.substringBefore(" ")} corrigé (${id.ifBlank { "vide" }} → $best)"
        }
        messages
    }

    /**
     * Contre-vérification avec la fiche coureur MyChrono de chaque profil (1 appel par profil) : chaque course
     * de la fiche doit se trouver dans le classement gardé sur le téléphone, sous son identifiant et sur la
     * bonne distance. Sinon (classement gardé avant une correction ou une fusion de fiches chez MyChrono,
     * téléchargement raté…), la course est retéléchargée ; s'il y manque encore, son résultat est repris de
     * la fiche et ajouté au classement de la course.
     */
    private suspend fun crossCheckWithRunnerPages(): List<String> {
        val profiles = algDao.getRunnerProfilesSnapshot().filter { it.myChronoRunnerId.isNotBlank() }
        if (profiles.isEmpty()) return emptyList()
        val profileByRunner = profiles.associateBy { it.myChronoRunnerId.trim() }
        val pages = profileByRunner.keys.mapNotNull { id ->
            myChronoApiClient.fetchRunnerPage(id).getOrNull()?.let { id to it }
        }.toMap()
        if (pages.isEmpty()) return emptyList()

        val racesByEventId = algDao.getAllRacesSnapshot()
            .filter { it.seasonYear >= FIRST_SEASON }
            .groupBy { it.myChronoEventId }

        data class Expected(val runnerId: String, val race: RaceEntity, val label: String, val result: MyChronoRunnerResult)

        val expected = pages.flatMap { (id, page) ->
            page.results.mapNotNull { r ->
                val race = racesByEventId[r.raceEventId]
                    ?.firstOrNull { r.dateIso.isBlank() || it.dateIso == r.dateIso }
                    ?: return@mapNotNull null
                val label = MyChronoApiClient.labelForRunnerResult(race, r) ?: return@mapNotNull null
                Expected(id, race, label, r)
            }
        }.distinctBy { Triple(it.runnerId, it.race.raceId, it.label) }

        val pageCounts = expected.groupBy { it.runnerId }.mapValues { (_, list) ->
            list.groupBy { it.race.seasonYear }.mapValues { (_, l) -> l.groupingBy { it.label }.eachCount() }
        }
        runnerPageCounts = runnerPageCounts + pages.keys.associateWith { pageCounts[it].orEmpty() }

        val rowsCache = HashMap<String, List<MyChronoRowEntity>>()
        suspend fun rowsOf(raceId: String) = rowsCache.getOrPut(raceId) { algDao.getRowsForRace(raceId) }
        suspend fun isCached(e: Expected) =
            rowsOf(e.race.raceId).any { it.distanceLabel == e.label && it.runnerId == e.runnerId }

        val missing = expected.filter { !isCached(it) }
        if (missing.isEmpty()) return emptyList()

        val toRefetch = missing.map { it.race }.distinctBy { it.raceId }
        myChronoApiClient.fetchManyRaceResults(toRefetch).forEach { (raceId, rows) ->
            toRefetch.firstOrNull { it.raceId == raceId }?.let { storeRows(it, rows) }
            rowsCache.remove(raceId)
        }

        val stillMissing = missing.filter { !isCached(it) }
        if (stillMissing.isNotEmpty()) {
            val bibs = algDao.getSeasonBibsSnapshot()
            val patches = stillMissing.map { e ->
                val page = pages.getValue(e.runnerId)
                val others = rowsOf(e.race.raceId).filter { it.distanceLabel == e.label }
                val is5 = e.label == MyChronoApiClient.LABEL_5KM
                val sb = bibs.firstOrNull {
                    it.profileId == profileByRunner[e.runnerId]?.profileId && it.seasonYear == e.race.seasonYear
                }
                MyChronoRowEntity(
                    raceId = e.race.raceId,
                    distanceLabel = e.label,
                    rowIndex = RUNNER_PAGE_ROW_INDEX + (e.runnerId.toIntOrNull() ?: (e.runnerId.hashCode() and 0xFFFFF)),
                    seasonYear = e.race.seasonYear,
                    isChallenge = e.race.isChallenge,
                    distanceName = e.result.distanceName.ifBlank { e.label },
                    distanceKm = e.result.lengthMeters?.let { (it / 100.0).roundToInt() / 10.0 }
                        ?: e.race.realDistanceKmFor(if (is5) 5 else 10)
                        ?: if (is5) 5.0 else 10.0,
                    runnerId = e.runnerId,
                    bibNumber = (if (is5) sb?.bib5km else sb?.bib10km).orEmpty(),
                    fullName = page.name,
                    club = page.club,
                    category = e.result.category,
                    sex = page.sex,
                    timeSeconds = e.result.timeSeconds,
                    overallRank = e.result.rank,
                    totalFinishers = e.result.finishers.takeIf { it > 0 } ?: (others.size + 1),
                    categoryRank = e.result.categoryRank,
                    // Effectif de la catégorie inconnu si le classement de la course n'est pas sur le téléphone.
                    categoryTotal = if (others.isEmpty()) 0 else others.count { it.category == e.result.category } + 1,
                    points = e.result.points
                )
            }
            inTransaction { algDao.insertRows(patches) }
        }

        return missing.groupBy { it.runnerId }.map { (id, list) ->
            val who = profileByRunner[id]?.displayName?.substringBefore(" ") ?: id
            val detail = list.groupingBy { it.label }.eachCount().entries
                .sortedBy { if (it.key == MyChronoApiClient.LABEL_10KM) 0 else 1 }
                .joinToString(" + ") { "${it.value} × ${it.key}" }
            val fromPage = stillMissing.count { it.runnerId == id }
            "$who : $detail manquant(s) dans les classements, retrouvé(s) via la fiche MyChrono" +
                if (fromPage > 0) " ($fromPage repris directement de la fiche)" else ""
        }
    }

    /** Met à jour le diagnostic affiché dans les réglages. */
    suspend fun refreshDiagnostics(repairs: List<String> = diagnosticsState.value.repairs) = withContext(Dispatchers.IO) {
        val profiles = algDao.getRunnerProfilesSnapshot()
        val ids = profiles.map { it.myChronoRunnerId.trim() }.filter { it.isNotBlank() }.distinct()
        val counts = if (ids.isEmpty()) emptyList() else algDao.countRowsForRunners(ids)
        val now = today()
        diagnosticsState.value = SyncDiagnostics(
            profiles = profiles.map { p ->
                val mine = counts.filter { it.runnerId == p.myChronoRunnerId.trim() }
                ProfileIdentityReport(
                    profileId = p.profileId,
                    runnerId = p.myChronoRunnerId.trim(),
                    myChronoName = mine.maxByOrNull { it.n }?.fullName,
                    publishedBySeason = mine.groupBy { it.seasonYear }
                        .mapValues { (_, l) -> l.associate { it.distanceLabel to it.n } },
                    onRunnerPageBySeason = runnerPageCounts[p.myChronoRunnerId.trim()]
                )
            },
            cachedRacesBySeason = algDao.cachedRaceCountBySeason().associate { it.seasonYear to it.n },
            publishedRacesBySeason = algDao.getAllRacesSnapshot()
                .filter { hasPublishedResults(it, now) }
                .groupingBy { it.seasonYear }.eachCount(),
            repairs = repairs
        )
    }

    /** Météo réelle des courses courues par le Trio (une seule fois par course). */
    private suspend fun fetchMissingWeather(now: LocalDate): Int {
        val known = algDao.getRaceWeatherSnapshot().map { it.raceId }.toSet()
        val racedIds = algDao.getAllResultsSnapshot().map { it.raceId }.toSet()
        val candidates = algDao.getAllRacesSnapshot()
            .filter { it.raceId in racedIds && it.raceId !in known }
            .filter { it.dateIso < now.toString() }
            .filter { it.startLatitude != null && it.startLongitude != null }
            .sortedByDescending { it.dateIso }
            .take(MAX_WEATHER_PER_SYNC)
        var count = 0
        for (race in candidates) {
            val w = openMeteoClient.fetchRaceWeather(
                raceId = race.raceId,
                latitude = race.startLatitude!!,
                longitude = race.startLongitude!!,
                raceDateIso = race.dateIso,
                startHour = OpenMeteoClient.parseStartHour(race.schedule),
                today = now
            ).getOrNull()
            if (w != null) {
                algDao.upsertRaceWeather(w)
                count++
            }
        }
        return count
    }

    /**
     * Met à jour le profil et ses dossards pour une saison, puis recalcule cette saison
     * à partir des résultats déjà sur le téléphone (synchro complète si la saison n'en a pas encore).
     */
    suspend fun updateProfileBibAndDistance(
        profileId: String,
        seasonYear: Int,
        newDisplayName: String,
        newMyChronoId: String,
        defaultBib10km: String,
        defaultBib5km: String,
        preferredDistanceKm: Int,
        clubName: String,
        categoryLabel: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val existing = algDao.getRunnerProfile(profileId)
            ?: return@withContext Result.failure(IllegalArgumentException("Profil introuvable"))
        val cleanBib10 = defaultBib10km.trim().removePrefix("#")
        val cleanBib5 = defaultBib5km.trim().removePrefix("#")
        val resolvedPreferredDist = when {
            preferredDistanceKm == 5 && cleanBib5.isNotBlank() -> 5
            preferredDistanceKm == 10 && cleanBib10.isNotBlank() -> 10
            cleanBib5.isNotBlank() && cleanBib10.isBlank() -> 5
            cleanBib10.isNotBlank() && cleanBib5.isBlank() -> 10
            preferredDistanceKm == 5 -> 5
            else -> 10
        }

        algDao.upsertRunnerProfile(
            existing.copy(
                displayName = newDisplayName.trim().ifBlank { existing.displayName },
                myChronoRunnerId = newMyChronoId.trim(),
                defaultBib10km = cleanBib10,
                defaultBib5km = cleanBib5,
                preferredDistanceKm = resolvedPreferredDist,
                clubName = clubName.trim().ifBlank { existing.clubName },
                categoryLabel = categoryLabel.trim().ifBlank { existing.categoryLabel }
            )
        )
        algDao.upsertSeasonBib(SeasonBibEntity(profileId, seasonYear, cleanBib10, cleanBib5))

        if (algDao.getRowsForSeason(seasonYear).isEmpty()) {
            syncAll()
        } else {
            val repairs = repairRunnerIds()
            recomputeSeason(seasonYear)
            refreshDiagnostics(repairs)
            Result.success((listOf("Saison $seasonYear recalculée") + repairs).joinToString(" • "))
        }
    }

    /**
     * Enregistre le dossard et la distance (5 ou 10 km) d'un profil pour une course précise,
     * récupère les résultats de cette course si besoin, puis recalcule la saison.
     */
    suspend fun setRaceBibAndDistanceAndSync(
        raceId: String,
        profileId: String,
        bibNumber: String,
        distanceKm: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val race = algDao.getRaceById(raceId)
            ?: return@withContext Result.failure(IllegalStateException("Course introuvable"))
        val cleanBib = bibNumber.trim().removePrefix("#")
        // Dossard de la saison saisi sur la mauvaise distance (ex. le dossard 5 km tapé alors que
        // la fiche était sur le 10 km) : on prend la distance de ce dossard.
        val seasonBib = algDao.getSeasonBibsSnapshot()
            .firstOrNull { it.profileId == profileId && it.seasonYear == race.seasonYear }
        val cleanDist = when {
            seasonBib != null && cleanBib.isNotBlank() && cleanBib == seasonBib.bib5km -> 5
            seasonBib != null && cleanBib.isNotBlank() && cleanBib == seasonBib.bib10km -> 10
            distanceKm == 5 -> 5
            else -> 10
        }

        if (cleanBib.isBlank()) {
            algDao.deleteBibOverride(raceId, profileId)
            recomputeSeason(race.seasonYear)
            return@withContext Result.success("Dossard retiré sur cette course.")
        }
        algDao.upsertBibOverride(RaceBibOverrideEntity(raceId, profileId, cleanBib, cleanDist))

        if (algDao.getRowsForRace(raceId).isEmpty()) {
            myChronoApiClient.fetchRaceRawResults(race.myChronoEventId, race.isChallenge)
                .onFailure { return@withContext Result.failure(it) }
                .onSuccess { storeRows(race, it) }
        }
        recomputeSeason(race.seasonYear)

        val mine = algDao.getAllResultsSnapshot().filter { it.raceId == raceId && it.profileId == profileId }
        val found = mine.firstOrNull { it.distanceLabel == (if (cleanDist == 5) "5 km" else "10 km") } ?: mine.firstOrNull()
        if (found != null) {
            val pts = if (race.isChallenge) " (${found.challengePoints} pts)" else " (hors challenge)"
            Result.success(
                "Résultat trouvé sur le ${found.distanceName ?: found.distanceLabel} (dossard #${found.bibNumber}) : ${found.officialTimeFormatted}$pts"
            )
        } else {
            Result.failure(
                IllegalStateException("Aucun résultat MyChrono (${race.title}) pour le dossard #$cleanBib sur ${race.distanceNameFor(cleanDist)}.")
            )
        }
    }

    /** Terrain noté par le coureur, notes et sélection Simulateur Top 15 ("" = non renseigné). */
    suspend fun saveRaceTerrainWeather(
        raceId: String,
        profileId: String,
        terrainCondition: String,
        isPlannedForSimulator: Boolean,
        plannedDistanceKm: Int,
        simulatedTargetPoints: Int?,
        runnerNotes: String
    ) = withContext(Dispatchers.IO) {
        val existing = algDao.getTerrainWeather(raceId, profileId)
        algDao.upsertTerrainWeather(
            RaceTerrainWeatherEntity(
                raceId = raceId,
                profileId = profileId,
                terrainCondition = terrainCondition.trim(),
                weatherCondition = existing?.weatherCondition.orEmpty(),
                temperatureC = existing?.temperatureC,
                isPlannedForSimulator = isPlannedForSimulator,
                plannedDistanceKm = if (plannedDistanceKm == 5) 5 else 10,
                simulatedTargetPoints = simulatedTargetPoints?.coerceIn(0, 1200),
                terrainNotes = runnerNotes.trim()
            )
        )
    }

    /** Bascule rapide d'une course à venir dans le Simulateur Top 15. */
    suspend fun togglePlannedRaceForSimulator(
        raceId: String,
        profileId: String,
        distanceKm: Int,
        isPlanned: Boolean,
        defaultTargetPoints: Int
    ) = withContext(Dispatchers.IO) {
        val existing = algDao.getTerrainWeather(raceId, profileId)
        algDao.upsertTerrainWeather(
            RaceTerrainWeatherEntity(
                raceId = raceId,
                profileId = profileId,
                weatherCondition = existing?.weatherCondition.orEmpty(),
                temperatureC = existing?.temperatureC,
                terrainCondition = existing?.terrainCondition.orEmpty(),
                isPlannedForSimulator = isPlanned,
                plannedDistanceKm = if (distanceKm == 5) 5 else 10,
                simulatedTargetPoints = existing?.simulatedTargetPoints ?: defaultTargetPoints.coerceIn(0, 1200),
                terrainNotes = existing?.terrainNotes.orEmpty()
            )
        )
    }

    /** Importe une trace GPX locale choisie par le coureur et l'associe à une course. */
    suspend fun importGpxTrackForRace(
        context: Context,
        raceId: String,
        profileId: String,
        uri: Uri
    ): Result<ActivityMetricsEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalArgumentException("Impossible de lire le fichier GPX")
            val race = algDao.getRaceById(raceId)
            val parsed = GpxAndTrackUtils.parseGpxXmlContent(
                xmlContent = content,
                fallbackName = race?.title ?: "Trace GPX Gaume"
            )
            val entity = ActivityMetricsEntity(
                raceId = raceId,
                profileId = profileId,
                sourceType = "GPX",
                sourceTitle = parsed.title.ifBlank { race?.title ?: "Trace GPX Gaume" },
                recordedDistanceKm = parsed.distanceKm,
                elevationGainMeters = parsed.elevationGainMeters,
                avgHeartRateBpm = parsed.avgHeartRateBpm,
                maxHeartRateBpm = parsed.maxHeartRateBpm,
                cadenceSpm = null,
                serializedTrackPoints = parsed.serializedCoordinates
            )
            algDao.upsertSingleActivityMetric(entity)
            entity
        }
    }
}
