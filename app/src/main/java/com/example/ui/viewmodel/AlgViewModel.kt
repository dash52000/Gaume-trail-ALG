package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityMetricsEntity
import com.example.data.local.MyChronoDirectoryEntry
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.local.RunnerProfileEntity
import com.example.data.repository.AlgRepository
import com.example.domain.AlgChallengeCalculator
import com.example.domain.RunnerChallengeSummary
import com.example.domain.SameCourseYearComparison
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AlgBottomTab {
    OVERVIEW, // 1. Courses
    PERFS, // 2. Pronostics, Records, Carte Bilan & Saisons
    SHARED_CALENDAR, // 3. Trio & Carte
    BADGES, // 4. Badges & Passeport
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
    val perfsSubTab: Int = 0, // 0 = Pronostics, 1 = Records, 2 = Carte Bilan & Export, 3 = Saisons
    val preselectedShareRaceId: String? = null,
    val selectedProfileId: String = "me",
    val selectedSeasonYear: Int = 2026,
    val availableSeasons: List<Int> = listOf(2026, 2027),
    val selectedChallengeDistanceKm: Int = 10, // 10 pour Challenge 10 km, 5 pour Challenge 5 km
    val raceFilterMode: RaceFilterMode = RaceFilterMode.ALL,
    val selectedRaceIdForDetail: String? = null,
    val showTop15SimulatorModal: Boolean = false,
    val profiles: List<RunnerProfileEntity> = emptyList(),
    val activeRunnerSummary: RunnerChallengeSummary? = null,
    val trioSeasonSummaries: Map<String, RunnerChallengeSummary> = emptyMap(),
    val multiSeasonSummariesForActiveRunner: Map<Int, RunnerChallengeSummary> = emptyMap(),
    val sameCourseComparisons2026Vs2027: List<SameCourseYearComparison> = emptyList(),
    val bibOverridesByRaceAndProfile: Map<String, Map<String, RaceBibOverrideEntity>> = emptyMap(),
    val terrainWeatherByRaceAndProfile: Map<String, Map<String, RaceTerrainWeatherEntity>> = emptyMap(),
    val directorySearchResults: List<MyChronoDirectoryEntry> = emptyList(),
    val directorySearchQuery: String = "",
    val statusBannerMessage: String? = null
)

private data class ControlState(
    val selectedTab: AlgBottomTab = AlgBottomTab.OVERVIEW,
    val perfsSubTab: Int = 0,
    val preselectedShareRaceId: String? = null,
    val selectedProfileId: String = "me",
    val selectedSeasonYear: Int = 2026,
    val selectedChallengeDistanceKm: Int = 10,
    val raceFilterMode: RaceFilterMode = RaceFilterMode.ALL,
    val selectedRaceIdForDetail: String? = null,
    val showTop15SimulatorModal: Boolean = false,
    val isSyncingMyChrono: Boolean = false,
    val directorySearchQuery: String = "",
    val statusBannerMessage: String? = null
)

private data class DataFlowsBundle(
    val profiles: List<RunnerProfileEntity>,
    val allRaces: List<RaceEntity>,
    val allResults: List<RaceResultEntity>,
    val allOverrides: List<RaceBibOverrideEntity>,
    val allMetrics: List<ActivityMetricsEntity>,
    val allTerrainWeather: List<RaceTerrainWeatherEntity>,
    val directory: List<MyChronoDirectoryEntry>
)

