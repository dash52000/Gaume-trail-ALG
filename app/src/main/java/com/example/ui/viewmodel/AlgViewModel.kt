package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityMetricsEntity
import com.example.data.local.ChallengeStandingEntity
import com.example.data.local.CourseFactorEntity
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.MyChronoDirectoryEntry
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.local.RaceWeatherEntity
import com.example.data.local.RunnerProfileEntity
import com.example.data.local.SeasonBibEntity
import com.example.data.local.SeasonGoalEntity
import com.example.data.local.VillageInfoEntity
import com.example.domain.ChallengesCalculator
import com.example.domain.FactBadge
import com.example.domain.GoalProgress
import com.example.domain.GoalsCalculator
import com.example.domain.RecordsSummary
import com.example.domain.TrioChallenge
import com.example.data.repository.AlgRepository
import com.example.data.repository.SyncDiagnostics
import com.example.domain.AlgChallengeCalculator
import com.example.domain.ChallengeStandingsCalculator
import com.example.domain.CourseDifficultyCalculator
import com.example.domain.ProfileChallengeStanding
import com.example.domain.RaceItemWithContext
import com.example.domain.RunnerChallengeSummary
import com.example.domain.SameCourseYearComparison
import com.example.domain.SeasonInsightsCalculator
import com.example.domain.SeasonRecap
import com.example.domain.VillagePassport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AlgBottomTab {
    OVERVIEW, // 1. Courses
    PERFS, // 2. Forme, Records, Bilan & Saisons
    SHARED_CALENDAR, // 3. Trio & Carte
    BADGES, // 4. Passeport des villages
    SETTINGS, // 5. Réglages
    MULTI_SEASON
}

enum class RaceFilterMode {
    ALL,
    COMPLETED,
    UPCOMING,
    TOP_15_ONLY
}

data class AlgUiState(
    val isLoading: Boolean = true,
    val isSyncingMyChrono: Boolean = false,
    val selectedTab: AlgBottomTab = AlgBottomTab.OVERVIEW,
    val perfsSubTab: Int = 0, // 0 = Forme, 1 = Records, 2 = Bilan & Carte, 3 = Saisons
    val preselectedShareRaceId: String? = null,
    val selectedProfileId: String = "me",
    val selectedSeasonYear: Int = LocalDate.now().year,
    val availableSeasons: List<Int> = listOf(LocalDate.now().year),
    val selectedChallengeDistanceKm: Int = 10, // 10 pour Challenge 10 km, 5 pour Challenge 5 km
    val raceFilterMode: RaceFilterMode = RaceFilterMode.ALL,
    val selectedRaceIdForDetail: String? = null,
    val showTop15SimulatorModal: Boolean = false,
    /** Profils avec les dossards de la saison affichée. */
    val profiles: List<RunnerProfileEntity> = emptyList(),
    val activeRunnerSummary: RunnerChallengeSummary? = null,
    val trioSeasonSummaries: Map<String, RunnerChallengeSummary> = emptyMap(),
    val multiSeasonSummariesForActiveRunner: Map<Int, RunnerChallengeSummary> = emptyMap(),
    val sameCourseComparisons: List<SameCourseYearComparison> = emptyList(),
    val comparisonYearA: Int = LocalDate.now().year - 1,
    val comparisonYearB: Int = LocalDate.now().year,
    val bibOverridesByRaceAndProfile: Map<String, Map<String, RaceBibOverrideEntity>> = emptyMap(),
    val terrainWeatherByRaceAndProfile: Map<String, Map<String, RaceTerrainWeatherEntity>> = emptyMap(),
    val directorySearchResults: List<MyChronoDirectoryEntry> = emptyList(),
    val directorySearchQuery: String = "",
    val statusBannerMessage: String? = null,
    /** Classement provisoire du Challenge : profileId → (distance 10 ou 5 → classement). */
    val trioStandings: Map<String, Map<Int, ProfileChallengeStanding>> = emptyMap(),
    /** Courses hors challenge de la saison (résultats récupérés, jamais comptés pour le challenge). */
    val horsChallengeItems: List<RaceItemWithContext> = emptyList(),
    /** Passeport des villages du profil actif, toutes saisons confondues. */
    val activePassport: VillagePassport? = null,
    /** Bilan de la saison affichée pour chaque profil (sur sa distance du challenge). */
    val trioRecaps: Map<String, SeasonRecap> = emptyMap(),
    /** Bilan du profil actif sur la distance choisie. */
    val activeRecap: SeasonRecap? = null,
    /** profileId → true si les dossards de la saison affichée ont été saisis. */
    val seasonBibsDeclared: Map<String, Boolean> = emptyMap(),
    /** Ce que MyChrono publie sous l'identifiant de chaque profil, et ce qui est en mémoire. */
    val diagnostics: SyncDiagnostics = SyncDiagnostics(),
    /** profileId → saison → distance → nombre de manches retrouvées par l'appli. */
    val foundResultCounts: Map<String, Map<Int, Map<String, Int>>> = emptyMap(),
    /** Records de parcours du profil actif sur la distance choisie, vus depuis la saison affichée. */
    val records: RecordsSummary? = null,
    val badges: List<FactBadge> = emptyList(),
    val trioChallenges: List<TrioChallenge> = emptyList(),
    /** Saison des objectifs : la saison affichée, ou la suivante si elle est terminée. */
    val goalSeason: Int = LocalDate.now().year,
    val goals: List<GoalProgress> = emptyList(),
    /** Clé de village normalisée → fiche Wikipédia. */
    val villageInfos: Map<String, VillageInfoEntity> = emptyMap(),
    val villageMapPoints: List<com.example.domain.VillageMapPoint> = emptyList()
)

