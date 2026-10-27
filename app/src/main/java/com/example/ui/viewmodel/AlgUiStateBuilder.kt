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

/**
 * Calcul de l'état des écrans à partir des données (sans réseau ni base) : bilans, records, badges,
 * classements… Utilisé par [AlgViewModel] et par l'export des données de la version web.
 * [baseSeasons] : saisons proposées par le dépôt (de 2025 à l'année en cours).
 */
internal object AlgUiStateBuilder {

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

    fun buildUiState(bundle: DataFlowsBundle, ctrl: ControlState, baseSeasons: List<Int>): AlgUiState {
        val today = LocalDate.now().toString()
        val allRaces = bundle.allRaces.filter { it.seasonYear >= AlgRepository.FIRST_SEASON }
        val horsIds = allRaces.filter { !it.isChallenge }.map { it.raceId }.toSet()
        val challengeRaces = allRaces.filter { it.isChallenge }
        val challengeResults = bundle.allResults.filter { it.raceId !in horsIds }
        val horsResults = bundle.allResults.filter { it.raceId in horsIds }
        val allOverrides = bundle.allOverrides
        val allMetrics = bundle.allMetrics
        val allTerrainWeather = bundle.allTerrainWeather

        val availableSeasons = (baseSeasons + allRaces.map { it.seasonYear })
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
}
