package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AlgChallengeCalculator
import com.example.domain.DistanceChallengeSummary
import com.example.domain.RaceItemWithContext
import com.example.domain.RunnerChallengeSummary
import com.example.data.local.RunnerProfileEntity
import com.example.domain.ProfileChallengeStanding
import com.example.domain.SameCourseYearComparison
import com.example.domain.SeasonRecap
import com.example.domain.CourseRecord
import com.example.domain.FactBadge
import com.example.domain.GoalProgress
import com.example.domain.GoalsCalculator
import com.example.domain.RecordStatus
import com.example.domain.RecordsSummary
import com.example.domain.TrioChallenge
import com.example.domain.VillageMapPoint
import com.example.data.local.VillageInfoEntity
import com.example.domain.VillagePassport
import com.example.ui.components.SeasonRecapCard
import com.example.ui.components.VillagePassportCard
import com.example.ui.components.ChallengeStandingCard
import com.example.ui.components.EditProfileBibAndDistanceDialog
import com.example.ui.components.GaumePerfsAndToolsHubSection
import com.example.ui.components.GaumePerfsCalculator
import com.example.ui.components.GaumeRacePronosticEntry
import com.example.ui.components.NextUpcomingPronosticBannerCard
import com.example.ui.components.RacePronosticVsActualMiniBanner
import com.example.ui.components.Top15StatusBadge
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal
import com.example.ui.viewmodel.AlgBottomTab
import com.example.ui.viewmodel.RaceFilterMode