private data class ControlState(
    val selectedTab: AlgBottomTab = AlgBottomTab.OVERVIEW,
    val perfsSubTab: Int = 0,
    val preselectedShareRaceId: String? = null,
    val selectedProfileId: String = "me",
    /** null = saison choisie automatiquement (la plus récente qui a déjà commencé). */
    val selectedSeasonYear: Int? = null,
    val selectedChallengeDistanceKm: Int = 10,
    val raceFilterMode: RaceFilterMode = RaceFilterMode.ALL,
    val selectedRaceIdForDetail: String? = null,
    val showTop15SimulatorModal: Boolean = false,
    val isSyncingMyChrono: Boolean = false,
    val directorySearchQuery: String = "",
    val statusBannerMessage: String? = null
)

private data class DataFlowsBundle(
    val profiles: List<RunnerProfileEntity> = emptyList(),
    val allRaces: List<RaceEntity> = emptyList(),
    val allResults: List<RaceResultEntity> = emptyList(),
    val allOverrides: List<RaceBibOverrideEntity> = emptyList(),
    val allMetrics: List<ActivityMetricsEntity> = emptyList(),
    val allTerrainWeather: List<RaceTerrainWeatherEntity> = emptyList(),
    val directory: List<MyChronoDirectoryEntry> = emptyList(),
    val standings: List<ChallengeStandingEntity> = emptyList(),
    val seasonBibs: List<SeasonBibEntity> = emptyList(),
    val courseFactors: List<CourseFactorEntity> = emptyList(),
    val raceWeather: List<RaceWeatherEntity> = emptyList(),
    val diagnostics: SyncDiagnostics = SyncDiagnostics(),
    val seasonGoals: List<SeasonGoalEntity> = emptyList(),
    val villageInfos: List<VillageInfoEntity> = emptyList()
)