class AlgViewModel(
    private val repository: AlgRepository
) : ViewModel() {

    private val controlFlow = MutableStateFlow(ControlState())

    private val dataBundleFlow = combine(
        combine(
            repository.runnerProfilesFlow,
            repository.allRacesFlow,
            repository.allResultsFlow
        ) { profiles, races, results ->
            Triple(profiles, races, results)
        },
        combine(
            repository.allBibOverridesFlow,
            repository.allActivityMetricsFlow,
            repository.allTerrainWeatherFlow,
            repository.directoryFlow
        ) { overrides, metrics, terrainWeather, directory ->
            DataFlowsBundle(
                profiles = emptyList(),
                allRaces = emptyList(),
                allResults = emptyList(),
                allOverrides = overrides,
                allMetrics = metrics,
                allTerrainWeather = terrainWeather,
                directory = directory
            )
        }
    ) { (profiles, races, results), partial ->
        partial.copy(
            profiles = profiles,
            allRaces = races,
            allResults = results
        )
    }

    val uiState: StateFlow<AlgUiState> = combine(
        dataBundleFlow,
        controlFlow
    ) { bundle, ctrl ->
        val profiles = bundle.profiles
        val allRaces = bundle.allRaces.filter { it.seasonYear >= 2026 }
        val allResults = bundle.allResults.filter { it.seasonYear >= 2026 }
        val allOverrides = bundle.allOverrides
        val allMetrics = bundle.allMetrics
        val allTerrainWeather = bundle.allTerrainWeather
        val directory = bundle.directory

        val availableSeasons = (repository.getAvailableSeasonsFrom2026() + allRaces.map { it.seasonYear })
            .filter { it >= 2026 }
            .distinct()
            .sorted()

        val activeSeasonYear = if (ctrl.selectedSeasonYear in availableSeasons) {
            ctrl.selectedSeasonYear
        } else {
            availableSeasons.firstOrNull() ?: 2026
        }

        val activeProfile = profiles.find { it.profileId == ctrl.selectedProfileId }
            ?: profiles.firstOrNull()

        val seasonRaces = allRaces.filter { it.seasonYear == activeSeasonYear }
        val seasonResults = allResults.filter { it.seasonYear == activeSeasonYear }

        val trioSummaries = profiles.associate { profile ->
            profile.profileId to AlgChallengeCalculator.computeChallengeSummary(
                profile = profile,
                seasonYear = activeSeasonYear,
                racesInSeason = seasonRaces,
                allSeasonResults = seasonResults,
                allActivityMetrics = allMetrics,
                allTerrainWeather = allTerrainWeather,
                allBibOverrides = allOverrides,
                allProfiles = profiles
            )
        }

        val activeSummary = activeProfile?.let { trioSummaries[it.profileId] }

        val multiSeasonSummaries = if (activeProfile != null) {
            availableSeasons.associateWith { year ->
                AlgChallengeCalculator.computeChallengeSummary(
                    profile = activeProfile,
                    seasonYear = year,
                    racesInSeason = allRaces.filter { it.seasonYear == year },
                    allSeasonResults = allResults.filter { it.seasonYear == year },
                    allActivityMetrics = allMetrics,
                    allTerrainWeather = allTerrainWeather,
                    allBibOverrides = allOverrides,
                    allProfiles = profiles
                )
            }
        } else emptyMap()

        val activeSummaryForYearA = multiSeasonSummaries[2026]
        val activeSummaryForYearB = multiSeasonSummaries[2027]
        val validResultsForComparisons = buildList {
            if (activeSummaryForYearA != null) {
                activeSummaryForYearA.racesWithContext.forEach { item ->
                    item.result10km?.let { add(it) }
                    item.result5km?.let { add(it) }
                }
            }
            if (activeSummaryForYearB != null) {
                activeSummaryForYearB.racesWithContext.forEach { item ->
                    item.result10km?.let { add(it) }
                    item.result5km?.let { add(it) }
                }
            }
        }

        val comparisons2026Vs2027 = if (activeProfile != null) {
            AlgChallengeCalculator.buildSameCourseComparisons(
                profileId = activeProfile.profileId,
                distanceKm = ctrl.selectedChallengeDistanceKm,
                seasonA = 2026,
                seasonB = 2027,
                allRaces = allRaces,
                allResults = validResultsForComparisons
            )
        } else emptyList()

        val overridesByRaceAndProfile = allOverrides.groupBy { it.raceId }
            .mapValues { (_, list) -> list.associateBy { it.profileId } }

        val terrainWeatherByRaceAndProfile = allTerrainWeather.groupBy { it.raceId }
            .mapValues { (_, list) -> list.associateBy { it.profileId } }

        val filteredDirectory = if (ctrl.directorySearchQuery.isBlank()) {
            directory
        } else {
            val q = ctrl.directorySearchQuery.trim().lowercase()
            directory.filter {
                it.fullName.lowercase().contains(q) ||
                    it.club.lowercase().contains(q) ||
                    it.myChronoRunnerId.lowercase().contains(q) ||
                    it.defaultBib10km.lowercase().contains(q) ||
                    it.defaultBib5km.lowercase().contains(q)
            }
        }

        AlgUiState(
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
            sameCourseComparisons2026Vs2027 = comparisons2026Vs2027,
            bibOverridesByRaceAndProfile = overridesByRaceAndProfile,
            terrainWeatherByRaceAndProfile = terrainWeatherByRaceAndProfile,
            directorySearchResults = filteredDirectory,
            directorySearchQuery = ctrl.directorySearchQuery,
            statusBannerMessage = ctrl.statusBannerMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlgUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            syncMyChronoSeason(2026)
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

    fun openPerfsShareCardForRace(raceId: String) {
        openPerfsWithSubTab(subTab = 2, shareRaceId = raceId)
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
        val validYear = maxOf(2026, seasonYear)
        controlFlow.value = controlFlow.value.copy(selectedSeasonYear = validYear)
        val hasRacesForYear = uiState.value.activeRunnerSummary?.racesWithContext?.isNotEmpty() == true
        if (!hasRacesForYear) {
            syncMyChronoSeason(validYear)
        }
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

    fun syncMyChronoSeason(seasonYear: Int = controlFlow.value.selectedSeasonYear) {
        if (controlFlow.value.isSyncingMyChrono) return
        val targetYear = maxOf(2026, seasonYear)
        viewModelScope.launch {
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = true,
                statusBannerMessage = "Synchronisation MyChrono $targetYear (10 km & 5 km séparés)…"
            )
            val result = repository.syncSeasonFromMyChrono(targetYear)
            val msg = result.fold(
                onSuccess = { it },
                onFailure = { "MyChrono $targetYear indisponible : ${it.message}" }
            )
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = false,
                statusBannerMessage = msg
            )
        }
    }

    fun purgeAndResyncCleanResults() {
        if (controlFlow.value.isSyncingMyChrono) return
        val targetYear = maxOf(2026, controlFlow.value.selectedSeasonYear)
        viewModelScope.launch {
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = true,
                statusBannerMessage = "Purge des résultats sans dossard et re-synchronisation MyChrono $targetYear…"
            )
            val result = repository.resetAndResyncCleanResults(targetYear)
            val msg = result.fold(
                onSuccess = { "Données nettoyées (0 donnée fictive) • $it" },
                onFailure = { "Données nettoyées • ${it.message}" }
            )
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = false,
                statusBannerMessage = msg
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
                statusBannerMessage = "Enregistrement des dossards (10 km / 5 km) et mise à jour MyChrono…"
            )
            val res = repository.updateProfileBibAndDistance(
                profileId = profileId,
                newDisplayName = displayName,
                newMyChronoId = myChronoId,
                defaultBib10km = cleanBib10,
                defaultBib5km = cleanBib5,
                preferredDistanceKm = nextDist,
                clubName = clubName,
                categoryLabel = categoryLabel
            )
            val msg = res.fold(
                onSuccess = { "Dossards mis à jour • $it" },
                onFailure = { "Dossards enregistrés • ${it.message}" }
            )
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = false,
                statusBannerMessage = msg
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
                statusBannerMessage = "Recherche du dossard #$bibNumber ($distanceKm km) dans MyChrono…"
            )
            val res = repository.setRaceBibAndDistanceAndSync(
                raceId = raceId,
                profileId = profileId,
                bibNumber = bibNumber,
                distanceKm = distanceKm
            )
            val msg = res.fold(
                onSuccess = { it },
                onFailure = { it.message ?: "Aucun résultat trouvé pour ce dossard." }
            )
            controlFlow.value = controlFlow.value.copy(
                isSyncingMyChrono = false,
                statusBannerMessage = msg
            )
        }
    }

    fun saveRaceTerrainAndWeather(
        raceId: String,
        profileId: String,
        terrainCondition: String,
        weatherCondition: String,
        temperatureC: Int?,
        terrainNotes: String,
        isPlannedForSimulator: Boolean,
        plannedDistanceKm: Int,
        simulatedTargetPoints: Int?
    ) {
        viewModelScope.launch {
            repository.saveRaceTerrainWeather(
                raceId = raceId,
                profileId = profileId,
                seasonYear = controlFlow.value.selectedSeasonYear,
                weatherCondition = weatherCondition,
                temperatureC = temperatureC,
                terrainCondition = terrainCondition,
                isPlannedForSimulator = isPlannedForSimulator,
                plannedDistanceKm = plannedDistanceKm,
                simulatedTargetPoints = simulatedTargetPoints ?: 920,
                runnerNotes = terrainNotes
            )
            controlFlow.value = controlFlow.value.copy(
                statusBannerMessage = "Météo ($weatherCondition) & Terrain ($terrainCondition) enregistrés."
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
                seasonYear = controlFlow.value.selectedSeasonYear,
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

    fun importGpxForRace(context: Context, raceId: String, profileId: String, uri: Uri) {
        viewModelScope.launch {
            val res = repository.importGpxTrackForRace(context, raceId, profileId, uri)
            val msg = res.fold(
                onSuccess = { "Trace GPX « ${it.sourceTitle} » importée (${it.recordedDistanceKm} km, +${it.elevationGainMeters} m)" },
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
