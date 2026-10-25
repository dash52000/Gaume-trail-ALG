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
                // ONGLET 4 : PASSEPORT DES VILLAGES (toutes saisons)
                item {
                    if (passport != null) {
                        VillagePassportCard(
                            runnerName = summary.profile.displayName,
                            passport = passport,
                            onOpenRaceDetail = onOpenRaceDetail
                        )
                    }
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

/**
 * EN-TÊTE UNIQUE COMPACT :
 * Fusionne la bannière Forêt de Gaume et la carte double 10 km / 5 km en un seul bloc compact,
 * pour voir immédiatement le contenu de l'onglet actif dès l'ouverture sur Galaxy S25.
 */
@Composable
private fun UnifiedCompactChallengeHeaderCard(
    summary: RunnerChallengeSummary,
    selectedDistanceKm: Int,
    onSelectDistanceKm: (Int) -> Unit,
    onAssignBibClick: () -> Unit,
    onOpenSimulatorClick: () -> Unit
) {
    val challenge10km = summary.challenge10km
    val challenge5km = summary.challenge5km
    val defaultBib10km = summary.profile.defaultBib10km
    val defaultBib5km = summary.profile.defaultBib5km
    val activeChallenge = if (selectedDistanceKm == 5) challenge5km else challenge10km
    val progressFraction = (activeChallenge.countingRacesCount.toFloat() / AlgChallengeCalculator.MAX_COUNTING_RACES)
        .coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top15_challenge_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Bandeau supérieur compact Forêt de Gaume (Identité coureur + bouton Dossards + bouton Simulateur)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_gaume_hero_1792505216058),
                    contentDescription = "Forêt de Gaume",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0A2416).copy(alpha = 0.92f),
                                    Color(0xFF0A2416).copy(alpha = 0.68f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${summary.profile.displayName} • Saison ${summary.seasonYear}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeChallenge.hasAssignedBib) {
                                "Dossard #${activeChallenge.assignedBibNumber} (${activeChallenge.distanceLabel})"
                            } else {
                                "Aucun dossard sur le ${activeChallenge.distanceLabel}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPodium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color.White.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAssignBibClick() }
                                .testTag("hero_edit_bib_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifier dossards",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Dossards",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            color = GoldPodium.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GoldPodium.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenSimulatorClick() }
                                .testTag("open_top15_simulator_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Simulateur Top 15",
                                    tint = GoldPodium,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Simulateur",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPodium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Corps compact : Sélecteur [Challenge 10 km] ⇄ [Challenge 5 km] + Jauge Top 15 + 3 KPIs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DistanceTabSelectorItem(
                        title = "Challenge 10 km",
                        bibLabel = if (defaultBib10km.isNotBlank()) "#$defaultBib10km" else "Aucun",
                        summary = challenge10km,
                        isSelected = selectedDistanceKm == 10,
                        onClick = { onSelectDistanceKm(10) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_challenge_10km")
                    )
                    DistanceTabSelectorItem(
                        title = "Challenge 5 km",
                        bibLabel = if (defaultBib5km.isNotBlank()) "#$defaultBib5km" else "Aucun",
                        summary = challenge5km,
                        isSelected = selectedDistanceKm == 5,
                        onClick = { onSelectDistanceKm(5) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_challenge_5km")
                    )
                }

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = if (activeChallenge.isChallengeComplete) {
                        CountingTop15BadgeGreen
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (!activeChallenge.hasAssignedBib) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAssignBibClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = ClayTerracotta,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "0 course sur le ${activeChallenge.distanceLabel} (aucun dossard). Touchez pour attribuer un dossard.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricColumnItem(
                            label = "Top 15 (${activeChallenge.countingRacesCount}/15)",
                            value = "${activeChallenge.totalChallengePoints} pts",
                            highlight = true,
                            modifier = Modifier.weight(1f)
                        )
                        MetricColumnItem(
                            label = "Moyenne / manche",
                            value = "${activeChallenge.averagePointsPerRace} pts",
                            highlight = false,
                            modifier = Modifier.weight(1f)
                        )
                        MetricColumnItem(
                            label = "Meilleur score",
                            value = "${activeChallenge.bestPoints} pts",
                            highlight = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceTabSelectorItem(
    title: String,
    bibLabel: String,
    summary: DistanceChallengeSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${summary.countingRacesCount}/15",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (summary.isChallengeComplete) {
                        CountingTop15BadgeGreen
                    } else if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }

            Text(
                text = if (summary.hasAssignedBib) {
                    "$bibLabel • ${summary.totalChallengePoints} pts"
                } else {
                    "0 course • Pas de dossard"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NoRealResultsYetBanner(
    summary: RunnerChallengeSummary,
    selectedDistanceKm: Int,
    hasAssignedBib: Boolean,
    isSyncing: Boolean,
    onEditBibClick: () -> Unit,
    onSyncClick: () -> Unit,
    onShowCalendarClick: () -> Unit
) {
    val activeBib = if (selectedDistanceKm == 5) {
        summary.profile.defaultBib5km
    } else {
        summary.profile.defaultBib10km
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("no_real_results_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (!hasAssignedBib) {
                    "0 course sur le Challenge $selectedDistanceKm km pour ${summary.profile.displayName}"
                } else {
                    "Synchronisez MyChrono pour récupérer les résultats du dossard #$activeBib ($selectedDistanceKm km)"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (!hasAssignedBib) {
                    "Aucun numéro de dossard n'est attribué à ${summary.profile.displayName} pour le Challenge $selectedDistanceKm km. Le compteur affiche 0 course."
                } else {
                    "Dossard #$activeBib actif sur le $selectedDistanceKm km. Appuyez sur Sync MyChrono pour charger toutes les manches officielles."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEditBibClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dossards 10k / 5k",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (hasAssignedBib) {
                    OutlinedButton(
                        onClick = onSyncClick,
                        enabled = !isSyncing,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSyncing) "Sync…" else "Sync MyChrono",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onShowCalendarClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text(
                            text = "Voir Calendrier",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumnItem(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SeasonKpiBentoGrid(distanceSummary: DistanceChallengeSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                title = "Allure (${distanceSummary.distanceLabel})",
                value = distanceSummary.averagePaceFormatted,
                subtitle = "${distanceSummary.averageSpeedKmh} km/h moy.",
                accentColor = MaterialTheme.colorScheme.primary
            )
            KpiStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Route,
                title = "Cumul (${distanceSummary.distanceLabel})",
                value = "${distanceSummary.totalDistanceRunKm} km",
                subtitle = "${distanceSummary.completedRacesCount} course(s)",
                accentColor = SemoisRiverTeal
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Terrain,
                title = "Dénivelé (${distanceSummary.distanceLabel})",
                value = "+${distanceSummary.totalElevationGainMeters} m",
                subtitle = "Relief Gaume & Lorraine",
                accentColor = ClayTerracotta
            )
            KpiStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                title = "Meilleure (${distanceSummary.distanceLabel})",
                value = "${distanceSummary.bestPoints} pts",
                subtitle = distanceSummary.bestRaceTitle ?: "0 course",
                accentColor = GoldPodium
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * CARTE DE COURSE ÉPURÉE (3 lignes claires et lisibles) :
 * - Ligne 1 : M#N + Nom de la course & Village + Badge Top 15 (ou Non courue)
 * - Ligne 2 (si courue) : Chrono Officiel • Allure (/km) • Classement (Xe) • Points Challenge (XXX pts)
 * - Ligne 3 : Badge Pronostic Prévu vs Réel + bouton rapide Carte Bilan
 */
@Composable
private fun RaceRoundOverviewCard(
    item: RaceItemWithContext,
    selectedDistanceKm: Int,
    pronosticEntry: GaumeRacePronosticEntry? = null,
    onClick: () -> Unit,
    onShareCardClick: () -> Unit = {}
) {
    val race = item.race
    val resultForSelectedDist = item.resultForDistance(selectedDistanceKm)
    val otherDistanceKm = if (selectedDistanceKm == 5) 10 else 5
    val resultForOtherDist = item.resultForDistance(otherDistanceKm)
    val isCounting = item.isCountingForDistance(selectedDistanceKm)
    val rankForDist = item.rankForDistance(selectedDistanceKm)
    val tw = item.terrainWeather
    val isPlannedSimulator = tw?.isPlannedForSimulator == true && tw.plannedDistanceKm == selectedDistanceKm

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("race_card_${race.roundNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (resultForSelectedDist != null && isCounting) {
                CountingTop15BadgeGreen.copy(alpha = 0.45f)
            } else if (isPlannedSimulator) {
                SemoisRiverTeal.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LIGNE 1 : M#N + Titre & Village + Statut Top 15
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (resultForSelectedDist != null) {
                            CountingTop15BadgeGreen.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = "M#${race.roundNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (resultForSelectedDist != null) {
                                CountingTop15BadgeGreen
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = race.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.gaumeGeo.canonicalVillage} • ${item.gaumeGeo.territoryName} • ${race.dateIso}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (resultForSelectedDist != null) {
                    Top15StatusBadge(
                        isCountingInTop15 = isCounting,
                        rankAmongRunnerRaces = rankForDist,
                        distanceLabel = resultForSelectedDist.distanceLabel
                    )
                } else if (isPlannedSimulator) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SemoisRiverTeal.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = tw?.simulatedTargetPoints?.let { "Prévue ($it pts)" } ?: "Prévue",
                            style = MaterialTheme.typography.labelSmall,
                            color = SemoisRiverTeal,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (resultForOtherDist != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SemoisRiverTeal.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Courue sur ${resultForOtherDist.distanceLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SemoisRiverTeal,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "À venir",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // LIGNE 2 (si courue) : Chrono Officiel • Allure • Classement • Points Challenge
            if (resultForSelectedDist != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverviewMiniStat("Chrono", resultForSelectedDist.officialTimeFormatted)
                        OverviewMiniStat("Allure", resultForSelectedDist.pacePerKmFormatted)
                        OverviewMiniStat("Scratch", "${resultForSelectedDist.overallRank}e/${resultForSelectedDist.totalFinishers}")
                        OverviewMiniStat(
                            "Points",
                            "${resultForSelectedDist.challengePoints} pts",
                            isHighlighted = true
                        )
                    }
                }
            }

            // LIGNE 3 : Pronostic Prévu vs Réel + Accès direct Carte Bilan
            if (pronosticEntry != null || resultForSelectedDist != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pronosticEntry != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            RacePronosticVsActualMiniBanner(entry = pronosticEntry)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (resultForSelectedDist != null) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onShareCardClick() }
                                .testTag("open_share_card_${race.roundNumber}"),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Carte Bilan",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Carte",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewMiniStat(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun SeasonOverBanner(recap: SeasonRecap, onOpenRecap: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GoldPodium.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, GoldPodium.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenRecap() }
            .testTag("season_over_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldPodium, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saison ${recap.seasonYear} terminée",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = (if (recap.challengeComplete) "Challenge ${recap.distanceLabel} validé" else "${recap.countingRaces}/15 courses comptées") +
                        " • ${recap.racesRun} courses • touchez pour le bilan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HorsChallengeSectionHeader(seasonYear: Int, count: Int, runCount: Int) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Text(
            text = "Hors challenge $seasonYear ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = (if (runCount > 0) "$runCount courue(s) • " else "") +
                "résultats récupérés quand MyChrono les publie, sans effet sur le challenge",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HorsChallengeRaceCard(item: RaceItemWithContext, onClick: () -> Unit) {
    val race = item.race
    val res = item.result10km ?: item.result5km
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("hors_challenge_card_${race.raceId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    text = "HC",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = race.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        item.gaumeGeo.canonicalVillage,
                        race.dateIso,
                        listOfNotNull(race.longDistanceLabel, race.shortDistanceLabel).joinToString(" / ").ifBlank { null }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            if (res != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = res.officialTimeFormatted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${res.distanceName ?: res.distanceLabel} • ${res.overallRank}e/${res.totalFinishers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = if (race.isCompleted) "Non courue" else "À venir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
