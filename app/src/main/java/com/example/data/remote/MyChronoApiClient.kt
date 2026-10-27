package com.example.data.remote

import com.example.data.local.MyChronoDirectoryEntry
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RunnerProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

data class MyChronoRawResultRow(
    val runnerId: String,
    val bibNumber: String,
    val fullName: String,
    val club: String,
    val category: String,
    val sex: String,
    val distanceLabel: String, // "10 km" ou "5 km"
    val distanceKm: Double,
    val timeSeconds: Int,
    val timeFormatted: String,
    val overallRank: Int,
    val totalFinishers: Int,
    val categoryRank: Int,
    val categoryTotal: Int,
    val points: Int,
    /** Libellé réel publié pour la distance (« 10 km », ou « 14 km » sur une course hors challenge). */
    val distanceName: String = ""
)

/** Un résultat de la fiche coureur MyChrono (/api/public/runners/{id}). */
data class MyChronoRunnerResult(
    /** Identifiant MyChrono de la course (= RaceEntity.myChronoEventId). */
    val raceEventId: String,
    val dateIso: String,
    val distanceName: String,
    val lengthMeters: Int?,
    val timeSeconds: Int,
    val rank: Int,
    val categoryRank: Int,
    val category: String,
    val points: Int,
    val finishers: Int
)

/** Fiche coureur MyChrono : identité et toutes ses courses, toutes compétitions confondues. */
data class MyChronoRunnerPage(
    val runnerId: String,
    val name: String,
    val club: String,
    val sex: String,
    val results: List<MyChronoRunnerResult>
)

/**
 * Client de l'API publique MyChrono (https://mychrono.softalux.com/api/public), qui publie
 * le calendrier et les résultats du Challenge des Allures Libres de Gaume.
 *
 * Règle : on ne lit que ce que MyChrono publie. Une information absente reste absente
 * (null / liste vide) ; elle n'est jamais remplacée par une valeur plausible.
 */
class MyChronoApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val MYCHRONO_PUBLIC_API = "https://mychrono.softalux.com/api/public"
        const val LABEL_10KM = "10 km"
        const val LABEL_5KM = "5 km"

        /** Règlement ALG : 1200 points au 1er de chaque distance, 1199 au 2e, et ainsi de suite. */
        fun reglementPointsForRank(rank: Int): Int = (1201 - rank).coerceAtLeast(1)

        fun raceIdFor(seasonYear: Int, roundNumber: Int): String = "alg_%d_r%02d".format(seasonYear, roundNumber)

        /**
         * Courses « Hors challenge » : MyChrono les numérote aussi à partir de 1, d'où un préfixe distinct
         * (sinon la course hors challenge n°4 écraserait la manche n°4).
         */
        fun horsChallengeRaceIdFor(seasonYear: Int, number: Int): String = "alg_%d_h%02d".format(seasonYear, number)

        /** Une course est hors challenge si MyChrono la publie comme course isolée ou dans la compétition « Hors challenge ». */
        fun isChallengeRace(kind: String, competition: String): Boolean {
            val k = kind.trim().lowercase(Locale.ROOT)
            val c = competition.trim().lowercase(Locale.ROOT)
            return k != "single" && !c.contains("hors challenge")
        }

        private fun nameTokens(name: String): Set<String> =
            java.text.Normalizer.normalize(name.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .split(Regex("[^a-z]+"))
                .filter { it.length >= 2 }
                .toSet()

        /**
         * Même personne d'après le nom : tous les mots du nom du profil se retrouvent dans le nom publié
         * (« Damien Collet » = « COLLET Damien », « Elodie » = « MARTIN Elodie »).
         */
        fun namesMatch(profileName: String, publishedName: String): Boolean {
            val wanted = nameTokens(profileName)
            return wanted.isNotEmpty() && nameTokens(publishedName).containsAll(wanted)
        }

        /**
         * Distance du challenge ("10 km" / "5 km") d'un résultat de la fiche coureur, avec les mêmes règles
         * que le classement de la course : libellé publié d'abord, sinon la longueur. Null pour une distance
         * enfants ou une marche.
         */
        fun labelForRunnerResult(race: RaceEntity, result: MyChronoRunnerResult): String? {
            val name = result.distanceName.trim()
            val lower = name.lowercase(Locale.ROOT)
            if (lower.contains("jeune") || lower.contains("enfant") || lower.contains("marche")) return null
            if (!race.isChallenge) {
                if (race.longDistanceLabel != null && name == race.longDistanceLabel) return LABEL_10KM
                if (race.shortDistanceLabel != null && name == race.shortDistanceLabel) return LABEL_5KM
            } else {
                if (Regex("^10\\b").containsMatchIn(lower)) return LABEL_10KM
                if (Regex("^5\\b").containsMatchIn(lower)) return LABEL_5KM
            }
            val meters = result.lengthMeters ?: return null
            return if (meters < 7200) LABEL_5KM else LABEL_10KM
        }

        /**
         * Fiche coureur https://mychrono.softalux.com/api/public/runners/{id} :
         * { "runner": {...}, "competitions": [ { "competition": {...}, "results": [...] } ] }.
         */
        fun parseRunnerPayload(jsonText: String): MyChronoRunnerPage? {
            val root = runCatching { JSONObject(jsonText.trim()) }.getOrNull() ?: return null
            val runner = root.optJSONObject("runner") ?: return null
            val id = runner.opt("id")?.toString()?.trim().orEmpty()
            if (id.isBlank() || id == "null") return null
            val results = mutableListOf<MyChronoRunnerResult>()
            val competitions = root.optJSONArray("competitions") ?: JSONArray()
            for (c in 0 until competitions.length()) {
                val list = competitions.optJSONObject(c)?.optJSONArray("results") ?: continue
                for (i in 0 until list.length()) {
                    val r = list.optJSONObject(i) ?: continue
                    val status = r.optString("status").lowercase(Locale.ROOT)
                    if (status.isNotBlank() && status != "finished") continue
                    val raceId = r.opt("raceId")?.toString()?.trim().orEmpty()
                    val rank = r.optInt("rank", 0)
                    val time = parseTimeValue(r.opt("time"))
                    if (raceId.isBlank() || raceId == "null" || rank <= 0 || time <= 0) continue
                    results += MyChronoRunnerResult(
                        raceEventId = raceId,
                        dateIso = r.optString("date").trim(),
                        distanceName = r.optString("distance").trim(),
                        lengthMeters = r.optInt("len", 0).takeIf { it > 0 },
                        timeSeconds = time,
                        rank = rank,
                        categoryRank = r.optInt("catRank", 0),
                        category = r.optString("cat").trim(),
                        points = r.optInt("points", 0).takeIf { it > 0 } ?: reglementPointsForRank(rank),
                        finishers = r.optInt("finishers", 0)
                    )
                }
            }
            return MyChronoRunnerPage(
                runnerId = id,
                name = runner.optString("name").trim(),
                club = runner.optString("club").trim().takeIf { it != "null" }.orEmpty(),
                sex = runner.optString("sex").trim().takeIf { it != "null" }.orEmpty(),
                results = results
            )
        }

        /** MyChrono encode le temps en entier : secondes × 1000 + rang ; un texte "mm:ss" est aussi accepté. */
        internal fun parseTimeValue(raw: Any?): Int = when (raw) {
            is Number -> raw.toLong().let { v ->
                when {
                    v > 100_000L -> (v / 1000L).toInt()
                    v > 0L -> v.toInt()
                    else -> 0
                }
            }
            is String -> {
                val parts = raw.trim()
                    .replace("'", ":").replace("h", ":").replace("m", ":").replace("s", "")
                    .split(":").mapNotNull { it.trim().toIntOrNull() }
                when (parts.size) {
                    2 -> parts[0] * 60 + parts[1]
                    3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                    else -> 0
                }
            }
            else -> 0
        }
    }

    private fun getJson(url: String): String? =
        httpClient.newCall(
            Request.Builder().url(url).header("Accept", "application/json").get().build()
        ).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string().orEmpty() else null
        }

    /** Calendrier publié d'une saison (manches et courses hors challenge). Liste vide si rien n'est publié. */
    suspend fun fetchSeasonCalendar(seasonYear: Int): Result<List<RaceEntity>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = getJson("$MYCHRONO_PUBLIC_API/races?year=$seasonYear")
                    ?: throw IllegalStateException("Serveur MyChrono injoignable")
                parseRacesPayload(body, seasonYear)
            }
        }

    /** Résultats bruts d'une course (tous les coureurs). */
    suspend fun fetchRaceRawResults(myChronoEventId: String, isChallenge: Boolean = true): Result<List<MyChronoRawResultRow>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = getJson("$MYCHRONO_PUBLIC_API/races/$myChronoEventId/results")
                    ?: throw IllegalStateException("Résultats MyChrono injoignables")
                // Réponse illisible (coupée, page d'erreur…) : échec, pour réessayer à la prochaine synchro
                // au lieu de garder un classement vide.
                val readable = runCatching { JSONObject(body.trim()).has("results") }.getOrDefault(false)
                if (!readable) throw IllegalStateException("Résultats MyChrono illisibles")
                parseResultsPayload(body, isChallenge)
            }
        }

    /** Fiche coureur MyChrono (toutes ses courses publiées). */
    suspend fun fetchRunnerPage(runnerId: String): Result<MyChronoRunnerPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = getJson("$MYCHRONO_PUBLIC_API/runners/${runnerId.trim()}")
                    ?: throw IllegalStateException("Fiche MyChrono injoignable")
                parseRunnerPayload(body) ?: throw IllegalStateException("Fiche MyChrono illisible")
            }
        }

    /** Plusieurs courses en parallèle ; une course encore en échec après 2 essais est absente du résultat. */
    suspend fun fetchManyRaceResults(races: List<RaceEntity>): Map<String, List<MyChronoRawResultRow>> {
        val first = fetchBatch(races, parallel = 4)
        val failed = races.filter { it.raceId !in first }
        if (failed.isEmpty()) return first
        // Deuxième essai, une course à la fois.
        return first + fetchBatch(failed, parallel = 1)
    }

    private suspend fun fetchBatch(races: List<RaceEntity>, parallel: Int): Map<String, List<MyChronoRawResultRow>> =
        coroutineScope {
            val semaphore = Semaphore(parallel)
            races.map { race ->
                async {
                    semaphore.withPermit {
                        race.raceId to fetchRaceRawResults(race.myChronoEventId, race.isChallenge).getOrNull()
                    }
                }
            }.awaitAll()
                .mapNotNull { (raceId, rows) -> rows?.let { raceId to it } }
                .toMap()
        }

    fun buildDirectoryFromRows(rows: List<MyChronoRawResultRow>): List<MyChronoDirectoryEntry> = buildDirectory(rows)

    private fun buildDirectory(rows: List<MyChronoRawResultRow>): List<MyChronoDirectoryEntry> {
        val byKey = LinkedHashMap<String, MyChronoDirectoryEntry>()
        rows.filter { it.runnerId.isNotBlank() }.forEach { r ->
            val existing = byKey[r.runnerId]
            byKey[r.runnerId] = MyChronoDirectoryEntry(
                myChronoRunnerId = r.runnerId,
                fullName = r.fullName,
                defaultBib10km = if (r.distanceLabel == LABEL_10KM && r.bibNumber.isNotBlank()) r.bibNumber
                else existing?.defaultBib10km.orEmpty(),
                defaultBib5km = if (r.distanceLabel == LABEL_5KM && r.bibNumber.isNotBlank()) r.bibNumber
                else existing?.defaultBib5km.orEmpty(),
                club = r.club.ifBlank { existing?.club.orEmpty() },
                category = r.category.ifBlank { existing?.category.orEmpty() },
                sampleRaceCount = (existing?.sampleRaceCount ?: 0) + 1,
                bestPoints = max(existing?.bestPoints ?: 0, r.points)
            )
        }
        return byKey.values.toList()
    }

    /**
     * Résultats d'un profil sur une course, séparément pour le 10 km et le 5 km.
     *
     * 1. Dossard saisi pour cette course précise : cherché sur sa distance (identifiant puis dossard,
     *    même porté par un autre identifiant : dossard prêté). S'il n'y est pas, on passe à la suite
     *    (un dossard saisi sur la mauvaise distance ne bloque plus rien).
     * 2. Identifiant MyChrono du coureur, sur les deux distances : il est le même au 10 km et au 5 km,
     *    et d'une saison à l'autre. C'est la règle principale.
     * 3. Dossard de la saison, uniquement sur une distance où l'identifiant n'a rien donné, et jamais
     *    s'il est porté par un autre identifiant MyChrono ni sur l'autre distance.
     *
     * Course hors challenge : étapes 1 et 2 seulement (le dossard de la saison n'y est pas valable),
     * et les points ne comptent pas (0).
     */
    fun findMatchingResultsForProfile(
        raceId: String,
        seasonYear: Int,
        profile: RunnerProfileEntity,
        raceOverride: RaceBibOverrideEntity?,
        rawRows: List<MyChronoRawResultRow>,
        isChallenge: Boolean = true
    ): List<RaceResultEntity> {
        if (rawRows.isEmpty()) return emptyList()
        fun entity(row: MyChronoRawResultRow) = toRaceResultEntity(raceId, seasonYear, profile.profileId, row, isChallenge)
        val runnerId = profile.myChronoRunnerId.trim()

        if (raceOverride != null && raceOverride.bibNumber.isNotBlank()) {
            val label = if (raceOverride.distanceKm == 5) LABEL_5KM else LABEL_10KM
            findRowInDistance(
                rows = rawRows.filter { it.distanceLabel == label },
                runnerId = runnerId,
                bib = raceOverride.bibNumber,
                bibIsExplicitForThisRace = true
            )?.let { return listOf(entity(it)) }
        }

        val byId = if (runnerId.isBlank()) emptyList() else listOf(LABEL_10KM, LABEL_5KM).mapNotNull { label ->
            rawRows.firstOrNull { it.distanceLabel == label && it.runnerId == runnerId }
        }
        if (!isChallenge) return byId.map { entity(it) }

        val foundLabels = byId.map { it.distanceLabel }.toSet()
        val byBib = listOf(LABEL_10KM to profile.defaultBib10km, LABEL_5KM to profile.defaultBib5km)
            .filter { (label, bib) -> bib.isNotBlank() && label !in foundLabels }
            .mapNotNull { (label, bib) ->
                findRowInDistance(
                    rows = rawRows.filter { it.distanceLabel == label },
                    runnerId = runnerId,
                    bib = bib,
                    bibIsExplicitForThisRace = false,
                    profileName = profile.displayName
                )
            }
        return (byId + byBib).map { entity(it) }
    }

    private fun findRowInDistance(
        rows: List<MyChronoRawResultRow>,
        runnerId: String,
        bib: String,
        bibIsExplicitForThisRace: Boolean,
        profileName: String = ""
    ): MyChronoRawResultRow? {
        if (rows.isEmpty()) return null
        val cleanRunnerId = runnerId.trim()
        if (cleanRunnerId.isNotBlank()) {
            rows.firstOrNull { it.runnerId == cleanRunnerId }?.let { return it }
        }
        val cleanBib = normalizeBib(bib)
        if (cleanBib.isBlank()) return null
        val bibRow = rows.firstOrNull { normalizeBib(it.bibNumber) == cleanBib } ?: return null
        // Dossard porté sous un autre identifiant : refusé, sauf si c'est le même nom (MyChrono a fusionné
        // deux fiches du coureur après coup, et le classement gardé sur le téléphone a l'ancien identifiant).
        val belongsToSomeoneElse = cleanRunnerId.isNotBlank() &&
            bibRow.runnerId.isNotBlank() &&
            bibRow.runnerId != cleanRunnerId &&
            !namesMatch(profileName, bibRow.fullName)
        return if (belongsToSomeoneElse && !bibIsExplicitForThisRace) null else bibRow
    }

    private fun toRaceResultEntity(
        raceId: String,
        seasonYear: Int,
        profileId: String,
        row: MyChronoRawResultRow,
        isChallenge: Boolean
    ): RaceResultEntity {
        val paceSecPerKm = (row.timeSeconds / row.distanceKm).roundToInt()
        val speedKmh = ((row.distanceKm / (row.timeSeconds / 3600.0)) * 100.0).roundToInt() / 100.0
        return RaceResultEntity(
            raceId = raceId,
            profileId = profileId,
            seasonYear = seasonYear,
            bibNumber = row.bibNumber,
            distanceLabel = row.distanceLabel,
            officialTimeSeconds = row.timeSeconds,
            officialTimeFormatted = row.timeFormatted,
            pacePerKmFormatted = "%d:%02d /km".format(paceSecPerKm / 60, paceSecPerKm % 60),
            speedKmh = speedKmh,
            overallRank = row.overallRank,
            totalFinishers = row.totalFinishers,
            categoryRank = row.categoryRank,
            categoryTotal = row.categoryTotal,
            challengePoints = if (isChallenge) row.points else 0,
            distanceName = row.distanceName.ifBlank { row.distanceLabel },
            distanceKm = row.distanceKm
        )
    }

    /** Distance adulte d'une course, rangée en grande distance ("10 km") ou petite distance ("5 km"). */
    data class DistanceSlot(val label: String, val name: String, val km: Double)

    /**
     * Distances adultes d'une course, indexées par id MyChrono. Les distances enfants sont ignorées.
     * Manche du challenge : « 10 km » et « 5 km » selon le libellé.
     * Hors challenge : la plus longue occupe la place du 10 km, la suivante celle du 5 km.
     */
    fun parseDistanceSlots(distArr: JSONArray?, isChallenge: Boolean = true): Map<Int, DistanceSlot> {
        if (distArr == null) return emptyMap()
        data class Adult(val id: Int, val label: String, val km: Double?)
        val adults = (0 until distArr.length()).mapNotNull { i ->
            val d = distArr.optJSONObject(i) ?: return@mapNotNull null
            val kind = d.optString("kind").lowercase(Locale.ROOT)
            val label = d.optString("label").trim()
            val lower = label.lowercase(Locale.ROOT)
            if (kind == "kids" || lower.contains("jeune") || lower.contains("enfant") || lower.contains("marche")) {
                return@mapNotNull null
            }
            if (d.optBoolean("noTime", false)) return@mapNotNull null
            val meters = listOf("effective", "real", "length")
                .firstNotNullOfOrNull { key -> d.optInt(key, 0).takeIf { it > 0 } }
            Adult(d.optInt("id", -1), label, meters?.let { (it / 100.0).roundToInt() / 10.0 })
        }
        if (adults.isEmpty()) return emptyMap()

        if (!isChallenge) {
            val withKm = adults.filter { it.km != null }.sortedByDescending { it.km!! }
            if (withKm.isEmpty()) return emptyMap()
            if (withKm.size == 1) {
                val a = withKm.first()
                return mapOf(a.id to DistanceSlot(if (a.km!! < 7.2) LABEL_5KM else LABEL_10KM, a.label, a.km))
            }
            return withKm.take(2).mapIndexed { idx, a ->
                a.id to DistanceSlot(if (idx == 0) LABEL_10KM else LABEL_5KM, a.label, a.km!!)
            }.toMap()
        }

        fun labelFor(a: Adult): String? {
            val lbl = a.label.lowercase(Locale.ROOT)
            return when {
                Regex("^10\\b").containsMatchIn(lbl) -> LABEL_10KM
                Regex("^5\\b").containsMatchIn(lbl) -> LABEL_5KM
                adults.size == 2 && a.km != null && adults.all { it.km != null } ->
                    if (a.km == adults.maxOf { it.km!! }) LABEL_10KM else LABEL_5KM
                a.km != null -> if (a.km < 7.2) LABEL_5KM else LABEL_10KM
                else -> null
            }
        }

        return adults.mapNotNull { a ->
            val label = labelFor(a) ?: return@mapNotNull null
            val km = a.km ?: return@mapNotNull null
            a.id to DistanceSlot(label, a.label.ifBlank { label }, km)
        }.toMap()
    }

    /**
     * Calendrier de https://mychrono.softalux.com/api/public/races?year=AAAA
     * (objet { "races": [...] }).
     */
    fun parseRacesPayload(jsonText: String, seasonYear: Int): List<RaceEntity> {
        val trimmed = jsonText.trim()
        val array = when {
            trimmed.startsWith("{") -> runCatching { JSONObject(trimmed).optJSONArray("races") }.getOrNull()
            trimmed.startsWith("[") -> runCatching { JSONArray(trimmed) }.getOrNull()
            else -> null
        } ?: return emptyList()

        val list = mutableListOf<RaceEntity>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val eventId = item.opt("id")?.toString().orEmpty()
            val roundNumber = item.optInt("num", 0)
            val dateIso = item.optString("date").take(10)
            if (eventId.isBlank() || roundNumber <= 0 || dateIso.length != 10) continue
            if (item.has("year") && item.optInt("year") != seasonYear) continue

            val isChallenge = isChallengeRace(item.optString("kind"), item.optString("competition"))
            val place = item.optString("place").trim()
            val name = item.optString("name").trim()
            val title = when {
                name.isNotBlank() && !name.equals(place, ignoreCase = true) && place.isNotBlank() -> "$place — $name"
                name.isNotBlank() -> name
                else -> place.ifBlank { if (isChallenge) "Manche $roundNumber" else "Course hors challenge" }
            }

            val distances = parseDistanceSlots(item.optJSONArray("distances"), isChallenge).values
            val longSlot = distances.firstOrNull { it.label == LABEL_10KM }
            val shortSlot = distances.firstOrNull { it.label == LABEL_5KM }
            val long = longSlot?.km
            val short = shortSlot?.km

            val info = item.optJSONObject("info")
            val facilities = info?.optJSONObject("facilities")
            fun fac(key: String): Boolean? = facilities?.takeIf { it.has(key) }?.optBoolean(key)
            fun txt(key: String): String? = info?.optString(key)?.trim()?.takeIf { it.isNotBlank() && it != "null" }
            fun coord(key: String): Double? = info?.takeIf { it.has(key) && !it.isNull(key) }
                ?.optDouble(key)?.takeIf { !it.isNaN() && it != 0.0 }

            val maps = info?.optJSONArray("maps")
            val routeLinks = if (maps == null) null else (0 until maps.length()).mapNotNull { m ->
                val o = maps.optJSONObject(m) ?: return@mapNotNull null
                val url = o.optString("url").trim()
                if (!url.startsWith("http")) return@mapNotNull null
                "${o.optString("label").ifBlank { "Parcours" }.replace('\t', ' ')}\t$url"
            }.joinToString("\n").ifBlank { null }

            val status = item.optString("status").lowercase(Locale.ROOT).ifBlank { null }
            val elevation = listOf("elevation_gain", "d_plus")
                .firstNotNullOfOrNull { key -> item.optInt(key, 0).takeIf { item.has(key) && it > 0 } }

            list.add(
                RaceEntity(
                    raceId = if (isChallenge) raceIdFor(seasonYear, roundNumber) else horsChallengeRaceIdFor(seasonYear, roundNumber),
                    seasonYear = seasonYear,
                    roundNumber = roundNumber,
                    myChronoEventId = eventId,
                    title = title,
                    village = place.ifBlank { title },
                    dateIso = dateIso,
                    distanceKm = long ?: 0.0,
                    elevationGainMeters = elevation,
                    isCompleted = status in setOf("closed", "completed", "finished", "results") ||
                        item.optBoolean("has_results", false),
                    distance5Km = short,
                    startLatitude = coord("lat"),
                    startLongitude = coord("lng"),
                    address = txt("address"),
                    schedule = txt("schedule"),
                    kidsSchedule = txt("kids"),
                    terrainDescription = txt("terrain"),
                    hasGarderie = fac("garderie"),
                    hasDouche = fac("douche"),
                    hasVestiaire = fac("vestiaire"),
                    hasResto = fac("resto"),
                    routeLinks = routeLinks,
                    sourceUrl = txt("source"),
                    statusLabel = status,
                    finishersCount = item.optInt("finishers", 0).takeIf { it > 0 },
                    isChallenge = isChallenge,
                    competitionLabel = item.optString("competition").trim().ifBlank { null },
                    longDistanceLabel = if (isChallenge) null else longSlot?.name,
                    shortDistanceLabel = if (isChallenge) null else shortSlot?.name
                )
            )
        }
        return list.sortedWith(compareBy<RaceEntity>({ !it.isChallenge }, { it.roundNumber }))
    }

    /**
     * Résultats de https://mychrono.softalux.com/api/public/races/{id}/results :
     * { "race": { "distances": [...] }, "results": [ { "distanceId": 4, "rows": [...] }, ... ] }.
     * Un format non reconnu donne une liste vide (aucun classement deviné).
     */
    fun parseResultsPayload(jsonText: String, isChallenge: Boolean = true): List<MyChronoRawResultRow> {
        val root = runCatching { JSONObject(jsonText.trim()) }.getOrNull() ?: return emptyList()
        val groups = root.optJSONArray("results") ?: return emptyList()
        val distances = parseDistanceSlots(
            root.optJSONObject("race")?.optJSONArray("distances") ?: root.optJSONArray("distances"),
            isChallenge
        )

        val rows = mutableListOf<MyChronoRawResultRow>()
        for (g in 0 until groups.length()) {
            val group = groups.optJSONObject(g) ?: continue
            val dist = distances[group.optInt("distanceId", -1)] ?: continue
            val groupRows = group.optJSONArray("rows") ?: continue

            data class Parsed(
                val rank: Int, val bib: String, val name: String, val cat: String, val sex: String,
                val catRank: Int, val club: String, val timeSec: Int, val points: Int, val runnerId: String
            )

            val parsed = (0 until groupRows.length()).mapNotNull { r ->
                val row = groupRows.optJSONObject(r) ?: return@mapNotNull null
                val status = row.optString("status").lowercase(Locale.ROOT)
                if (status.isNotBlank() && status != "finished") return@mapNotNull null
                val rank = row.optInt("rank", 0)
                if (rank <= 0) return@mapNotNull null
                val timeSec = parseTimeSeconds(row)
                if (timeSec <= 0) return@mapNotNull null
                Parsed(
                    rank = rank,
                    bib = row.opt("bib")?.toString()?.trim().orEmpty().let { if (it == "0" || it == "null") "" else it },
                    name = row.optString("name").trim(),
                    cat = row.optString("cat").trim(),
                    sex = row.optString("sex").trim(),
                    catRank = row.optInt("catRank", 0),
                    club = row.optString("club").trim(),
                    timeSec = timeSec,
                    points = row.optInt("points", 0).takeIf { it > 0 } ?: reglementPointsForRank(rank),
                    runnerId = row.optInt("runnerId", 0).takeIf { it > 0 }?.toString().orEmpty()
                )
            }.sortedBy { it.rank }

            val totalInDistance = parsed.size
            val totalByCategory = parsed.groupingBy { it.cat }.eachCount()
            val seenInCategory = HashMap<String, Int>()

            parsed.forEach { p ->
                val positionInCategory = (seenInCategory[p.cat] ?: 0) + 1
                seenInCategory[p.cat] = positionInCategory
                rows.add(
                    MyChronoRawResultRow(
                        runnerId = p.runnerId,
                        bibNumber = p.bib,
                        fullName = p.name,
                        club = p.club,
                        category = p.cat,
                        sex = p.sex,
                        distanceLabel = dist.label,
                        distanceKm = dist.km,
                        timeSeconds = p.timeSec,
                        timeFormatted = formatSecondsToMmSs(p.timeSec),
                        overallRank = p.rank,
                        totalFinishers = totalInDistance,
                        categoryRank = p.catRank.takeIf { it > 0 } ?: positionInCategory,
                        categoryTotal = totalByCategory[p.cat] ?: 1,
                        points = p.points,
                        distanceName = dist.name
                    )
                )
            }
        }
        return rows
    }

    /**
     * MyChrono encode le temps en entier : secondes × 1000 + rang (ex. 3254075 = 54:14 pour le 75e).
     * On garde les secondes. Un temps texte "mm:ss" ou "h:mm:ss" est aussi accepté.
     */
    private fun parseTimeSeconds(row: JSONObject): Int = parseTimeValue(row.opt("time"))

    private fun normalizeBib(bib: String): String =
        bib.trim().removePrefix("#").trimStart('0')

    private fun formatSecondsToMmSs(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val min = (totalSeconds % 3600) / 60
        val sec = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, min, sec) else "%02d:%02d".format(min, sec)
    }
}
