package com.example.domain

import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceWeatherEntity
import com.example.data.local.SeasonGoalEntity
import kotlin.math.abs
import kotlin.math.roundToInt

/** Une édition d'un parcours courue par le coureur. */
data class CourseEdition(
    val seasonYear: Int,
    val raceId: String,
    val dateIso: String,
    val timeSeconds: Int,
    val timeFormatted: String,
    val distanceKm: Double,
    val paceSecPerKm: Int
)

enum class RecordStatus {
    /** Couru cette saison et meilleur que toutes les éditions précédentes. */
    BEATEN,
    /** Couru cette saison, sans battre la meilleure édition précédente. */
    NOT_BEATEN,
    /** Pas encore couru cette saison, une édition précédente existe : record à battre. */
    TO_BEAT,
    /** Une seule édition, rien à comparer pour l'instant. */
    FIRST
}

/** Record d'un parcours : meilleure édition, et duel de la saison contre les saisons précédentes. */
data class CourseRecord(
    val courseKey: String,
    val village: String,
    val title: String,
    val distanceLabel: String,
    val editions: List<CourseEdition>,
    val best: CourseEdition,
    /** Meilleure édition avant la saison regardée (la référence à battre). */
    val bestBefore: CourseEdition?,
    val status: RecordStatus,
    /** Écart de la saison regardée avec la référence (s), négatif = plus rapide. Allure × km si la longueur a changé. */
    val deltaSeconds: Int?,
    /** Course de la saison regardée sur ce parcours, pas encore courue. */
    val upcomingRaceId: String?,
    val upcomingDateIso: String?
)

data class RecordsSummary(
    val seasonYear: Int,
    val records: List<CourseRecord>,
    /** raceId d'une course de la saison regardée → son record de parcours. */
    val byRaceId: Map<String, CourseRecord>
) {
    val beaten: Int get() = records.count { it.status == RecordStatus.BEATEN }
    val notBeaten: Int get() = records.count { it.status == RecordStatus.NOT_BEATEN }
    val toBeat: Int get() = records.count { it.status == RecordStatus.TO_BEAT }
}

data class FactBadge(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val unlocked: Boolean,
    val progress: Int,
    val target: Int,
    /** Où et quand il a été obtenu (ou le meilleur avancement). */
    val detail: String?
)

data class TrioChallenge(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    /** null = compteur sans objectif. */
    val target: Int?,
    val unit: String,
    val detail: String?
)

/**
 * Gamification fondée uniquement sur des faits : résultats MyChrono, indice des parcours,
 * météo réelle (Open-Meteo) et calendrier publié. Aucun point d'expérience inventé.
 */
object ChallengesCalculator {

    private fun Double.fmt1() = "%.1f".format(this)

    private fun edition(race: RaceEntity, res: RaceResultEntity): CourseEdition? {
        val km = res.distanceKm?.takeIf { it > 0.5 } ?: race.realDistanceKmFor(if (res.distanceLabel == "5 km") 5 else 10)
            ?: return null
        return CourseEdition(
            seasonYear = race.seasonYear,
            raceId = race.raceId,
            dateIso = race.dateIso,
            timeSeconds = res.officialTimeSeconds,
            timeFormatted = res.officialTimeFormatted,
            distanceKm = km,
            paceSecPerKm = (res.officialTimeSeconds / km).roundToInt()
        )
    }

    /** a meilleure que b ? Au temps si la longueur est la même (±2 %), sinon à l'allure. */
    fun isBetter(a: CourseEdition, b: CourseEdition): Boolean =
        if (abs(a.distanceKm - b.distanceKm) / b.distanceKm <= 0.02) a.timeSeconds < b.timeSeconds
        else a.paceSecPerKm < b.paceSecPerKm

    private fun delta(a: CourseEdition, ref: CourseEdition): Int =
        if (abs(a.distanceKm - ref.distanceKm) / ref.distanceKm <= 0.02) a.timeSeconds - ref.timeSeconds
        else ((a.paceSecPerKm - ref.paceSecPerKm) * a.distanceKm).roundToInt()