class AlgViewModel(
    private val repository: AlgRepository
) : ViewModel() {

    private val controlFlow = MutableStateFlow(ControlState())

    private val dataBundleFlow = combine(
        combine(
            repository.runnerProfilesFlow,
            repository.allRacesFlow,
            repository.allResultsFlow,
            repository.allBibOverridesFlow
        ) { profiles, races, results, overrides ->
            DataFlowsBundle(profiles = profiles, allRaces = races, allResults = results, allOverrides = overrides)
        },
        combine(
            repository.allActivityMetricsFlow,
            repository.allTerrainWeatherFlow,
            repository.directoryFlow,
            repository.standingsFlow
        ) { metrics, terrainWeather, directory, standings ->
            DataFlowsBundle(allMetrics = metrics, allTerrainWeather = terrainWeather, directory = directory, standings = standings)
        },
        combine(
            repository.seasonBibsFlow,
            repository.courseFactorsFlow,
            repository.raceWeatherFlow,
            repository.diagnosticsFlow
        ) { bibs, factors, weather, diagnostics ->
            DataFlowsBundle(seasonBibs = bibs, courseFactors = factors, raceWeather = weather, diagnostics = diagnostics)
        },
        combine(
            repository.seasonGoalsFlow,
            repository.villageInfoFlow
        ) { goals, infos ->
            DataFlowsBundle(seasonGoals = goals, villageInfos = infos)
        }
    ) { a, b, c, d ->
        a.copy(
            allMetrics = b.allMetrics,
            allTerrainWeather = b.allTerrainWeather,
            directory = b.directory,
            standings = b.standings,
            seasonBibs = c.seasonBibs,
            courseFactors = c.courseFactors,
            raceWeather = c.raceWeather,
            diagnostics = c.diagnostics,
            seasonGoals = d.seasonGoals,
            villageInfos = d.villageInfos
        )
    }

    /** Ce qui oblige à tout recalculer : la saison, le profil et la distance affichés. */
    private data class ComputeKey(val seasonYear: Int?, val profileId: String, val distanceKm: Int)

    /**
     * Calculs lourds (bilans, records, badges, passeports…) : refaits seulement quand les données changent
     * (après une synchro, une saisie) ou quand saison, profil ou distance changent — pas à chaque tap.
     * Les écritures en rafale d'une synchro sont regroupées.
     */
    @OptIn(FlowPreview::class)
    private val computedFlow = combine(
        dataBundleFlow.debounce(150),
        controlFlow.map { ComputeKey(it.selectedSeasonYear, it.selectedProfileId, it.selectedChallengeDistanceKm) }
            .distinctUntilChanged()
    ) { bundle, key ->
        bundle to buildUiState(
            bundle,
            ControlState(
                selectedSeasonYear = key.seasonYear,
                selectedProfileId = key.profileId,
                selectedChallengeDistanceKm = key.distanceKm
            )
        )
    }.flowOn(Dispatchers.Default)

    /** État de l'écran : les calculs ci-dessus + l'état des contrôles (onglet, filtres, fiche ouverte…). */
    val uiState: StateFlow<AlgUiState> = combine(computedFlow, controlFlow) { (bundle, computed), ctrl ->
        computed.copy(
            isSyncingMyChrono = ctrl.isSyncingMyChrono,
            selectedTab = ctrl.selectedTab,
            perfsSubTab = ctrl.perfsSubTab,
            preselectedShareRaceId = ctrl.preselectedShareRaceId,
            raceFilterMode = ctrl.raceFilterMode,
            selectedRaceIdForDetail = ctrl.selectedRaceIdForDetail,
            showTop15SimulatorModal = ctrl.showTop15SimulatorModal,
            directorySearchQuery = ctrl.directorySearchQuery,
            directorySearchResults = filterDirectory(bundle.directory, ctrl.directorySearchQuery),
            statusBannerMessage = ctrl.statusBannerMessage
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlgUiState()
        )

    /** Annuaire MyChrono : rien tant qu'on n'a pas tapé au moins 2 caractères (il compte des milliers de coureurs). */
    private fun filterDirectory(directory: List<MyChronoDirectoryEntry>, query: String): List<MyChronoDirectoryEntry> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return directory.filter {
            it.fullName.lowercase().contains(q) ||
                it.club.lowercase().contains(q) ||
                it.myChronoRunnerId.lowercase().contains(q) ||
                it.defaultBib10km.lowercase().contains(q) ||
                it.defaultBib5km.lowercase().contains(q)
        }.take(100)
    }

    /**
     * Dossards d'un profil pour une saison, distance par distance : celui saisi, sinon celui porté
     * dans ses résultats MyChrono de la saison (retrouvés par son identifiant).
     * Le booléen indique si des dossards ont été saisis pour cette saison.
     */
    private fun profileForSeason(
        profile: RunnerProfileEntity,
        seasonYear: Int,
        seasonBibs: Map<Pair<String, Int>, SeasonBibEntity>,
        challengeResults: List<RaceResultEntity>
    ): Pair<RunnerProfileEntity, Boolean> {
        val sb = seasonBibs[profile.profileId to seasonYear]
        val declared = sb != null && (sb.bib10km.isNotBlank() || sb.bib5km.isNotBlank())
        fun bibFromResults(label: String): String {
            val results = challengeResults.filter {
                it.profileId == profile.profileId && it.seasonYear == seasonYear && it.distanceLabel == label
            }
            if (results.isEmpty()) return ""
            return results.groupingBy { it.bibNumber }.eachCount().maxByOrNull { it.value }?.key
                ?.ifBlank { null } ?: "—"
        }
        return profile.copy(
            defaultBib10km = sb?.bib10km?.takeIf { it.isNotBlank() } ?: bibFromResults("10 km"),
            defaultBib5km = sb?.bib5km?.takeIf { it.isNotBlank() } ?: bibFromResults("5 km")
        ) to declared
    }

    private fun buildUiState(bundle: DataFlowsBundle, ctrl: ControlState): AlgUiState {
        val today = LocalDate.now().toString()
        val allRaces = bundle.allRaces.filter { it.seasonYear >= AlgRepository.FIRST_SEASON }
        val horsIds = allRaces.filter { !it.isChallenge }.map { it.raceId }.toSet()
        val challengeRaces = allRaces.filter { it.isChallenge }
        val challengeResults = bundle.allResults.filter { it.raceId !in horsIds }
        val horsResults = bundle.allResults.filter { it.raceId in horsIds }
        val allOverrides = bundle.allOverrides
        val allMetrics = bundle.allMetrics
        val allTerrainWeather = bundle.allTerrainWeather

        val availableSeasons = (repository.getAvailableSeasons() + allRaces.map { it.seasonYear })
            .filter { it >= AlgRepository.FIRST_SEASON }
            .distinct()
            .sorted()

        val autoSeason = challengeRaces.filter { it.dateIso <= today }.maxByOrNull { it.dateIso }?.seasonYear
            ?: availableSeasons.lastOrNull() ?: LocalDate.now().year
        val activeSeasonYear = ctrl.selectedSeasonYear?.takeIf { it in availableSeasons } ?: autoSeason

        val seasonBibs = bundle.seasonBibs.associateBy { it.profileId to it.seasonYear }
        val factorsByRace: Map<String, Map<String, Double>> = bundle.courseFactors
            .groupBy { it.raceId }
            .mapValues { (_, list) -> list.associate { it.distanceLabel to it.factor } }
        val courseKeys = CourseDifficultyCalculator.courseKeys(allRaces)
        val previousEditionByRaceId = allRaces.mapNotNull { race ->
            CourseDifficultyCalculator.previousEdition(race, allRaces, courseKeys)?.let { race.raceId to it.raceId }
        }.toMap()
        val weatherByRaceId = bundle.raceWeather.associateBy { it.raceId }

        fun summaryFor(profile: RunnerProfileEntity, year: Int): RunnerChallengeSummary {
            val (seasonProfile, _) = profileForSeason(profile, year, seasonBibs, challengeResults)
            val seasonProfiles = bundle.profiles.map { profileForSeason(it, year, seasonBibs, challengeResults).first }
            return AlgChallengeCalculator.computeChallengeSummary(
                profile = seasonProfile,
                seasonYear = year,
                racesInSeason = challengeRaces.filter { it.seasonYear == year },
                allSeasonResults = challengeResults.filter { it.seasonYear == year },
                allActivityMetrics = allMetrics,
                allTerrainWeather = allTerrainWeather,
                allBibOverrides = allOverrides,
                allProfiles = seasonProfiles,
                courseFactors = factorsByRace,
                previousEditionByRaceId = previousEditionByRaceId,
                weatherByRaceId = weatherByRaceId
            )
        }

        val summariesByProfileAndYear: Map<String, Map<Int, RunnerChallengeSummary>> =
            bundle.profiles.associate { profile ->
                profile.profileId to availableSeasons.associateWith { year -> summaryFor(profile, year) }
            }

        fun withFallbackForm(summary: RunnerChallengeSummary): RunnerChallengeSummary {
            val previous = summariesByProfileAndYear[summary.profile.profileId]?.get(summary.seasonYear - 1)
                ?: return summary
            return summary.copy(
                fallbackFormPace10km = SeasonInsightsCalculator.currentFormPace(SeasonInsightsCalculator.formPoints(previous, 10)),
                fallbackFormPace5km = SeasonInsightsCalculator.currentFormPace(SeasonInsightsCalculator.formPoints(previous, 5))
            )
        }

        val seasonProfilesWithFlag = bundle.profiles.map { profileForSeason(it, activeSeasonYear, seasonBibs, challengeResults) }
        val profiles = seasonProfilesWithFlag.map { it.first }
        val bibsDeclared = seasonProfilesWithFlag.associate { it.first.profileId to it.second }
        val activeProfile = profiles.find { it.profileId == ctrl.selectedProfileId } ?: profiles.firstOrNull()

        val trioSummaries = profiles.associate { profile ->
            profile.profileId to withFallbackForm(summariesByProfileAndYear.getValue(profile.profileId).getValue(activeSeasonYear))
        }
        val activeSummary = activeProfile?.let { trioSummaries[it.profileId] }

        val trioStandings = profiles.associate { profile ->
            profile.profileId to listOf(10, 5).mapNotNull { dist ->
                ChallengeStandingsCalculator.standingForProfile(
                    profile = profile,
                    seasonYear = activeSeasonYear,
                    distanceKm = dist,
                    allStandings = bundle.standings
                )?.let { dist to it }
            }.toMap()
        }

        val multiSeasonSummaries = activeProfile?.let { summariesByProfileAndYear[it.profileId] }.orEmpty()

        // Comparaison avec la saison précédente, parcours par parcours.
        val yearB = activeSeasonYear
        val yearA = activeSeasonYear - 1
        val comparisons = if (activeProfile != null) {
            val valid = listOfNotNull(multiSeasonSummaries[yearA], multiSeasonSummaries[yearB])
                .flatMap { s -> s.racesWithContext.flatMap { listOfNotNull(it.result10km, it.result5km) } }
            AlgChallengeCalculator.buildSameCourseComparisons(
                profileId = activeProfile.profileId,
                distanceKm = ctrl.selectedChallengeDistanceKm,
                seasonA = yearA,
                seasonB = yearB,
                allRaces = challengeRaces,
                allResults = valid
            )
        } else emptyList()

        // Courses hors challenge de la saison, avec les résultats du Trio (ne comptent pas pour le challenge).
        val horsItems = allRaces
            .filter { !it.isChallenge && it.seasonYear == activeSeasonYear }
            .sortedBy { it.dateIso }
            .map { race ->
                val forRace = horsResults.filter { it.raceId == race.raceId }
                val mine = forRace.filter { it.profileId == activeProfile?.profileId }
                RaceItemWithContext(
                    race = race,
                    gaumeGeo = GaumeInitialSeedData.resolveGaumeGeoForRace(race),
                    result10km = mine.firstOrNull { it.distanceLabel == "10 km" },
                    isCountingInTop15For10km = false,
                    rankAmongRunnerRaces10km = null,
                    pointsDeltaToEnterTop15For10km = null,
                    result5km = mine.firstOrNull { it.distanceLabel == "5 km" },
                    isCountingInTop15For5km = false,
                    rankAmongRunnerRaces5km = null,
                    pointsDeltaToEnterTop15For5km = null,
                    resultsByProfileAndDistance = forRace.groupBy { it.profileId },
                    activityMetrics = allMetrics.firstOrNull { it.raceId == race.raceId && it.profileId == activeProfile?.profileId },
                    terrainWeather = allTerrainWeather.firstOrNull { it.raceId == race.raceId && it.profileId == activeProfile?.profileId },
                    courseFactor10km = factorsByRace[race.raceId]?.get("10 km"),
                    courseFactor5km = factorsByRace[race.raceId]?.get("5 km"),
                    previousEditionRaceId = previousEditionByRaceId[race.raceId],
                    weather = weatherByRaceId[race.raceId]
                )
            }

        val passports = profiles.associate { p ->
            p.profileId to SeasonInsightsCalculator.buildPassport(p.profileId, allRaces, bundle.allResults)
        }
        fun horsCount(profileId: String): Int =
            horsItems.count { item -> item.resultsByProfileAndDistance[profileId].orEmpty().isNotEmpty() }

        val trioRecaps = profiles.mapNotNull { p ->
            val summary = trioSummaries[p.profileId] ?: return@mapNotNull null
            val dist = when {
                summary.challengeForDistance(p.preferredDistanceKm).completedRacesCount > 0 -> p.preferredDistanceKm
                summary.challenge10km.completedRacesCount >= summary.challenge5km.completedRacesCount -> 10
                else -> 5
            }
            p.profileId to SeasonInsightsCalculator.buildRecap(
                summary = summary,
                distanceKm = dist,
                standing = trioStandings[p.profileId]?.get(dist),
                passport = passports.getValue(p.profileId),
                horsChallengeRaces = horsCount(p.profileId)
            )
        }.toMap()
        val activeRecap = activeSummary?.let { s ->
            SeasonInsightsCalculator.buildRecap(
                summary = s,
                distanceKm = ctrl.selectedChallengeDistanceKm,
                standing = trioStandings[s.profile.profileId]?.get(ctrl.selectedChallengeDistanceKm),
                passport = passports.getValue(s.profile.profileId),
                horsChallengeRaces = horsCount(s.profile.profileId)
            )
        }

        // Gamification fondée sur les faits : records de parcours, badges, défis du Trio, objectifs.
        val selectedLabel = if (ctrl.selectedChallengeDistanceKm == 5) "5 km" else "10 km"
        val records = activeProfile?.let {
            ChallengesCalculator.records(it.profileId, selectedLabel, activeSeasonYear, allRaces, challengeResults)
        }
        fun beatenIn(profileId: String, year: Int): Int = listOf("10 km", "5 km").sumOf { l ->
            ChallengesCalculator.records(profileId, l, year, allRaces, challengeResults).beaten
        }
        val activePassportValue = activeProfile?.let { passports[it.profileId] }
        val badges = activeProfile?.let { p ->
            ChallengesCalculator.badges(
                profileId = p.profileId,
                trioProfileIds = profiles.map { it.profileId },
                allRaces = allRaces,
                allResults = bundle.allResults,
                courseFactors = factorsByRace,
                weatherByRaceId = weatherByRaceId,
                recordsBeatenEver = availableSeasons.sumOf { y -> beatenIn(p.profileId, y) },
                villagesVisited = activePassportValue?.visited?.size ?: 0
            )
        }.orEmpty()
        val trioChallenges = ChallengesCalculator.trioChallenges(
            seasonYear = activeSeasonYear,
            trioProfileIds = profiles.map { it.profileId },
            allRaces = allRaces,
            allResults = bundle.allResults
        )
        val goalSeason = if (activeSummary != null && SeasonInsightsCalculator.isSeasonOver(activeSummary)) {
            activeSeasonYear + 1
        } else activeSeasonYear
        val goals = activeProfile?.let { p ->
            val preferredLabel = if (p.preferredDistanceKm == 5) "5 km" else "10 km"
            val lastSeasonRecords = ChallengesCalculator.records(p.profileId, preferredLabel, goalSeason, allRaces, challengeResults)
            GoalsCalculator.progress(
                profileId = p.profileId,
                goalSeason = goalSeason,
                preferredDistanceLabel = preferredLabel,
                goals = bundle.seasonGoals,
                allRaces = allRaces,
                allResults = bundle.allResults,
                recordsBeatenInSeason = beatenIn(p.profileId, goalSeason),
                newVillagesInSeason = activePassportValue?.visited?.count { it.firstSeason == goalSeason } ?: 0,
                coursesRunTwiceAvailable = lastSeasonRecords.records.count { it.status == com.example.domain.RecordStatus.TO_BEAT }
            )
        }.orEmpty()

        val overridesByRaceAndProfile = allOverrides.groupBy { it.raceId }
            .mapValues { (_, list) -> list.associateBy { it.profileId } }
        val terrainWeatherByRaceAndProfile = allTerrainWeather.groupBy { it.raceId }
            .mapValues { (_, list) -> list.associateBy { it.profileId } }

        return AlgUiState(
            isLoading = profiles.isEmpty(),
            isSyncingMyChrono = ctrl.isSyncingMyChrono,
            selectedTab = ctrl.selectedTab,
            perfsSubTab = ctrl.perfsSubTab,
            preselectedShareRaceId = ctrl.preselectedShareRaceId,
            selectedProfileId = activeProfile?.profileId ?: "me",
            selectedSeasonYear = activeSeasonYear,
            availableSeasons = availableSeasons,
            selectedChallengeDistanceKm = ctrl.selectedChallengeDistanceKm,
            raceFilterMode = ctrl.raceFilterMode,
            selectedRaceIdForDetail = ctrl.selectedRaceIdForDetail,
            showTop15SimulatorModal = ctrl.showTop15SimulatorModal,
            profiles = profiles,
            activeRunnerSummary = activeSummary,
            trioSeasonSummaries = trioSummaries,
            multiSeasonSummariesForActiveRunner = multiSeasonSummaries,
            sameCourseComparisons = comparisons,
            comparisonYearA = yearA,
            comparisonYearB = yearB,
            bibOverridesByRaceAndProfile = overridesByRaceAndProfile,
            terrainWeatherByRaceAndProfile = terrainWeatherByRaceAndProfile,
            directorySearchResults = emptyList(),
            directorySearchQuery = ctrl.directorySearchQuery,
            statusBannerMessage = ctrl.statusBannerMessage,
            trioStandings = trioStandings,
            horsChallengeItems = horsItems,
            activePassport = activeProfile?.let { passports[it.profileId] },
            trioRecaps = trioRecaps,
            activeRecap = activeRecap,
            seasonBibsDeclared = bibsDeclared,
            diagnostics = bundle.diagnostics,
            foundResultCounts = challengeResults.groupBy { it.profileId }.mapValues { (_, l) ->
                l.groupBy { it.seasonYear }.mapValues { (_, ls) -> ls.groupingBy { it.distanceLabel }.eachCount() }
            },
            records = records,
            badges = badges,
            trioChallenges = trioChallenges,
            goalSeason = goalSeason,
            goals = goals,
            villageInfos = bundle.villageInfos.associateBy { it.villageKey },
            villageMapPoints = activeProfile?.let {
                com.example.domain.VillageMapCalculator.points(it.profileId, allRaces, bundle.allResults)
            }.orEmpty()
        )
    }

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.refreshDiagnostics()
            syncMyChronoSeason()
        }
    }

    fun selectTab(tab: AlgBottomTab) {
        controlFlow.value = controlFlow.value.copy(selectedTab = tab)
    }

    fun selectPerfsSubTab(subTab: Int) {
        controlFlow.value = controlFlow.value.copy(
            selectedTab = AlgBottomTab.PERFS,
            perfsSubTab = subTab.coerceIn(0, 3)
        )
    }

    fun openPerfsWithSubTab(subTab: Int, shareRaceId: String? = null) {
        controlFlow.value = controlFlow.value.copy(
            selectedRaceIdForDetail = null,
            selectedTab = AlgBottomTab.PERFS,
            perfsSubTab = subTab.coerceIn(0, 3),
            preselectedShareRaceId = shareRaceId ?: controlFlow.value.preselectedShareRaceId
        )
    }

    fun selectShareRaceId(raceId: String) {
        controlFlow.value = controlFlow.value.copy(preselectedShareRaceId = raceId)
    }

    fun selectRunnerProfile(profileId: String) {
        val profile = uiState.value.profiles.find { it.profileId == profileId }
        val preferredDist = profile?.preferredDistanceKm ?: controlFlow.value.selectedChallengeDistanceKm
        controlFlow.value = controlFlow.value.copy(
            selectedProfileId = profileId,
            selectedChallengeDistanceKm = preferredDist
        )
    }

    fun selectSeasonYear(seasonYear: Int) {
        val validYear = maxOf(AlgRepository.FIRST_SEASON, seasonYear)
        controlFlow.value = controlFlow.value.copy(selectedSeasonYear = validYear)
        val hasRacesForYear = uiState.value.availableSeasons.contains(validYear) &&
            uiState.value.multiSeasonSummariesForActiveRunner[validYear]?.racesWithContext?.isNotEmpty() == true
        if (!hasRacesForYear) syncMyChronoSeason()
    }

    fun selectChallengeDistance(distanceKm: Int) {
        val cleanDist = if (distanceKm == 5) 5 else 10
        controlFlow.value = controlFlow.value.copy(selectedChallengeDistanceKm = cleanDist)
    }

    fun selectRaceFilterMode(mode: RaceFilterMode) {
        controlFlow.value = controlFlow.value.copy(raceFilterMode = mode)
    }

    fun openRaceDetail(raceId: String) {
        controlFlow.value = controlFlow.value.copy(selectedRaceIdForDetail = raceId)
    }

    fun closeRaceDetail() {
        controlFlow.value = controlFlow.value.copy(selectedRaceIdForDetail = null)
    }

    fun setTop15SimulatorModalVisible(visible: Boolean) {
        controlFlow.value = controlFlow.value.copy(showTop15SimulatorModal = visible)
    }

    fun clearStatusBanner() {
        controlFlow.value = controlFlow.value.copy(statusBannerMessage = null)
    }

    fun updateDirectorySearchQuery(query: String) {
        controlFlow.value = controlFlow.value.copy(directorySearchQuery = query)
    }

    /** Synchronise toutes les saisons (seules les courses nouvelles ou récentes sont téléchargées). */
    fun syncMyChronoSeason(@Suppress("UNUSED_PARAMETER") seasonYear: Int? = null) {
        if (controlFlow.value.isSyncingMyChrono) return
        viewModelScope.launch {
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = true,
                statusBannerMessage = "Synchronisation MyChrono…"
            )
            val result = repository.syncAll()
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = false,
                statusBannerMessage = result.fold(
                    onSuccess = { it },
                    onFailure = { "MyChrono indisponible : ${it.message}" }
                )
            )
        }
    }

    fun purgeAndResyncCleanResults() {
        if (controlFlow.value.isSyncingMyChrono) return
        viewModelScope.launch {
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = true,
                statusBannerMessage = "Téléchargement complet des classements MyChrono…"
            )
            val result = repository.resetAndResyncCleanResults()
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = false,
                statusBannerMessage = result.fold(
                    onSuccess = { "Tout est recalculé • $it" },
                    onFailure = { "MyChrono indisponible : ${it.message}" }
                )
            )
        }
    }

    fun updateRunnerProfileBibAndDistance(
        profileId: String,
        displayName: String,
        myChronoId: String,
        defaultBib10km: String,
        defaultBib5km: String,
        preferredDistanceKm: Int,
        clubName: String,
        categoryLabel: String
    ) {
        val season = uiState.value.selectedSeasonYear
        viewModelScope.launch {
            val cleanBib10 = defaultBib10km.trim().removePrefix("#")
            val cleanBib5 = defaultBib5km.trim().removePrefix("#")
            val nextDist = when {
                preferredDistanceKm == 5 && cleanBib5.isNotBlank() -> 5
                preferredDistanceKm == 10 && cleanBib10.isNotBlank() -> 10
                cleanBib5.isNotBlank() && cleanBib10.isBlank() -> 5
                cleanBib10.isNotBlank() && cleanBib5.isBlank() -> 10
                preferredDistanceKm == 5 -> 5
                else -> 10
            }
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = true,
                selectedChallengeDistanceKm = if (controlFlow.value.selectedProfileId == profileId) nextDist else controlFlow.value.selectedChallengeDistanceKm,
                statusBannerMessage = "Enregistrement des dossards $season…"
            )
            val res = repository.updateProfileBibAndDistance(
                profileId = profileId,
                seasonYear = season,
                newDisplayName = displayName,
                newMyChronoId = myChronoId,
                defaultBib10km = cleanBib10,
                defaultBib5km = cleanBib5,
                preferredDistanceKm = nextDist,
                clubName = clubName,
                categoryLabel = categoryLabel
            )
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = false,
                statusBannerMessage = res.fold(
                    onSuccess = { "Dossards $season enregistrés • $it" },
                    onFailure = { "Dossards $season enregistrés • ${it.message}" }
                )
            )
        }
    }

    fun saveRaceBibAndDistanceOverride(
        raceId: String,
        profileId: String,
        bibNumber: String,
        distanceKm: Int
    ) {
        viewModelScope.launch {
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = true,
                selectedChallengeDistanceKm = if (distanceKm == 5) 5 else 10,
                statusBannerMessage = "Recherche du dossard #$bibNumber dans MyChrono…"
            )
            val res = repository.setRaceBibAndDistanceAndSync(
                raceId = raceId,
                profileId = profileId,
                bibNumber = bibNumber,
                distanceKm = distanceKm
            )
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = false,
                statusBannerMessage = res.fold(
                    onSuccess = { it },
                    onFailure = { it.message ?: "Aucun résultat trouvé pour ce dossard." }
                )
            )
        }
    }

    fun saveRaceTerrainAndWeather(
        raceId: String,
        profileId: String,
        terrainCondition: String,
        terrainNotes: String,
        isPlannedForSimulator: Boolean,
        plannedDistanceKm: Int,
        simulatedTargetPoints: Int?
    ) {
        viewModelScope.launch {
            repository.saveRaceTerrainWeather(
                raceId = raceId,
                profileId = profileId,
                terrainCondition = terrainCondition,
                isPlannedForSimulator = isPlannedForSimulator,
                plannedDistanceKm = plannedDistanceKm,
                simulatedTargetPoints = simulatedTargetPoints,
                runnerNotes = terrainNotes
            )
            controlFlow.value = controlFlow.value.copy(
                statusBannerMessage = if (terrainCondition.isBlank()) "Notes enregistrées." else "Terrain « $terrainCondition » enregistré."
            )
        }
    }

    fun togglePlannedSimulatorRace(
        raceId: String,
        profileId: String,
        isPlanned: Boolean,
        distanceKm: Int,
        targetPoints: Int
    ) {
        viewModelScope.launch {
            repository.togglePlannedRaceForSimulator(
                raceId = raceId,
                profileId = profileId,
                distanceKm = distanceKm,
                isPlanned = isPlanned,
                defaultTargetPoints = targetPoints
            )
        }
    }

    fun linkProfileToDirectoryEntry(profileId: String, entry: MyChronoDirectoryEntry) {
        viewModelScope.launch {
            val existing = uiState.value.profiles.find { it.profileId == profileId }
            val prefDist = when {
                entry.defaultBib5km.isNotBlank() && entry.defaultBib10km.isBlank() -> 5
                entry.defaultBib10km.isNotBlank() && entry.defaultBib5km.isBlank() -> 10
                else -> existing?.preferredDistanceKm ?: 10
            }
            updateRunnerProfileBibAndDistance(
                profileId = profileId,
                displayName = entry.fullName,
                myChronoId = entry.myChronoRunnerId,
                defaultBib10km = entry.defaultBib10km,
                defaultBib5km = entry.defaultBib5km,
                preferredDistanceKm = prefDist,
                clubName = entry.club,
                categoryLabel = entry.category
            )
        }
    }

    fun setSeasonGoal(goalType: String, target: Int) {
        val state = uiState.value
        viewModelScope.launch {
            repository.setSeasonGoal(state.selectedProfileId, state.goalSeason, goalType, target)
        }
    }

    fun loadVillageInfo(village: String) {
        viewModelScope.launch { repository.loadVillageInfo(village) }
    }

    fun exportUserData(context: Context, uri: Uri) {
        viewModelScope.launch {
            val res = repository.exportUserDataTo(context, uri)
            controlFlow.value = controlFlow.value.copy(
                statusBannerMessage = res.fold(onSuccess = { it }, onFailure = { "Sauvegarde impossible : ${it.message}" })
            )
        }
    }

    fun importUserData(context: Context, uri: Uri) {
        viewModelScope.launch {
            val res = repository.importUserDataFrom(context, uri)
            controlFlow.value = controlFlow.value.copy(
                statusBannerMessage = res.fold(onSuccess = { it }, onFailure = { "Restauration impossible : ${it.message}" })
            )
        }
    }

    fun importGpxForRace(context: Context, raceId: String, profileId: String, uri: Uri) {
        viewModelScope.launch {
            val res = repository.importGpxTrackForRace(context, raceId, profileId, uri)
            val msg = res.fold(
                onSuccess = {
                    val dPlus = it.elevationGainMeters?.let { d -> ", +$d m" } ?: ", sans altitude"
                    "Trace GPX « ${it.sourceTitle} » importée (${it.recordedDistanceKm} km$dPlus)"
                },
                onFailure = { it.message ?: "Erreur lecture GPX" }
            )
            controlFlow.value = controlFlow.value.copy(statusBannerMessage = msg)
        }
    }

    class Factory(private val repository: AlgRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlgViewModel(repository) as T
        }
    }
}
