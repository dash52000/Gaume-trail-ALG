package com.example.data.remote

import com.example.data.local.GaumeInitialSeedData
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
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

data class MyChronoSeasonSyncPayload(
    val seasonYear: Int,
    val updatedRaces: List<RaceEntity>,
    val rawRowsByRaceId: Map<String, List<MyChronoRawResultRow>>,
    val discoveredDirectoryEntries: List<MyChronoDirectoryEntry>,
    val message: String
)

data class MyChronoRawResultRow(
    val runnerId: String,
    val bibNumber: String,
    val fullName: String,
    val club: String,
    val category: String,
    val distanceLabel: String, // "10 km" ou "5 km"
    val distanceKm: Double,
    val timeSeconds: Int,
    val timeFormatted: String,
    val overallRank: Int,
    val totalFinishers: Int,
    val categoryRank: Int,
    val categoryTotal: Int,
    val points: Int
)

/**
 * Client HTTP officiel pour récupérer les courses et résultats du Challenge des Allures Libres de Gaume (ALG)
 * depuis l'API publique MyChrono (https://mychrono.softalux.com/api/public) à partir de 2026 uniquement,
 * avec séparation stricte entre le Challenge 10 km et le Challenge 5 km.
 */
class MyChronoApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val MYCHRONO_PUBLIC_API = "https://mychrono.softalux.com/api/public"
    }

    suspend fun fetchSeasonFromMyChrono(
        seasonYear: Int,
        existingRaces: List<RaceEntity>,
        profiles: List<RunnerProfileEntity> = emptyList()
    ): Result<MyChronoSeasonSyncPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val targetYear = max(2026, seasonYear)
            val racesUrl = "$MYCHRONO_PUBLIC_API/races?year=$targetYear"
            val request = Request.Builder()
                .url(racesUrl)
                .header("Accept", "application/json")
                .get()
                .build()

            val bodyString = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Serveur MyChrono HTTP ${response.code}")
                }
                response.body?.string().orEmpty()
            }

            val fallbackCalendar = existingRaces.ifEmpty {
                GaumeInitialSeedData.buildOfficialGaumeSeasonCalendar(targetYear)
            }
            val parsedRaces = parseRacesPayload(bodyString, targetYear, fallbackCalendar)

            val rawRowsByRace = LinkedHashMap<String, List<MyChronoRawResultRow>>()
            val directoryMap = LinkedHashMap<String, MyChronoDirectoryEntry>()
            val semaphore = Semaphore(6)

            val fetchedResultsByRaceId = coroutineScope {
                parsedRaces.map { race ->
                    async {
                        semaphore.withPermit {
                            val resultsUrl = "$MYCHRONO_PUBLIC_API/races/${race.myChronoEventId}/results"
                            val resultsReq = Request.Builder()
                                .url(resultsUrl)
                                .header("Accept", "application/json")
                                .get()
                                .build()

                            val rows = runCatching {
                                httpClient.newCall(resultsReq).execute().use { resp ->
                                    if (resp.isSuccessful) {
                                        parseResultsPayload(resp.body?.string().orEmpty())
                                    } else {
                                        emptyList()
                                    }
                                }
                            }.getOrDefault(emptyList())

                            race.raceId to rows
                        }
                    }
                }.awaitAll()
            }

            for ((raceId, rows) in fetchedResultsByRaceId) {
                if (rows.isNotEmpty()) {
                    rawRowsByRace[raceId] = rows
                    rows.forEach { r ->
                        val key = if (r.runnerId.isNotBlank()) {
                            r.runnerId
                        } else {
                            "${normalizeText(r.fullName)}_${r.bibNumber}"
                        }
                        val existingEntry = directoryMap[key]
                        val bib10 = if (r.distanceLabel == "10 km" && r.bibNumber.isNotBlank()) {
                            r.bibNumber
                        } else {
                            existingEntry?.defaultBib10km.orEmpty()
                        }
                        val bib5 = if (r.distanceLabel == "5 km" && r.bibNumber.isNotBlank()) {
                            r.bibNumber
                        } else {
                            existingEntry?.defaultBib5km.orEmpty()
                        }
                        directoryMap[key] = MyChronoDirectoryEntry(
                            myChronoRunnerId = r.runnerId.ifBlank { existingEntry?.myChronoRunnerId.orEmpty() },
                            fullName = r.fullName,
                            defaultBib10km = bib10,
                            defaultBib5km = bib5,
                            club = r.club.ifBlank { existingEntry?.club ?: "Allure Libre Gaume" },
                            category = r.category.ifBlank { existingEntry?.category ?: "Général" },
                            sampleRaceCount = (existingEntry?.sampleRaceCount ?: 0) + 1,
                            bestPoints = max(existingEntry?.bestPoints ?: 0, r.points)
                        )
                    }
                }
            }

            val finalRaces = parsedRaces.map { race ->
                val hasResults = rawRowsByRace[race.raceId]?.isNotEmpty() == true
                race.copy(isCompleted = hasResults || race.isCompleted)
            }

            val discoveredList = directoryMap.values.toList()
            MyChronoSeasonSyncPayload(
                seasonYear = targetYear,
                updatedRaces = finalRaces,
                rawRowsByRaceId = rawRowsByRace,
                discoveredDirectoryEntries = discoveredList,
                message = "MyChrono $targetYear synchronisé (${finalRaces.size} manches, ${rawRowsByRace.size} classements)."
            )
        }
    }

    suspend fun fetchRaceRawResults(
        myChronoEventId: String,
        seasonYear: Int
    ): Result<List<MyChronoRawResultRow>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEventId = myChronoEventId.removePrefix("${seasonYear}_").ifBlank { myChronoEventId }
            val urls = listOf(
                "$MYCHRONO_PUBLIC_API/races/$myChronoEventId/results",
                "$MYCHRONO_PUBLIC_API/races/$cleanEventId/results"
            ).distinct()

            for (resultsUrl in urls) {
                val req = Request.Builder()
                    .url(resultsUrl)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                val rows = runCatching {
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            parseResultsPayload(resp.body?.string().orEmpty())
                        } else {
                            emptyList()
                        }
                    }
                }.getOrDefault(emptyList())

                if (rows.isNotEmpty()) return@runCatching rows
            }
            emptyList()
        }
    }

    /**
     * Retourne séparément le résultat 10 km et/ou le résultat 5 km d'un coureur sur une course.
     * RÈGLE STRICTE :
     * - Si aucun numéro de dossard n'est attribué pour une distance (ex. defaultBib10km vide pour Laetitia/Élodie),
     *   aucun résultat n'est recherché ni retourné pour cette distance (0 course).
     * - Ne mélange jamais le 5 km et le 10 km.
     */
    fun findMatchingResultsForProfile(
        raceId: String,
        seasonYear: Int,
        profile: RunnerProfileEntity,
        raceOverride: RaceBibOverrideEntity?,
        rawRows: List<MyChronoRawResultRow>
    ): List<RaceResultEntity> {
        if (rawRows.isEmpty()) return emptyList()

        if (raceOverride != null && raceOverride.bibNumber.isNotBlank()) {
            val targetDistanceLabel = if (raceOverride.distanceKm == 5) "5 km" else "10 km"
            val matchedRow = findSingleDistanceMatch(
                rawRows = rawRows,
                targetDistanceLabel = targetDistanceLabel,
                targetBib = normalizeBib(raceOverride.bibNumber),
                profile = profile,
                allowFallbackAcrossDistancesForExplicitOverride = true
            )
            return if (matchedRow != null) {
                listOf(toRaceResultEntity(raceId, seasonYear, profile.profileId, matchedRow, targetDistanceLabel))
            } else {
                emptyList()
            }
        }

        val results = mutableListOf<RaceResultEntity>()

        // 1) Recherche dans les résultats 10 km UNIQUEMENT si un dossard 10 km est attribué au profil
        val cleanBib10 = normalizeBib(profile.defaultBib10km)
        if (cleanBib10.isNotBlank()) {
            val row10km = findSingleDistanceMatch(
                rawRows = rawRows,
                targetDistanceLabel = "10 km",
                targetBib = cleanBib10,
                profile = profile,
                allowFallbackAcrossDistancesForExplicitOverride = false
            )
            if (row10km != null) {
                results.add(toRaceResultEntity(raceId, seasonYear, profile.profileId, row10km, "10 km"))
            }
        }

        // 2) Recherche dans les résultats 5 km UNIQUEMENT si un dossard 5 km est attribué au profil
        val cleanBib5 = normalizeBib(profile.defaultBib5km)
        if (cleanBib5.isNotBlank()) {
            val row5km = findSingleDistanceMatch(
                rawRows = rawRows,
                targetDistanceLabel = "5 km",
                targetBib = cleanBib5,
                profile = profile,
                allowFallbackAcrossDistancesForExplicitOverride = false
            )
            if (row5km != null) {
                results.add(toRaceResultEntity(raceId, seasonYear, profile.profileId, row5km, "5 km"))
            }
        }

        return results
    }

    private fun findSingleDistanceMatch(
        rawRows: List<MyChronoRawResultRow>,
        targetDistanceLabel: String,
        targetBib: String,
        profile: RunnerProfileEntity,
        allowFallbackAcrossDistancesForExplicitOverride: Boolean
    ): MyChronoRawResultRow? {
        if (targetBib.isBlank()) return null

        val distanceRows = rawRows.filter { it.distanceLabel == targetDistanceLabel }
        if (distanceRows.isEmpty() && !allowFallbackAcrossDistancesForExplicitOverride) return null

        val normalizedProfileName = normalizeText(profile.displayName)
        val nameTokens = normalizedProfileName.split(" ").filter { it.length >= 3 }

        // 1. Correspondance par numéro de dossard dans la distance cible
        val bibCandidates = distanceRows.filter { normalizeBib(it.bibNumber) == targetBib }
        if (bibCandidates.size == 1) return bibCandidates.first()
        if (bibCandidates.size > 1) {
            bibCandidates.firstOrNull { candidate ->
                val candName = normalizeText(candidate.fullName)
                nameTokens.isNotEmpty() && nameTokens.all { token -> candName.contains(token) }
            }?.let { return it }
            return bibCandidates.first()
        }

        if (allowFallbackAcrossDistancesForExplicitOverride) {
            val anyBibCandidates = rawRows.filter { normalizeBib(it.bibNumber) == targetBib }
            if (anyBibCandidates.isNotEmpty()) return anyBibCandidates.first()
            return null
        }

        // 2. Fallback par runnerId MyChrono (ex: 1171 pour Damien) strictement dans la même distance
        if (profile.myChronoRunnerId.isNotBlank()) {
            distanceRows.firstOrNull {
                it.runnerId.equals(profile.myChronoRunnerId, ignoreCase = true)
            }?.let { return it }
        }

        // 3. Fallback par nom complet (au moins 2 mots) strictement dans la même distance
        if (nameTokens.size >= 2) {
            distanceRows.firstOrNull { candidate ->
                val candName = normalizeText(candidate.fullName)
                nameTokens.all { token -> candName.contains(token) }
            }?.let { return it }
        }

        return null
    }

    private fun toRaceResultEntity(
        raceId: String,
        seasonYear: Int,
        profileId: String,
        matchedRow: MyChronoRawResultRow,
        canonicalDistanceLabel: String
    ): RaceResultEntity {
        val distKm = if (matchedRow.distanceKm > 1.0) {
            matchedRow.distanceKm
        } else if (canonicalDistanceLabel == "5 km") {
            5.0
        } else {
            10.0
        }
        val safeSeconds = matchedRow.timeSeconds.coerceAtLeast(60)
        val paceSecPerKm = (safeSeconds / distKm).roundToInt()
        val paceMin = paceSecPerKm / 60
        val paceSec = paceSecPerKm % 60
        val speedKmh = ((distKm / (safeSeconds / 3600.0)) * 100.0).roundToInt() / 100.0

        return RaceResultEntity(
            raceId = raceId,
            profileId = profileId,
            seasonYear = seasonYear,
            bibNumber = matchedRow.bibNumber.ifBlank { "--" },
            distanceLabel = canonicalDistanceLabel,
            officialTimeSeconds = safeSeconds,
            officialTimeFormatted = matchedRow.timeFormatted,
            pacePerKmFormatted = "%d:%02d /km".format(paceMin, paceSec),
            speedKmh = speedKmh,
            overallRank = matchedRow.overallRank,
            totalFinishers = matchedRow.totalFinishers,
            categoryRank = matchedRow.categoryRank,
            categoryTotal = matchedRow.categoryTotal,
            challengePoints = matchedRow.points
        )
    }

    /**
     * Analyse la liste des courses depuis https://mychrono.softalux.com/api/public/races?year=2026
     */
    fun parseRacesPayload(
        jsonText: String,
        seasonYear: Int,
        existingRaces: List<RaceEntity>
    ): List<RaceEntity> {
        val trimmed = jsonText.trim()
        if (trimmed.isBlank()) return existingRaces

        val array = when {
            trimmed.startsWith("[") -> runCatching { JSONArray(trimmed) }.getOrNull() ?: JSONArray()
            trimmed.startsWith("{") -> {
                val obj = runCatching { JSONObject(trimmed) }.getOrNull()
                obj?.optJSONArray("data")
                    ?: obj?.optJSONArray("races")
                    ?: obj?.optJSONArray("items")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        if (array.length() == 0) return existingRaces

        val list = mutableListOf<RaceEntity>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val eventId = item.optString("id")
                .ifBlank { item.optString("race_id") }
                .ifBlank { "${seasonYear}_${i + 1}" }

            val roundNumber = item.optInt(
                "round",
                item.optInt("order", item.optInt("num", i + 1))
            )

            val rawName = item.optString("name").ifBlank { item.optString("title") }
            val village = item.optString("city")
                .ifBlank { item.optString("location") }
                .ifBlank { item.optString("place") }
                .ifBlank { item.optString("village") }
                .ifBlank { "Gaume" }

            val title = when {
                rawName.isNotBlank() && !rawName.equals(village, ignoreCase = true) -> "$village — $rawName"
                rawName.isNotBlank() -> "Allure Libre de $rawName"
                else -> "Manche #${i + 1}"
            }

            val dateIso = item.optString("date")
                .ifBlank { item.optString("start_date") }
                .take(10)
                .ifBlank { "$seasonYear-03-01" }

            var distanceKm = item.optDouble("distance_km", item.optDouble("distance", 10.0)).let {
                if (it.isNaN() || it <= 0.0) 10.0 else it
            }
            val distArr = item.optJSONArray("distances")
            if (distArr != null && distArr.length() > 0) {
                var maxAdultKm = 0.0
                for (dIdx in 0 until distArr.length()) {
                    val dObj = distArr.optJSONObject(dIdx) ?: continue
                    val kind = dObj.optString("kind", "adult").lowercase(Locale.ROOT)
                    val label = dObj.optString("label", "")
                    if (kind == "kids" || label.lowercase(Locale.ROOT).contains("enfant")) continue
                    val meters = dObj.optInt("effective", dObj.optInt("real", dObj.optInt("length", 0)))
                    val km = if (meters > 100) {
                        ((meters / 100.0).roundToInt() / 10.0)
                    } else {
                        dObj.optDouble("distance", 0.0)
                    }
                    if (km > maxAdultKm) maxAdultKm = km
                }
                if (maxAdultKm > 0.0) distanceKm = maxAdultKm
            }

            val elevation = item.optInt("elevation_gain", item.optInt("d_plus", 150))
            val statusStr = item.optString("status", "").lowercase(Locale.ROOT)
            val isCompleted = item.optBoolean("has_results", false) ||
                item.optBoolean("published", false) ||
                item.optBoolean("is_completed", false) ||
                statusStr == "completed" ||
                statusStr == "finished" ||
                statusStr == "results"

            val formattedId = "alg_%d_r%02d".format(seasonYear, roundNumber)

            list.add(
                RaceEntity(
                    raceId = formattedId,
                    seasonYear = seasonYear,
                    roundNumber = roundNumber,
                    myChronoEventId = eventId,
                    title = title,
                    village = village,
                    dateIso = dateIso,
                    distanceKm = distanceKm,
                    elevationGainMeters = elevation,
                    isCompleted = isCompleted
                )
            )
        }

        return list.sortedBy { it.roundNumber }
    }

    /**
     * Analyse les résultats JSON d'une course depuis https://mychrono.softalux.com/api/public/races/{id}/results
     */
    fun parseResultsPayload(jsonText: String): List<MyChronoRawResultRow> {
        val trimmed = jsonText.trim()
        if (trimmed.isBlank()) return emptyList()

        if (trimmed.startsWith("{")) {
            val rootObj = runCatching { JSONObject(trimmed) }.getOrNull()
            val groupedResults = rootObj?.optJSONArray("results")
            if (rootObj != null && groupedResults != null && groupedResults.length() > 0) {
                val firstElem = groupedResults.optJSONObject(0)
                if (firstElem != null && firstElem.has("rows")) {
                    return parseGroupedMyChronoResults(rootObj, groupedResults)
                }
            }
        }

        val rows = mutableListOf<MyChronoRawResultRow>()
        val arraysToParse: List<Pair<String, JSONArray>> = extractResultArrays(trimmed)

        for ((sectionLabel, array) in arraysToParse) {
            val totalInArray = array.length()
            for (i in 0 until totalInArray) {
                val item = array.optJSONObject(i) ?: continue
                val runnerId = item.optString("runner_id")
                    .ifBlank { item.optString("runnerId") }
                    .ifBlank { item.optString("athlete_id") }
                    .ifBlank { item.optString("id") }

                val bibNumber = item.optString("bib")
                    .ifBlank { item.optString("dossard") }
                    .ifBlank { item.optString("numero") }
                    .ifBlank { item.optString("bib_number") }
                    .trim()

                val firstName = item.optString("first_name").ifBlank { item.optString("prenom") }
                val lastName = item.optString("last_name").ifBlank { item.optString("nom") }
                val fullName = item.optString("full_name")
                    .ifBlank { item.optString("name") }
                    .ifBlank { "$firstName $lastName".trim() }
                if (fullName.isBlank() && bibNumber.isBlank()) continue

                val club = item.optString("club").ifBlank { item.optString("team") }.ifBlank { item.optString("city") }
                val category = item.optString("category")
                    .ifBlank { item.optString("categorie") }
                    .ifBlank { item.optString("cat") }
                    .ifBlank { "S" }

                val rawCourseLabel = item.optString("distance_label")
                    .ifBlank { item.optString("course") }
                    .ifBlank { item.optString("parcours") }
                    .ifBlank { item.optString("race_name") }
                    .ifBlank { sectionLabel }

                val rawDistKm = item.optDouble("distance_km", item.optDouble("distance", Double.NaN))
                val isShort5km = isFiveKmCourse(rawCourseLabel, rawDistKm)
                val distanceKm = if (!rawDistKm.isNaN() && rawDistKm > 0.5) {
                    if (rawDistKm > 100.0) ((rawDistKm / 100.0).roundToInt() / 10.0) else rawDistKm
                } else if (isShort5km) {
                    5.0
                } else {
                    10.0
                }
                val canonicalDistanceLabel = if (isShort5km) "5 km" else "10 km"

                val rawTimeStr = item.optString("time")
                    .ifBlank { item.optString("temps") }
                    .ifBlank { item.optString("chrono") }
                val rawTimeSec = item.optInt("time_seconds", 0)
                val timeSec = if (rawTimeSec > 0) rawTimeSec else parseFormattedTimeToSeconds(rawTimeStr)
                if (timeSec <= 0) continue

                val rank = item.optInt(
                    "rank",
                    item.optInt("position", item.optInt("classement", i + 1))
                ).coerceAtLeast(1)
                val catRank = item.optInt(
                    "category_rank",
                    item.optInt("catRank", item.optInt("place_cat", max(1, rank / 3)))
                ).coerceAtLeast(1)
                val catTotal = item.optInt("category_total", max(catRank, totalInArray / 3))

                val explicitPoints = item.optInt("points", item.optInt("challenge_points", 0))
                val points = if (explicitPoints > 0) {
                    explicitPoints
                } else {
                    max(100, 1001 - rank)
                }

                rows.add(
                    MyChronoRawResultRow(
                        runnerId = runnerId,
                        bibNumber = bibNumber,
                        fullName = fullName,
                        club = club,
                        category = category,
                        distanceLabel = canonicalDistanceLabel,
                        distanceKm = distanceKm,
                        timeSeconds = timeSec,
                        timeFormatted = formatSecondsToMmSs(timeSec),
                        overallRank = rank,
                        totalFinishers = max(totalInArray, rank),
                        categoryRank = catRank,
                        categoryTotal = catTotal,
                        points = points
                    )
                )
            }
        }
        return rows
    }

    /**
     * Analyse le format groupé officiel de https://mychrono.softalux.com/api/public/races/{id}/results
     * où `race.distances` donne les distances en mètres (`effective` / `real` / `length`)
     * et `results` contient `[{ "distanceId": ..., "rows": [...] }]`.
     */
    private fun parseGroupedMyChronoResults(
        root: JSONObject,
        groupedResults: JSONArray
    ): List<MyChronoRawResultRow> {
        val raceObj = root.optJSONObject("race")
        val distanceLabelById = LinkedHashMap<Int, Pair<String, Double>>()
        val distArr = raceObj?.optJSONArray("distances") ?: root.optJSONArray("distances")

        if (distArr != null) {
            for (i in 0 until distArr.length()) {
                val d = distArr.optJSONObject(i) ?: continue
                val dId = d.optInt("id", -1)
                val rawLabel = d.optString("label", "")
                val kind = d.optString("kind", "adult").lowercase(Locale.ROOT)
                if (kind == "kids" || rawLabel.lowercase(Locale.ROOT).contains("enfant")) {
                    distanceLabelById[dId] = "Enfants" to 1.0
                    continue
                }
                val lengthMeters = d.optInt(
                    "effective",
                    d.optInt("real", d.optInt("length", 10000))
                )
                val km = if (lengthMeters > 100) {
                    ((lengthMeters / 100.0).roundToInt() / 10.0).coerceAtLeast(1.0)
                } else {
                    d.optDouble("distance", 10.0).coerceAtLeast(1.0)
                }
                val stdLabel = if (km < 7.2) "5 km" else "10 km"
                if (dId != -1) {
                    distanceLabelById[dId] = stdLabel to km
                }
            }
        }

        val rows = mutableListOf<MyChronoRawResultRow>()
        for (g in 0 until groupedResults.length()) {
            val group = groupedResults.optJSONObject(g) ?: continue
            val distanceId = group.optInt("distanceId", -1)
            val (stdLabel, distKm) = distanceLabelById[distanceId] ?: when (g) {
                0 -> "10 km" to 10.0
                1 -> "5 km" to 5.0
                else -> "Enfants" to 1.0
            }
            if (stdLabel == "Enfants") continue

            val groupRows = group.optJSONArray("rows") ?: continue
            val totalInDist = groupRows.length()
            for (rIdx in 0 until totalInDist) {
                val row = groupRows.optJSONObject(rIdx) ?: continue
                val rank = row.optInt("rank", rIdx + 1).coerceAtLeast(1)
                val bib = row.optInt("bib", 0).let {
                    if (it > 0) it.toString() else row.optString("bib", "")
                }
                val fullName = row.optString("name", "").trim()
                if (fullName.isEmpty() && bib.isEmpty()) continue

                val cat = row.optString("cat", "S").ifBlank { "S" }
                val catRank = row.optInt("catRank", 1).coerceAtLeast(1)
                val club = row.optString("club", "").ifBlank { row.optString("city", "Gaume") }

                val rawTimeVal = row.optLong("time", 0L)
                val timeSec = when {
                    rawTimeVal > 100_000L -> (rawTimeVal / 1000L).toInt()
                    rawTimeVal > 0L -> rawTimeVal.toInt()
                    else -> parseFormattedTimeToSeconds(row.optString("time", ""))
                }
                if (timeSec <= 0) continue

                val apiPoints = row.optInt("points", 0)
                val points = if (apiPoints > 0) apiPoints else max(100, 1001 - rank)
                val runnerIdNum = row.optInt("runnerId", 0)
                val runnerIdStr = if (runnerIdNum > 0) runnerIdNum.toString() else ""

                rows.add(
                    MyChronoRawResultRow(
                        runnerId = runnerIdStr,
                        bibNumber = bib,
                        fullName = fullName,
                        club = club,
                        category = cat,
                        distanceLabel = stdLabel,
                        distanceKm = distKm,
                        timeSeconds = timeSec,
                        timeFormatted = formatSecondsToMmSs(timeSec),
                        overallRank = rank,
                        totalFinishers = max(totalInDist, rank),
                        categoryRank = catRank,
                        categoryTotal = max(catRank, totalInDist / 3),
                        points = points
                    )
                )
            }
        }
        return rows
    }

    private fun extractResultArrays(trimmedJson: String): List<Pair<String, JSONArray>> {
        if (trimmedJson.startsWith("[")) {
            return listOf("" to JSONArray(trimmedJson))
        }
        if (!trimmedJson.startsWith("{")) return emptyList()

        val obj = JSONObject(trimmedJson)
        val out = mutableListOf<Pair<String, JSONArray>>()

        for (directKey in listOf("results", "data", "items", "classement", "rows")) {
            obj.optJSONArray(directKey)?.let { arr ->
                if (arr.length() > 0) out.add(directKey to arr)
            }
        }
        if (out.isNotEmpty()) return out

        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val childArr = obj.optJSONArray(k)
            if (childArr != null && childArr.length() > 0) {
                out.add(k to childArr)
            } else {
                val childObj = obj.optJSONObject(k)
                if (childObj != null) {
                    val nestedArr = childObj.optJSONArray("results")
                        ?: childObj.optJSONArray("data")
                        ?: childObj.optJSONArray("rows")
                    if (nestedArr != null && nestedArr.length() > 0) {
                        out.add(k to nestedArr)
                    }
                }
            }
        }
        return out
    }

    private fun isFiveKmCourse(label: String, rawDistKm: Double): Boolean {
        if (!rawDistKm.isNaN() && rawDistKm > 0.5) {
            val km = if (rawDistKm > 100.0) rawDistKm / 1000.0 else rawDistKm
            return km < 7.2
        }
        val lower = label.lowercase(Locale.ROOT)
        val firstNum = Regex("(\\d+(?:[.,]\\d+)?)")
            .find(lower)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
        if (firstNum != null) {
            val km = if (firstNum > 100.0) firstNum / 1000.0 else firstNum
            return km < 7.2
        }
        return lower.contains("court") || lower.contains("petit")
    }

    private fun normalizeBib(bib: String): String =
        bib.trim().removePrefix("#").trimStart('0').ifBlank { bib.trim().removePrefix("#") }

    private fun normalizeText(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(Locale.ROOT).trim(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("-", " ")
            .replace("'", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseFormattedTimeToSeconds(formatted: String): Int {
        val clean = formatted.trim()
            .replace("'", ":")
            .replace("h", ":")
            .replace("m", ":")
            .replace("s", "")
        val parts = clean.split(":").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    private fun formatSecondsToMmSs(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val min = (totalSeconds % 3600) / 60
        val sec = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, min, sec)
        } else {
            "%02d:%02d".format(min, sec)
        }
    }
}