    private fun bestOf(list: List<CourseEdition>): CourseEdition? =
        list.fold(null as CourseEdition?) { acc, e -> if (acc == null || isBetter(e, acc)) e else acc }

    /**
     * Records de parcours d'un coureur sur une distance du challenge, vus depuis une saison.
     * Un parcours = même lieu et même rang dans la saison (voir [CourseDifficultyCalculator.courseKeys]).
     */
    fun records(
        profileId: String,
        distanceLabel: String,
        seasonYear: Int,
        allRaces: List<RaceEntity>,
        allResults: List<RaceResultEntity>
    ): RecordsSummary {
        val races = allRaces.filter { it.isChallenge }
        val keys = CourseDifficultyCalculator.courseKeys(races)
        val raceById = races.associateBy { it.raceId }
        val mine = allResults.filter { it.profileId == profileId && it.distanceLabel == distanceLabel && it.raceId in raceById }

        val editionsByKey = mine.mapNotNull { res ->
            val race = raceById.getValue(res.raceId)
            val key = keys[race.raceId] ?: return@mapNotNull null
            edition(race, res)?.let { key to it }
        }.groupBy({ it.first }, { it.second })

        // Course de la saison regardée encore à courir, sinon de la saison suivante si elle est publiée.
        fun upcomingFor(key: String): RaceEntity? =
            races.filter { keys[it.raceId] == key && !it.isCompleted && it.seasonYear >= seasonYear }
                .minByOrNull { it.dateIso }

        val records = editionsByKey.mapNotNull { (key, editions) ->
            val sorted = editions.sortedBy { it.dateIso }
            val best = bestOf(sorted) ?: return@mapNotNull null
            val thisSeason = sorted.lastOrNull { it.seasonYear == seasonYear }
            val before = bestOf(sorted.filter { it.seasonYear < seasonYear })
            val upcoming = if (thisSeason == null) upcomingFor(key) else null
            val status = when {
                thisSeason != null && before != null -> if (isBetter(thisSeason, before)) RecordStatus.BEATEN else RecordStatus.NOT_BEATEN
                thisSeason == null && upcoming != null -> RecordStatus.TO_BEAT
                else -> RecordStatus.FIRST
            }
            val refRace = raceById.getValue((thisSeason ?: best).raceId)
            CourseRecord(
                courseKey = key,
                village = GaumeInitialSeedData.resolveGaumeGeoForRace(refRace).canonicalVillage,
                title = refRace.title,
                distanceLabel = distanceLabel,
                editions = sorted,
                best = best,
                bestBefore = before ?: if (thisSeason == null) best else null,
                status = status,
                deltaSeconds = if (thisSeason != null && before != null) delta(thisSeason, before) else null,
                upcomingRaceId = upcoming?.raceId,
                upcomingDateIso = upcoming?.dateIso
            )
        }.sortedWith(compareBy<CourseRecord>({ it.status.ordinal }, { it.upcomingDateIso ?: it.best.dateIso.substring(5) }))

        // Lien course de la saison → record : la course courue, ou la course à venir.
        val byRaceId = buildMap {
            records.forEach { r ->
                r.editions.filter { it.seasonYear == seasonYear }.forEach { put(it.raceId, r) }
                r.upcomingRaceId?.let { put(it, r) }
            }
        }
        return RecordsSummary(seasonYear, records, byRaceId)
    }