@Composable
fun RunnerOverviewScreen(
    summary: RunnerChallengeSummary?,
    selectedDistanceKm: Int,
    raceFilterMode: RaceFilterMode,
    isSyncingMyChrono: Boolean,
    activeBottomTab: AlgBottomTab = AlgBottomTab.OVERVIEW,
    perfsSubTab: Int = 0,
    preselectedShareRaceId: String? = null,
    multiSeasonSummaries: Map<Int, RunnerChallengeSummary> = emptyMap(),
    sameCourseComparisons: List<SameCourseYearComparison> = emptyList(),
    comparisonYearA: Int = summary?.seasonYear?.minus(1) ?: 0,
    comparisonYearB: Int = summary?.seasonYear ?: 0,
    trioStandings: Map<String, Map<Int, ProfileChallengeStanding>> = emptyMap(),
    profiles: List<RunnerProfileEntity> = emptyList(),
    horsChallengeItems: List<RaceItemWithContext> = emptyList(),
    passport: VillagePassport? = null,
    activeRecap: SeasonRecap? = null,
    trioRecaps: Map<String, SeasonRecap> = emptyMap(),
    records: RecordsSummary? = null,
    badges: List<FactBadge> = emptyList(),
    trioChallenges: List<TrioChallenge> = emptyList(),
    goalSeason: Int = summary?.seasonYear ?: 0,
    goals: List<GoalProgress> = emptyList(),
    villageInfos: Map<String, VillageInfoEntity> = emptyMap(),
    onSetGoal: (goalType: String, target: Int) -> Unit = { _, _ -> },
    onRequestVillageInfo: (String) -> Unit = {},
    onSelectDistanceKm: (Int) -> Unit,
    onSelectFilterMode: (RaceFilterMode) -> Unit,
    onOpenRaceDetail: (String) -> Unit,
    onOpenTop15Simulator: () -> Unit,
    onOpenPerfsTabWithSubTab: (subTab: Int, shareRaceId: String?) -> Unit = { _, _ -> },
    onSelectPerfsSubTab: (Int) -> Unit = {},
    onSelectShareRaceId: (String) -> Unit = {},
    onSelectSeasonYear: (Int) -> Unit = {},
    onTogglePlannedSimulatorRace: (raceId: String, isPlanned: Boolean, targetPoints: Int) -> Unit,
    onQuickUpdateTerrain: (raceId: String, terrainCondition: String) -> Unit,
    onTriggerMyChronoSync: () -> Unit,
    onUpdateRunnerBibAndDistance: (
        profileId: String,
        displayName: String,
        myChronoId: String,
        bib10km: String,
        bib5km: String,
        preferredDistanceKm: Int,
        clubName: String,
        categoryLabel: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary == null) return

    var showEditBibDialog by remember { mutableStateOf(false) }

    val activeDistanceSummary = summary.challengeForDistance(selectedDistanceKm)
    val seasonPronostics = remember(summary, selectedDistanceKm) {
        GaumePerfsCalculator.computeSeasonPronostics(
            summary = summary,
            selectedDistanceKm = selectedDistanceKm
        )
    }

    // Si aucun dossard n'est attribué pour cette distance, 0 course affichée en mode ALL / COMPLETED / TOP_15.
    // En mode ALL avec dossard actif, on place les courses courues en premier pour une lecture immédiate !
    val filteredRaces = remember(
        summary.racesWithContext,
        raceFilterMode,
        selectedDistanceKm,
        activeDistanceSummary.hasAssignedBib
    ) {
        when (raceFilterMode) {
            RaceFilterMode.ALL -> {
                if (!activeDistanceSummary.hasAssignedBib) {
                    emptyList()
                } else {
                    summary.racesWithContext.sortedWith(
                        compareByDescending<RaceItemWithContext> {
                            it.resultForDistance(selectedDistanceKm) != null
                        }.thenBy { it.race.roundNumber }
                    )
                }
            }
            RaceFilterMode.COMPLETED -> summary.racesWithContext.filter {
                it.resultForDistance(selectedDistanceKm) != null
            }
            RaceFilterMode.UPCOMING -> summary.racesWithContext.filter {
                it.resultForDistance(selectedDistanceKm) == null
            }
            RaceFilterMode.TOP_15_ONLY -> summary.racesWithContext.filter {
                it.isCountingForDistance(selectedDistanceKm)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("runner_overview_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // EN-TÊTE UNIQUE COMPACT (fusionne la bannière et le sélecteur 10 km / 5 km)
        item {
            UnifiedCompactChallengeHeaderCard(
                summary = summary,
                selectedDistanceKm = selectedDistanceKm,
                onSelectDistanceKm = onSelectDistanceKm,
                onAssignBibClick = { showEditBibDialog = true },
                onOpenSimulatorClick = onOpenTop15Simulator
            )
        }

        when (activeBottomTab) {
            AlgBottomTab.OVERVIEW -> {
                // ONGLET 1 : COURSES (Classement provisoire + Pronostic Prochaine Manche + Liste + Filtres rapides)
                if (activeRecap != null && activeRecap.isSeasonOver && activeRecap.racesRun > 0) {
                    item {
                        SeasonOverBanner(
                            recap = activeRecap,
                            onOpenRecap = { onOpenPerfsTabWithSubTab(2, null) }
                        )
                    }
                }

                if (activeDistanceSummary.hasAssignedBib) {
                    item {
                        ChallengeStandingCard(
                            activeProfile = summary.profile,
                            selectedDistanceKm = selectedDistanceKm,
                            trioStandings = trioStandings,
                            profiles = profiles
                        )
                    }
                }

                if (activeDistanceSummary.hasAssignedBib) {
                    item {
                        NextUpcomingPronosticBannerCard(
                            pronosticsSummary = seasonPronostics,
                            onOpenPerfsTab = { onOpenPerfsTabWithSubTab(0, null) },
                            onOpenRaceDetail = onOpenRaceDetail
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Manches ${summary.seasonYear} • ${activeDistanceSummary.distanceLabel}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeDistanceSummary.completedRacesCount} courue(s) • ${activeDistanceSummary.countingRacesCount}/15 Top 15",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filters = listOf(
                                RaceFilterMode.ALL to if (activeDistanceSummary.hasAssignedBib) "Toutes" else "Mes courses (0)",
                                RaceFilterMode.COMPLETED to "Courues (${activeDistanceSummary.completedRacesCount})",
                                RaceFilterMode.TOP_15_ONLY to "Top 15 (${activeDistanceSummary.countingRacesCount})",
                                RaceFilterMode.UPCOMING to "Calendrier"
                            )
                            filters.forEach { (mode, label) ->
                                val isSelected = raceFilterMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectFilterMode(mode) }
                                        .testTag("filter_chip_${mode.name.lowercase()}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (activeDistanceSummary.completedRacesCount == 0 && raceFilterMode != RaceFilterMode.UPCOMING) {
                    item {
                        NoRealResultsYetBanner(
                            summary = summary,
                            selectedDistanceKm = selectedDistanceKm,
                            hasAssignedBib = activeDistanceSummary.hasAssignedBib,
                            isSyncing = isSyncingMyChrono,
                            onEditBibClick = { showEditBibDialog = true },
                            onSyncClick = onTriggerMyChronoSync,
                            onShowCalendarClick = { onSelectFilterMode(RaceFilterMode.UPCOMING) }
                        )
                    }
                }

                if (filteredRaces.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                        ) {
                            Text(
                                text = if (!activeDistanceSummary.hasAssignedBib) {
                                    "0 course pour ${summary.profile.displayName} sur le Challenge ${activeDistanceSummary.distanceLabel} (aucun dossard attribué pour ce challenge)."
                                } else {
                                    "0 course enregistrée dans ce filtre pour le Challenge ${activeDistanceSummary.distanceLabel}."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(filteredRaces, key = { "${it.race.raceId}_${selectedDistanceKm}" }) { item ->
                        RaceRoundOverviewCard(
                            item = item,
                            selectedDistanceKm = selectedDistanceKm,
                            courseRecord = records?.byRaceId?.get(item.race.raceId),
                            pronosticEntry = if (activeDistanceSummary.hasAssignedBib) {
                                seasonPronostics.allPronosticsByRaceId[item.race.raceId]
                            } else null,
                            onClick = { onOpenRaceDetail(item.race.raceId) },
                            onShareCardClick = {
                                onOpenPerfsTabWithSubTab(2, item.race.raceId)
                            }
                        )
                    }
                }

                // Courses hors challenge : résultats récupérés, jamais comptés pour le challenge.
                if (horsChallengeItems.isNotEmpty() && raceFilterMode != RaceFilterMode.TOP_15_ONLY) {
                    item {
                        HorsChallengeSectionHeader(
                            seasonYear = summary.seasonYear,
                            count = horsChallengeItems.size,
                            runCount = horsChallengeItems.count { it.result10km != null || it.result5km != null }
                        )
                    }
                    items(horsChallengeItems, key = { "hc_${it.race.raceId}" }) { item ->
                        HorsChallengeRaceCard(item = item, onClick = { onOpenRaceDetail(item.race.raceId) })
                    }
                }
            }

            AlgBottomTab.PERFS -> {
                // ONGLET 2 : PERFS (Organisé en 4 sous-vues : Pronostics, Records & Stats, Carte Bilan & Export, Saisons)
                item {
                    GaumePerfsAndToolsHubSection(
                        summary = summary,
                        selectedDistanceKm = selectedDistanceKm,
                        preselectedShareRaceId = preselectedShareRaceId,
                        onSelectShareRaceId = onSelectShareRaceId,
                        onOpenRaceDetail = onOpenRaceDetail,
                        activeSubTab = perfsSubTab,
                        onSelectSubTab = onSelectPerfsSubTab,
                        statsSlot = {
                            SeasonKpiBentoGrid(
                                distanceSummary = activeDistanceSummary
                            )
                        },
                        seasonsSlot = if (multiSeasonSummaries.isNotEmpty()) {
                            {
                                MultiSeasonProgressionContent(
                                    activeRunnerName = summary.profile.displayName,
                                    selectedDistanceKm = selectedDistanceKm,
                                    multiSeasonSummaries = multiSeasonSummaries,
                                    sameCourseComparisons = sameCourseComparisons,
                                    comparisonYearA = comparisonYearA,
                                    comparisonYearB = comparisonYearB,
                                    onSelectDistanceKm = onSelectDistanceKm,
                                    onSelectSeasonYear = onSelectSeasonYear
                                )
                            }
                        } else null,
                        recapSlot = activeRecap?.let { recap ->
                            {
                                SeasonRecapCard(
                                    recap = recap,
                                    trioRecaps = trioRecaps,
                                    profiles = profiles
                                )
                            }
                        }
                    )
                }
            }

            AlgBottomTab.BADGES -> {
                // ONGLET 4 : DÉFIS (records de parcours, badges, Trio, villages, objectifs)
                item {
                    com.example.ui.screens.ChallengesContent(
                        runnerName = summary.profile.displayName,
                        seasonYear = summary.seasonYear,
                        selectedDistanceKm = selectedDistanceKm,
                        records = records,
                        badges = badges,
                        trioChallenges = trioChallenges,
                        goalSeason = goalSeason,
                        goals = goals,
                        passport = passport,
                        villageInfos = villageInfos,
                        onSelectDistanceKm = onSelectDistanceKm,
                        onOpenRaceDetail = onOpenRaceDetail,
                        onSetGoal = onSetGoal,
                        onRequestVillageInfo = onRequestVillageInfo
                    )
                }
            }

            else -> Unit
        }
    }

    if (showEditBibDialog) {
        EditProfileBibAndDistanceDialog(
            profile = summary.profile,
            seasonYear = summary.seasonYear,
            onDismiss = { showEditBibDialog = false },
            onConfirm = { name, myChronoId, bib10, bib5, prefDist, club, cat ->
                showEditBibDialog = false
                onUpdateRunnerBibAndDistance(
                    summary.profile.profileId,
                    name,
                    myChronoId,
                    bib10,
                    bib5,
                    prefDist,
                    club,
                    cat
                )
            }
        )
    }
}
