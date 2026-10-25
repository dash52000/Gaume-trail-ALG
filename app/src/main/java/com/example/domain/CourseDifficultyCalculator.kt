package com.example.domain

import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.RaceEntity
import com.example.data.remote.MyChronoApiClient
import com.example.data.remote.MyChronoRawResultRow
import kotlin.math.roundToInt

/**
 * Indice de difficulté des parcours, calculé uniquement à partir des résultats publiés par MyChrono.
 *
 * Principe : un coureur régulier (au moins [MIN_RACES_FOR_REGULAR] manches sur la distance) a une allure
 * habituelle. Sur un parcours donné, on regarde de combien les réguliers présents ont été plus lents
 * ou plus rapides que leur allure habituelle ; la médiane de ces écarts est l'indice du parcours.
 * Il intègre tout ce qui ralentit le peloton ce jour-là : dénivelé, terrain, météo, longueur réelle.
 *
 * L'allure habituelle et les indices sont affinés ensemble (quelques itérations), puis normalisés
 * pour qu'un parcours médian de la saison vaille 1,00.
 */
object CourseDifficultyCalculator {
    const val MIN_RACES_FOR_REGULAR = 5
    const val MIN_REGULARS_PER_RACE = 8
    private const val ITERATIONS = 4

    data class Factor(
        val raceId: String,
        val distanceLabel: String,
        val factor: Double,
        val regularsCount: Int
    )

    private data class Obs(val raceId: String, val runnerKey: String, val paceSecPerKm: Double)

    private fun runnerKey(row: MyChronoRawResultRow): String =
        row.runnerId.ifBlank { "${row.fullName.trim().lowercase()}|${row.bibNumber}" }

    fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    /**
     * @param rowsByRaceId résultats bruts de toutes les courses d'une saison (manches et hors challenge)
     * @param challengeRaceIds manches du challenge : elles définissent les réguliers et la normalisation
     */
    fun computeSeasonFactors(
        rowsByRaceId: Map<String, List<MyChronoRawResultRow>>,
        challengeRaceIds: Set<String>
    ): List<Factor> = listOf(MyChronoApiClient.LABEL_10KM, MyChronoApiClient.LABEL_5KM).flatMap { label ->
        computeForLabel(label, rowsByRaceId, challengeRaceIds)
    }

    private fun computeForLabel(
        label: String,
        rowsByRaceId: Map<String, List<MyChronoRawResultRow>>,
        challengeRaceIds: Set<String>
    ): List<Factor> {
        val observations = rowsByRaceId.flatMap { (raceId, rows) ->
            rows.filter { it.distanceLabel == label && it.distanceKm > 0.5 && it.timeSeconds > 0 }
                .distinctBy { runnerKey(it) }
                .map { Obs(raceId, runnerKey(it), it.timeSeconds / it.distanceKm) }
        }
        if (observations.isEmpty()) return emptyList()

        val challengeObs = observations.filter { it.raceId in challengeRaceIds }
        val regulars = challengeObs.groupBy { it.runnerKey }
            .filterValues { it.size >= MIN_RACES_FOR_REGULAR }
            .keys
        if (regulars.isEmpty()) return emptyList()

        val regularObs = observations.filter { it.runnerKey in regulars }
        val byRace = regularObs.groupBy { it.raceId }

        var reference: Map<String, Double> = challengeObs.filter { it.runnerKey in regulars }
            .groupBy { it.runnerKey }
            .mapValues { (_, list) -> median(list.map { it.paceSecPerKm })!! }

        var factors: Map<String, Pair<Double, Int>> = emptyMap()
        repeat(ITERATIONS) {
            val raw = byRace.mapNotNull { (raceId, list) ->
                val ratios = list.mapNotNull { o -> reference[o.runnerKey]?.let { o.paceSecPerKm / it } }
                if (ratios.size < MIN_REGULARS_PER_RACE) null
                else raceId to (median(ratios)!! to ratios.size)
            }.toMap()
            val norm = median(raw.filterKeys { it in challengeRaceIds }.values.map { it.first }) ?: return emptyList()
            factors = raw.mapValues { (_, v) -> (v.first / norm) to v.second }

            reference = regularObs
                .filter { it.raceId in challengeRaceIds && factors.containsKey(it.raceId) }
                .groupBy { it.runnerKey }
                .mapValues { (_, list) -> median(list.map { it.paceSecPerKm / factors.getValue(it.raceId).first })!! }
        }

        return factors.map { (raceId, v) ->
            Factor(raceId, label, (v.first * 1000.0).roundToInt() / 1000.0, v.second)
        }
    }

    /** « +8 % » / « −3 % » / « moyen » pour un indice. */
    fun formatFactor(factor: Double): String {
        val pct = ((factor - 1.0) * 100.0).roundToInt()
        return when {
            pct == 0 -> "parcours moyen"
            pct > 0 -> "+$pct % plus lent"
            else -> "${-pct} % plus rapide"
        }
    }

    /** Phrase complète : « 8 % plus lent qu'un parcours moyen de la saison ». */
    fun describeFactor(factor: Double): String {
        val pct = ((factor - 1.0) * 100.0).roundToInt()
        return when {
            pct == 0 -> "Dans la moyenne des parcours de la saison"
            pct > 0 -> "$pct % plus lent qu'un parcours moyen de la saison"
            else -> "${-pct} % plus rapide qu'un parcours moyen de la saison"
        }
    }

    /**
     * Clé d'un parcours d'une saison à l'autre : lieu + rang de la course dans ce lieu pendant la saison.
     * Virton accueille par exemple trois courses : la 1re de 2026 correspond à la 1re de 2025.
     * Les manches et les courses hors challenge sont appariées séparément.
     */
    fun courseKeys(races: List<RaceEntity>): Map<String, String> {
        val keys = HashMap<String, String>()
        races.groupBy { Triple(it.seasonYear, it.isChallenge, GaumeInitialSeedData.normalizeKey(it.village)) }
            .forEach { (k, list) ->
                list.sortedWith(compareBy<RaceEntity>({ it.dateIso }, { it.roundNumber }))
                    .forEachIndexed { idx, race ->
                        keys[race.raceId] = (if (k.second) "c:" else "h:") + k.third + "#" + (idx + 1)
                    }
            }
        return keys
    }

    /** Édition précédente la plus récente du même parcours (saison antérieure), ou null. */
    fun previousEdition(race: RaceEntity, allRaces: List<RaceEntity>, keys: Map<String, String>): RaceEntity? {
        val key = keys[race.raceId] ?: return null
        return allRaces
            .filter { it.seasonYear < race.seasonYear && keys[it.raceId] == key }
            .maxByOrNull { it.seasonYear }
    }
}