    /** Badges, toutes saisons confondues. */
    fun badges(
        profileId: String,
        trioProfileIds: List<String>,
        allRaces: List<RaceEntity>,
        allResults: List<RaceResultEntity>,
        courseFactors: Map<String, Map<String, Double>>,
        weatherByRaceId: Map<String, RaceWeatherEntity>,
        recordsBeatenEver: Int,
        villagesVisited: Int
    ): List<FactBadge> {
        val raceById = allRaces.associateBy { it.raceId }
        val mine = allResults.filter { it.profileId == profileId && it.raceId in raceById }
        val mineChallenge = mine.filter { raceById.getValue(it.raceId).isChallenge }
        val ranRaceIds = mine.map { it.raceId }.toSet()
        fun where(raceId: String): String {
            val r = raceById.getValue(raceId)
            return "${GaumeInitialSeedData.resolveGaumeGeoForRace(r).canonicalVillage} ${r.dateIso.substring(8, 10)}/${r.dateIso.substring(5, 7)}/${r.seasonYear}"
        }
        val seasons = allRaces.map { it.seasonYear }.distinct().sorted()
        val badges = mutableListOf<FactBadge>()

        // Challenge validé : 15 manches sur une même distance dans une saison.
        val best15 = seasons.flatMap { y ->
            listOf("10 km", "5 km").map { label -> Triple(y, label, mineChallenge.count { it.seasonYear == y && it.distanceLabel == label }) }
        }.maxByOrNull { it.third }
        badges += FactBadge(
            "challenge_valide", "Challenge validé", "15 manches sur une même distance dans une saison",
            "Challenge", (best15?.third ?: 0) >= 15, minOf(15, best15?.third ?: 0), 15,
            best15?.takeIf { it.third > 0 }?.let { "${it.second} ${it.first} : ${it.third} manches" }
        )

        // Doublé : 10 km et 5 km la même saison.
        val doubleSeason = seasons.firstOrNull { y ->
            mineChallenge.any { it.seasonYear == y && it.distanceLabel == "10 km" } &&
                mineChallenge.any { it.seasonYear == y && it.distanceLabel == "5 km" }
        }
        badges += FactBadge(
            "double", "Doublé", "Courir le 10 km et le 5 km du challenge la même saison", "Challenge",
            doubleSeason != null, if (doubleSeason != null) 2 else minOf(1, mineChallenge.size), 2,
            doubleSeason?.let { "Saison $it" }
        )

        // « Tout <village> » : toutes les manches d'un village qui en accueille plusieurs dans la saison.
        allRaces.filter { it.isChallenge }
            .groupBy { GaumeInitialSeedData.normalizeKey(it.village) }
            .forEach { (_, villageRaces) ->
                val bySeason = villageRaces.groupBy { it.seasonYear }.filterValues { it.size >= 2 }
                if (bySeason.isEmpty()) return@forEach
                val name = GaumeInitialSeedData.resolveGaumeGeoForRace(villageRaces.first()).canonicalVillage
                val best = bySeason.map { (y, list) -> Triple(y, list.size, list.count { it.raceId in ranRaceIds }) }
                    .maxWith(compareBy<Triple<Int, Int, Int>>({ it.third == it.second }, { it.third }))
                badges += FactBadge(
                    "tout_${GaumeInitialSeedData.normalizeKey(name).replace(' ', '_')}", "Tout $name",
                    "Courir toutes les manches de $name d'une même saison (${best.second} courses)", "Villages",
                    best.third == best.second, best.third, best.second, "Saison ${best.first} : ${best.third}/${best.second}"
                )
            }

        // Météo réelle.
        val rainy = mine.map { it.raceId }.distinct().filter { id ->
            weatherByRaceId[id]?.let { w ->
                (w.precipitationDuringMm ?: 0.0) >= 0.5 || w.weatherCode in setOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95)
            } == true
        }
        badges += FactBadge(
            "sous_la_flotte", "Sous la flotte", "Courir sous la pluie (météo réelle au départ)", "Météo",
            rainy.isNotEmpty(), minOf(1, rainy.size), 1,
            rainy.maxByOrNull { weatherByRaceId[it]?.precipitationDuringMm ?: 0.0 }?.let { id ->
                where(id) + (weatherByRaceId[id]?.precipitationDuringMm?.let { " • ${it.fmt1()} mm" } ?: "") +
                    if (rainy.size > 1) " (${rainy.size} courses)" else ""
            }
        )
        val muddy = mine.map { it.raceId }.distinct()
            .mapNotNull { id -> weatherByRaceId[id]?.rainPrevious72hMm?.let { id to it } }
            .filter { it.second >= 20.0 }
        badges += FactBadge(
            "apres_deluge", "Après le déluge", "Courir après au moins 20 mm de pluie sur les 3 jours précédents", "Météo",
            muddy.isNotEmpty(), minOf(1, muddy.size), 1,
            muddy.maxByOrNull { it.second }?.let { "${where(it.first)} • ${it.second.fmt1()} mm" }
        )
        val coldest = mine.map { it.raceId }.distinct()
            .mapNotNull { id -> weatherByRaceId[id]?.temperatureC?.let { id to it } }
            .minByOrNull { it.second }
        badges += FactBadge(
            "froid_de_canard", "Froid de canard", "Courir par 2 °C ou moins au départ", "Météo",
            (coldest?.second ?: 99.0) <= 2.0, if ((coldest?.second ?: 99.0) <= 2.0) 1 else 0, 1,
            coldest?.let { "Plus froide : ${where(it.first)} • ${it.second.fmt1()} °C" }
        )
        val hottest = mine.map { it.raceId }.distinct()
            .mapNotNull { id -> weatherByRaceId[id]?.temperatureC?.let { id to it } }
            .maxByOrNull { it.second }
        badges += FactBadge(
            "coup_de_chaud", "Coup de chaud", "Courir par 24 °C ou plus au départ", "Météo",
            (hottest?.second ?: -99.0) >= 24.0, if ((hottest?.second ?: -99.0) >= 24.0) 1 else 0, 1,
            hottest?.let { "Plus chaude : ${where(it.first)} • ${it.second.fmt1()} °C" }
        )

