package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AlgDatabase
import com.example.data.repository.AlgRepository
import com.example.ui.components.RunnerAndSeasonHeaderBar
import com.example.ui.components.Top15SimulatorModalDialog
import com.example.ui.screens.MultiSeasonProgressionScreen
import com.example.ui.screens.RaceDetailModalScreen
import com.example.ui.screens.RunnerOverviewScreen
import com.example.ui.screens.SettingsAndAssociationScreen
import com.example.ui.screens.SharedCalendarScreen
import com.example.ui.theme.GaumeTrailAlgTheme
import com.example.ui.viewmodel.AlgBottomTab
import com.example.ui.viewmodel.AlgUiState
import com.example.ui.viewmodel.AlgViewModel

class MainActivity : ComponentActivity() {

    private lateinit var algViewModel: AlgViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AlgDatabase.getInstance(applicationContext)
        val repository = AlgRepository(
            database.algDao(),
            database = database,
            syncPrefs = applicationContext.getSharedPreferences("mychrono_sync", MODE_PRIVATE)
        )
        algViewModel = ViewModelProvider(
            this,
            AlgViewModel.Factory(repository)
        )[AlgViewModel::class.java]

        val prefs = getSharedPreferences("alg_gaume_prefs", Context.MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("dark_theme_enabled", false))
            }
            GaumeTrailAlgTheme(darkTheme = isDarkTheme) {
                val uiState by algViewModel.uiState.collectAsStateWithLifecycle()
                GaumeTrailAlgAppContent(
                    uiState = uiState,
                    viewModel = algViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = {
                        val next = !isDarkTheme
                        isDarkTheme = next
                        prefs.edit().putBoolean("dark_theme_enabled", next).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun GaumeTrailAlgAppContent(
    uiState: AlgUiState,
    viewModel: AlgViewModel,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {}
) {
    val context = LocalContext.current

    val selectedRaceItem = uiState.selectedRaceIdForDetail?.let { detailId ->
        uiState.activeRunnerSummary?.racesWithContext?.find { it.race.raceId == detailId }
            ?: uiState.horsChallengeItems.find { it.race.raceId == detailId }
            ?: uiState.multiSeasonSummariesForActiveRunner.values
                .firstNotNullOfOrNull { s -> s.racesWithContext.find { it.race.raceId == detailId } }
    }

    if (selectedRaceItem != null) {
        val activeProfile = uiState.profiles.find { it.profileId == uiState.selectedProfileId }
        val overridesForRace = uiState.bibOverridesByRaceAndProfile[selectedRaceItem.race.raceId].orEmpty()

        RaceDetailModalScreen(
            raceItem = selectedRaceItem,
            activeProfile = activeProfile,
            profiles = uiState.profiles,
            trioSummaries = uiState.trioSeasonSummaries,
            bibOverridesForRace = overridesForRace,
            selectedDistanceKm = uiState.selectedChallengeDistanceKm,
            isSyncingMyChrono = uiState.isSyncingMyChrono,
            onClose = { viewModel.closeRaceDetail() },
            onOpenShareCardForRace = { raceId ->
                viewModel.closeRaceDetail()
                viewModel.openPerfsWithSubTab(subTab = 2, shareRaceId = raceId)
            },
            onSaveRaceBibAndDistance = { profileId, bib, distKm ->
                viewModel.saveRaceBibAndDistanceOverride(
                    raceId = selectedRaceItem.race.raceId,
                    profileId = profileId,
                    bibNumber = bib,
                    distanceKm = distKm
                )
            },
            onSaveRaceTerrainAndWeather = { profileId, terrain, notes, isPlanned, plannedDistKm, targetPts ->
                viewModel.saveRaceTerrainAndWeather(
                    raceId = selectedRaceItem.race.raceId,
                    profileId = profileId,
                    terrainCondition = terrain,
                    terrainNotes = notes,
                    isPlannedForSimulator = isPlanned,
                    plannedDistanceKm = plannedDistKm,
                    simulatedTargetPoints = targetPts
                )
            },
            onSelectRunnerProfile = { viewModel.selectRunnerProfile(it) },
            onImportGpxFile = { uri ->
                viewModel.importGpxForRace(
                    context = context,
                    raceId = selectedRaceItem.race.raceId,
                    profileId = uiState.selectedProfileId,
                    uri = uri
                )
            },
            courseRecord = uiState.records?.byRaceId?.get(selectedRaceItem.race.raceId)
        )
    } else {
        BackHandler(enabled = uiState.selectedTab != AlgBottomTab.OVERVIEW) {
            viewModel.selectTab(AlgBottomTab.OVERVIEW)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column {
                    RunnerAndSeasonHeaderBar(
                        profiles = uiState.profiles,
                        selectedProfileId = uiState.selectedProfileId,
                        selectedSeasonYear = uiState.selectedSeasonYear,
                        availableSeasons = uiState.availableSeasons,
                        isSyncing = uiState.isSyncingMyChrono,
                        onSelectProfile = { viewModel.selectRunnerProfile(it) },
                        onSelectSeasonYear = { viewModel.selectSeasonYear(it) },
                        onTriggerSync = { viewModel.syncMyChronoSeason(uiState.selectedSeasonYear) },
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme
                    )

                    AnimatedVisibility(visible = !uiState.statusBannerMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.clearStatusBanner() }
                                .testTag("status_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.statusBannerMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearStatusBanner() },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fermer le message",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // BARRE DE NAVIGATION À 5 ONGLETS : [Courses] [Perfs] [Trio & Carte] [Défis] [Réglages]
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == AlgBottomTab.OVERVIEW,
                        onClick = { viewModel.selectTab(AlgBottomTab.OVERVIEW) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "Courses"
                            )
                        },
                        label = {
                            Text(
                                text = "Courses",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_overview")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == AlgBottomTab.PERFS || uiState.selectedTab == AlgBottomTab.MULTI_SEASON,
                        onClick = { viewModel.selectTab(AlgBottomTab.PERFS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Perfs"
                            )
                        },
                        label = {
                            Text(
                                text = "Perfs",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_perfs")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == AlgBottomTab.SHARED_CALENDAR,
                        onClick = { viewModel.selectTab(AlgBottomTab.SHARED_CALENDAR) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Trio & Carte"
                            )
                        },
                        label = {
                            Text(
                                text = "Trio & Carte",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_calendar")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == AlgBottomTab.BADGES,
                        onClick = { viewModel.selectTab(AlgBottomTab.BADGES) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Défis"
                            )
                        },
                        label = {
                            Text(
                                text = "Défis",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_badges")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == AlgBottomTab.SETTINGS,
                        onClick = { viewModel.selectTab(AlgBottomTab.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Réglages"
                            )
                        },
                        label = {
                            Text(
                                text = "Réglages",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.selectedTab) {
                    AlgBottomTab.OVERVIEW,
                    AlgBottomTab.PERFS,
                    AlgBottomTab.BADGES -> {
                        RunnerOverviewScreen(
                            summary = uiState.activeRunnerSummary,
                            selectedDistanceKm = uiState.selectedChallengeDistanceKm,
                            raceFilterMode = uiState.raceFilterMode,
                            isSyncingMyChrono = uiState.isSyncingMyChrono,
                            activeBottomTab = uiState.selectedTab,
                            perfsSubTab = uiState.perfsSubTab,
                            preselectedShareRaceId = uiState.preselectedShareRaceId,
                            multiSeasonSummaries = uiState.multiSeasonSummariesForActiveRunner,
                            sameCourseComparisons = uiState.sameCourseComparisons,
                            comparisonYearA = uiState.comparisonYearA,
                            comparisonYearB = uiState.comparisonYearB,
                            trioStandings = uiState.trioStandings,
                            profiles = uiState.profiles,
                            horsChallengeItems = uiState.horsChallengeItems,
                            passport = uiState.activePassport,
                            activeRecap = uiState.activeRecap,
                            trioRecaps = uiState.trioRecaps,
                            records = uiState.records,
                            badges = uiState.badges,
                            trioChallenges = uiState.trioChallenges,
                            goalSeason = uiState.goalSeason,
                            goals = uiState.goals,
                            villageMapPoints = uiState.villageMapPoints,
                            villageInfos = uiState.villageInfos,
                            onSetGoal = { type, target -> viewModel.setSeasonGoal(type, target) },
                            onRequestVillageInfo = { viewModel.loadVillageInfo(it) },
                            onSelectDistanceKm = { viewModel.selectChallengeDistance(it) },
                            onSelectFilterMode = { viewModel.selectRaceFilterMode(it) },
                            onOpenRaceDetail = { viewModel.openRaceDetail(it) },
                            onOpenTop15Simulator = { viewModel.setTop15SimulatorModalVisible(true) },
                            onOpenPerfsTabWithSubTab = { subTab, shareRaceId ->
                                viewModel.openPerfsWithSubTab(subTab, shareRaceId)
                            },
                            onSelectPerfsSubTab = { viewModel.selectPerfsSubTab(it) },
                            onSelectShareRaceId = { viewModel.selectShareRaceId(it) },
                            onSelectSeasonYear = { year ->
                                viewModel.selectSeasonYear(year)
                                viewModel.selectTab(AlgBottomTab.OVERVIEW)
                            },
                            onTogglePlannedSimulatorRace = { raceId, isPlanned, targetPts ->
                                viewModel.togglePlannedSimulatorRace(
                                    raceId = raceId,
                                    profileId = uiState.selectedProfileId,
                                    isPlanned = isPlanned,
                                    distanceKm = uiState.selectedChallengeDistanceKm,
                                    targetPoints = targetPts
                                )
                            },
                            onQuickUpdateTerrain = { raceId, terrainCond ->
                                val existingTw = uiState.terrainWeatherByRaceAndProfile[raceId]?.get(uiState.selectedProfileId)
                                viewModel.saveRaceTerrainAndWeather(
                                    raceId = raceId,
                                    profileId = uiState.selectedProfileId,
                                    terrainCondition = terrainCond,
                                    terrainNotes = existingTw?.terrainNotes.orEmpty(),
                                    isPlannedForSimulator = existingTw?.isPlannedForSimulator ?: false,
                                    plannedDistanceKm = existingTw?.plannedDistanceKm ?: uiState.selectedChallengeDistanceKm,
                                    simulatedTargetPoints = existingTw?.simulatedTargetPoints
                                )
                            },
                            onTriggerMyChronoSync = { viewModel.syncMyChronoSeason(uiState.selectedSeasonYear) },
                            onUpdateRunnerBibAndDistance = { profileId, name, myChronoId, bib10, bib5, prefDist, club, cat ->
                                viewModel.updateRunnerProfileBibAndDistance(
                                    profileId = profileId,
                                    displayName = name,
                                    myChronoId = myChronoId,
                                    defaultBib10km = bib10,
                                    defaultBib5km = bib5,
                                    preferredDistanceKm = prefDist,
                                    clubName = club,
                                    categoryLabel = cat
                                )
                            }
                        )
                    }

                    AlgBottomTab.SHARED_CALENDAR -> {
                        SharedCalendarScreen(
                            seasonYear = uiState.selectedSeasonYear,
                            selectedDistanceKm = uiState.selectedChallengeDistanceKm,
                            profiles = uiState.profiles,
                            trioSummaries = uiState.trioSeasonSummaries,
                            horsChallengeItems = uiState.horsChallengeItems,
                            onOpenRaceDetail = { viewModel.openRaceDetail(it) },
                            onOpenTop15Simulator = { viewModel.setTop15SimulatorModalVisible(true) },
                            onTogglePlannedSimulatorRace = { raceId, isPlanned, targetPts ->
                                viewModel.togglePlannedSimulatorRace(
                                    raceId = raceId,
                                    profileId = uiState.selectedProfileId,
                                    isPlanned = isPlanned,
                                    distanceKm = uiState.selectedChallengeDistanceKm,
                                    targetPoints = targetPts
                                )
                            },
                            onQuickUpdateTerrain = { raceId, terrainCond ->
                                val existingTw = uiState.terrainWeatherByRaceAndProfile[raceId]?.get(uiState.selectedProfileId)
                                viewModel.saveRaceTerrainAndWeather(
                                    raceId = raceId,
                                    profileId = uiState.selectedProfileId,
                                    terrainCondition = terrainCond,
                                    terrainNotes = existingTw?.terrainNotes.orEmpty(),
                                    isPlannedForSimulator = existingTw?.isPlannedForSimulator ?: false,
                                    plannedDistanceKm = existingTw?.plannedDistanceKm ?: uiState.selectedChallengeDistanceKm,
                                    simulatedTargetPoints = existingTw?.simulatedTargetPoints
                                )
                            }
                        )
                    }

                    AlgBottomTab.MULTI_SEASON -> {
                        MultiSeasonProgressionScreen(
                            activeRunnerName = uiState.activeRunnerSummary?.profile?.displayName ?: "Moi",
                            selectedDistanceKm = uiState.selectedChallengeDistanceKm,
                            multiSeasonSummaries = uiState.multiSeasonSummariesForActiveRunner,
                            sameCourseComparisons = uiState.sameCourseComparisons,
                            comparisonYearA = uiState.comparisonYearA,
                            comparisonYearB = uiState.comparisonYearB,
                            onSelectDistanceKm = { viewModel.selectChallengeDistance(it) },
                            onSelectSeasonYear = { year ->
                                viewModel.selectSeasonYear(year)
                                viewModel.selectTab(AlgBottomTab.OVERVIEW)
                            }
                        )
                    }

                    AlgBottomTab.SETTINGS -> {
                        SettingsAndAssociationScreen(
                            profiles = uiState.profiles,
                            selectedSeasonYear = uiState.selectedSeasonYear,
                            directorySearchQuery = uiState.directorySearchQuery,
                            directoryResults = uiState.directorySearchResults,
                            isSyncingMyChrono = uiState.isSyncingMyChrono,
                            onUpdateSearchQuery = { viewModel.updateDirectorySearchQuery(it) },
                            onLinkProfileToEntry = { profileId, entry ->
                                viewModel.linkProfileToDirectoryEntry(profileId, entry)
                            },
                            onUpdateProfileBibAndDistance = { profileId, name, myChronoId, bib10, bib5, prefDist, club, cat ->
                                viewModel.updateRunnerProfileBibAndDistance(
                                    profileId = profileId,
                                    displayName = name,
                                    myChronoId = myChronoId,
                                    defaultBib10km = bib10,
                                    defaultBib5km = bib5,
                                    preferredDistanceKm = prefDist,
                                    clubName = club,
                                    categoryLabel = cat
                                )
                            },
                            onTriggerMyChronoSync = { year -> viewModel.syncMyChronoSeason(year) },
                            onPurgeAndResyncCleanResults = { viewModel.purgeAndResyncCleanResults() },
                            diagnostics = uiState.diagnostics,
                            foundResultCounts = uiState.foundResultCounts
                        )
                    }
                }
            }
        }
    }

    // Modale Simulateur Top 15 & Prochaines Courses à courir
    if (uiState.showTop15SimulatorModal) {
        val activeSummary = uiState.activeRunnerSummary
        val simSummary = activeSummary?.simulatorForDistance(uiState.selectedChallengeDistanceKm)
        if (activeSummary != null && simSummary != null) {
            Top15SimulatorModalDialog(
                runnerName = activeSummary.profile.displayName,
                simulation = simSummary,
                onDismiss = { viewModel.setTop15SimulatorModalVisible(false) },
                onTogglePlannedRace = { raceId, isPlanned, targetPts ->
                    viewModel.togglePlannedSimulatorRace(
                        raceId = raceId,
                        profileId = uiState.selectedProfileId,
                        isPlanned = isPlanned,
                        distanceKm = uiState.selectedChallengeDistanceKm,
                        targetPoints = targetPts
                    )
                }
            )
        }
    }
}