        // Mur de Gaume : les 3 parcours les plus durs d'une saison, sur la distance la plus courue.
        val mainLabel = mineChallenge.groupingBy { it.distanceLabel }.eachCount().maxByOrNull { it.value }?.key ?: "10 km"
        val wall = seasons.mapNotNull { y ->
            val hardest = allRaces.filter { it.isChallenge && it.seasonYear == y }
                .mapNotNull { r -> courseFactors[r.raceId]?.get(mainLabel)?.let { r to it } }
                .sortedByDescending { it.second }.take(3)
            if (hardest.size < 3) null
            else Triple(y, hardest, hardest.count { (r, _) -> mineChallenge.any { it.raceId == r.raceId && it.distanceLabel == mainLabel } })
        }.maxByOrNull { it.third }
        badges += FactBadge(
            "mur_de_gaume", "Mur de Gaume", "Finir les 3 parcours les plus durs d'une saison (indice du peloton, $mainLabel)", "Parcours",
            wall?.third == 3, wall?.third ?: 0, 3,
            wall?.let { w -> "Saison ${w.first} : " + w.second.joinToString(", ") { GaumeInitialSeedData.resolveGaumeGeoForRace(it.first).canonicalVillage } }
        )

        // Fidèle : le même parcours deux saisons de suite.
        val keys = CourseDifficultyCalculator.courseKeys(allRaces.filter { it.isChallenge })
        val seasonsByKey = mineChallenge.groupBy { keys[it.raceId] }.filterKeys { it != null }
            .mapValues { (_, l) -> l.map { it.seasonYear }.toSet() }
        val faithful = seasonsByKey.filterValues { ys -> ys.any { it + 1 in ys } }
        badges += FactBadge(
            "fidele", "Fidèle", "Courir le même parcours deux saisons de suite", "Parcours",
            faithful.isNotEmpty(), minOf(1, faithful.size), 1,
            if (faithful.isNotEmpty()) "${faithful.size} parcours" else null
        )
        badges += FactBadge(
            "record_battu", "Record battu", "Battre son record sur un parcours déjà couru", "Parcours",
            recordsBeatenEver > 0, minOf(1, recordsBeatenEver), 1,
            if (recordsBeatenEver > 0) "$recordsBeatenEver record(s) battu(s)" else null
        )

        // Hors piste : une course hors challenge.
        val hc = mine.filter { !raceById.getValue(it.raceId).isChallenge }
        badges += FactBadge(
            "hors_piste", "Hors piste", "Courir une course hors challenge", "Villages",
            hc.isNotEmpty(), minOf(1, hc.size), 1, hc.firstOrNull()?.let { where(it.raceId) }
        )

        // Le Trio au complet sur une même course.
        val trioRaces = allResults.filter { it.profileId in trioProfileIds }.groupBy { it.raceId }
            .filterValues { l -> l.map { it.profileId }.toSet().size >= trioProfileIds.size && trioProfileIds.size >= 3 }
            .keys.filter { it in ranRaceIds }
        badges += FactBadge(
            "trio_complet", "Le Trio au complet", "Courir une même course à trois", "Trio",
            trioRaces.isNotEmpty(), minOf(1, trioRaces.size), 1,
            if (trioRaces.isNotEmpty()) "${trioRaces.size} course(s) à trois" else null
        )

        // Dix mois sur dix : une course chaque mois du calendrier d'une saison.
        val months = seasons.mapNotNull { y ->
            val calendarMonths = allRaces.filter { it.isChallenge && it.seasonYear == y }.map { it.dateIso.substring(5, 7) }.toSet()
            if (calendarMonths.isEmpty()) null
            else Triple(y, calendarMonths.size, mine.filter { raceById.getValue(it.raceId).seasonYear == y }
                .map { raceById.getValue(it.raceId).dateIso.substring(5, 7) }.toSet().intersect(calendarMonths).size)
        }.maxByOrNull { it.third }
        badges += FactBadge(
            "tous_les_mois", "Tous les mois", "Courir au moins une fois chaque mois du calendrier d'une saison", "Régularité",
            months != null && months.third == months.second, months?.third ?: 0, months?.second ?: 10,
            months?.let { "Saison ${it.first} : ${it.third}/${it.second} mois" }
        )

        // Collectionneur de villages.
        listOf(10, 20, 30).forEach { n ->
            badges += FactBadge(
                "villages_$n", "$n villages", "Courir dans $n villages différents (toutes saisons)", "Villages",
                villagesVisited >= n, minOf(n, villagesVisited), n, "$villagesVisited village(s)"
            )
        }
        return badges
    }

    /** Défis coopératifs du Trio pour une saison. */
    fun trioChallenges(
        seasonYear: Int,
        trioProfileIds: List<String>,
        allRaces: List<RaceEntity>,
        allResults: List<RaceResultEntity>
    ): List<TrioChallenge> {
        val seasonRaces = allRaces.filter { it.seasonYear == seasonYear }
        val manches = seasonRaces.filter { it.isChallenge }
        val raceById = seasonRaces.associateBy { it.raceId }
        val trio = allResults.filter { it.profileId in trioProfileIds && it.raceId in raceById }
        val byRace = trio.groupBy { it.raceId }

        val covered = manches.count { byRace.containsKey(it.raceId) }
        val passedManches = manches.count { it.isCompleted }
        val uncoveredUpcoming = manches.filter { !it.isCompleted && !byRace.containsKey(it.raceId) }
        val together = byRace.count { (_, l) -> l.map { it.profileId }.toSet().size >= minOf(3, trioProfileIds.size) }
        val km = trio.sumOf { res ->
            res.distanceKm ?: raceById[res.raceId]?.realDistanceKmFor(if (res.distanceLabel == "5 km") 5 else 10) ?: 0.0
        }
        val villages = byRace.keys.mapNotNull { raceById[it] }
            .map { GaumeInitialSeedData.normalizeKey(GaumeInitialSeedData.resolveGaumeGeoForRace(it).canonicalVillage) }
            .toSet()
        val calendarVillages = manches.map { GaumeInitialSeedData.normalizeKey(it.village) }.toSet()

        return listOf(
            TrioChallenge(
                "couverture", "Toutes les manches couvertes",
                "Au moins l'un de vous trois au départ de chaque manche de la saison",
                covered, manches.size, "manches",
                if (uncoveredUpcoming.isNotEmpty()) "Pas encore couvertes : " + uncoveredUpcoming.joinToString(", ") { it.village }
                else if (passedManches > covered) "${passedManches - covered} manche(s) passée(s) sans le Trio" else null
            ),
            TrioChallenge(
                "a_trois", "Courses à trois", "Les trois au départ de la même course",
                together, null, "courses", null
            ),
            TrioChallenge(
                "km_trio", "Kilomètres du Trio", "Distance de course cumulée par les trois (manches et hors challenge)",
                km.roundToInt(), null, "km", null
            ),
            TrioChallenge(
                "villages_trio", "Villages du Trio", "Villages du calendrier où au moins l'un de vous a couru",
                villages.intersect(calendarVillages).size, calendarVillages.size, "villages", null
            )
        )
    }
}

/** Objectif de saison et son avancement. */
data class GoalProgress(
    val type: String,
    val title: String,
    val description: String,
    /** null = objectif pas encore fixé. */
    val target: Int?,
    val progress: Int,
    val unit: String,
    /** Valeur proposée pour fixer l'objectif, tirée de la saison précédente. */
    val suggested: Int
)

object GoalsCalculator {
    const val CHALLENGE = "challenge"
    const val RECORDS = "records"
    const val VILLAGES = "villages"
    const val RACES = "races"
    const val KM = "km"

    fun formatDelta(seconds: Int): String {
        val sign = if (seconds < 0) "−" else "+"
        val a = kotlin.math.abs(seconds)
        return if (a >= 3600) "$sign${a / 3600}:%02d:%02d".format((a % 3600) / 60, a % 60) else "$sign${a / 60}:%02d".format(a % 60)
    }

    private fun seasonStats(profileId: String, seasonYear: Int, allRaces: List<RaceEntity>, allResults: List<RaceResultEntity>): Pair<Int, Int> {
        val raceById = allRaces.filter { it.seasonYear == seasonYear }.associateBy { it.raceId }
        val mine = allResults.filter { it.profileId == profileId && it.raceId in raceById }
        val km = mine.sumOf { r ->
            r.distanceKm ?: raceById[r.raceId]?.realDistanceKmFor(if (r.distanceLabel == "5 km") 5 else 10) ?: 0.0
        }
        return mine.map { it.raceId }.distinct().size to km.roundToInt()
    }

    fun progress(
        profileId: String,
        goalSeason: Int,
        preferredDistanceLabel: String,
        goals: List<SeasonGoalEntity>,
        allRaces: List<RaceEntity>,
        allResults: List<RaceResultEntity>,
        recordsBeatenInSeason: Int,
        newVillagesInSeason: Int,
        coursesRunTwiceAvailable: Int
    ): List<GoalProgress> {
        val mine = goals.filter { it.profileId == profileId && it.seasonYear == goalSeason }.associateBy { it.goalType }
        val raceById = allRaces.associateBy { it.raceId }
        val counted = allResults.count {
            it.profileId == profileId && it.seasonYear == goalSeason && it.distanceLabel == preferredDistanceLabel &&
                raceById[it.raceId]?.isChallenge == true
        }.coerceAtMost(15)
        val (races, km) = seasonStats(profileId, goalSeason, allRaces, allResults)
        val (prevRaces, prevKm) = seasonStats(profileId, goalSeason - 1, allRaces, allResults)

        return listOf(
            GoalProgress(CHALLENGE, "Valider le challenge $preferredDistanceLabel", "15 manches comptées sur ta distance",
                mine[CHALLENGE]?.target, counted, "manches", 15),
            GoalProgress(RECORDS, "Battre mes records de parcours", "Parcours déjà courus où tu fais mieux qu'avant",
                mine[RECORDS]?.target, recordsBeatenInSeason, "records", maxOf(1, coursesRunTwiceAvailable / 2)),
            GoalProgress(VILLAGES, "Découvrir de nouveaux villages", "Villages où tu n'as jamais couru",
                mine[VILLAGES]?.target, newVillagesInSeason, "villages", 3),
            GoalProgress(RACES, "Nombre de courses", "Manches et courses hors challenge",
                mine[RACES]?.target, races, "courses", maxOf(15, prevRaces)),
            GoalProgress(KM, "Kilomètres de course", "Distance cumulée en course",
                mine[KM]?.target, km, "km", maxOf(100, (prevKm / 10) * 10))
        )
    }
}
