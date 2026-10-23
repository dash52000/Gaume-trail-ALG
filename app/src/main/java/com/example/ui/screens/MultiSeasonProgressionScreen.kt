package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AlgChallengeCalculator
import com.example.domain.RunnerChallengeSummary
import com.example.domain.SameCourseYearComparison
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal

/**
 * Vue Saisons (à partir de 2026 : 2026, 2027...) :
 * - Sépare strictement le Challenge 10 km et le Challenge 5 km
 * - Affiche le bilan par année >= 2026 (sans jamais afficher 2025)
 * - Compare les chronos et points manche par manche entre 2026 et 2027+
 */
@Composable
fun MultiSeasonProgressionScreen(
    activeRunnerName: String,
    selectedDistanceKm: Int,
    multiSeasonSummaries: Map<Int, RunnerChallengeSummary>,
    sameCourseComparisons2026Vs2027: List<SameCourseYearComparison>,
    onSelectDistanceKm: (Int) -> Unit,
    onSelectSeasonYear: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("multi_season_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MultiSeasonProgressionContent(
                activeRunnerName = activeRunnerName,
                selectedDistanceKm = selectedDistanceKm,
                multiSeasonSummaries = multiSeasonSummaries,
                sameCourseComparisons2026Vs2027 = sameCourseComparisons2026Vs2027,
                onSelectDistanceKm = onSelectDistanceKm,
                onSelectSeasonYear = onSelectSeasonYear
            )
        }
    }
}

@Composable
fun MultiSeasonProgressionContent(
    activeRunnerName: String,
    selectedDistanceKm: Int,
    multiSeasonSummaries: Map<Int, RunnerChallengeSummary>,
    sameCourseComparisons2026Vs2027: List<SameCourseYearComparison>,
    onSelectDistanceKm: (Int) -> Unit,
    onSelectSeasonYear: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedYears = multiSeasonSummaries.keys.filter { it >= 2026 }.sorted()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_season_content"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Progression Saisons (dès 2026)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$activeRunnerName • Historique et évolution 2026 → 2027+",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Sélecteur strictement séparé 10 km / 5 km
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 5).forEach { distKm ->
                        val isSelected = selectedDistanceKm == distKm
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectDistanceKm(distKm) }
                                .testTag("seasons_dist_tab_${distKm}km"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Challenge $distKm km",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cartes Bilan par année (2026, 2027...)
        sortedYears.forEach { year ->
            val summary = multiSeasonSummaries[year] ?: return@forEach
            val distSummary = summary.challengeForDistance(selectedDistanceKm)
            val progressFraction = (distSummary.countingRacesCount.toFloat() / AlgChallengeCalculator.MAX_COUNTING_RACES)
                .coerceIn(0f, 1f)

            val terrainCounts = summary.racesWithContext.mapNotNull { it.terrainWeather?.terrainCondition }
            val muddyCount = terrainCounts.count { it == "Boueux" || it == "Très gras" }
            val dryCount = terrainCounts.count { it == "Sec" || it == "Souple" }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSeasonYear(year) }
                    .testTag("season_summary_card_$year"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(
                    1.dp,
                    if (distSummary.completedRacesCount > 0) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saison $year • ${distSummary.distanceLabel}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (distSummary.isChallengeComplete) {
                                CountingTop15BadgeGreen.copy(alpha = 0.16f)
                            } else {
                                GoldPodium.copy(alpha = 0.18f)
                            }
                        ) {
                            Text(
                                text = "${distSummary.countingRacesCount} / 15 courses • ${distSummary.totalChallengePoints} pts",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (distSummary.isChallengeComplete) {
                                    CountingTop15BadgeGreen
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = if (distSummary.isChallengeComplete) CountingTop15BadgeGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeasonMetricBox(
                            label = "Moyenne / course",
                            value = "${distSummary.averagePointsPerRace} pts",
                            modifier = Modifier.weight(1f)
                        )
                        SeasonMetricBox(
                            label = "Allure moy.",
                            value = distSummary.averagePaceFormatted,
                            modifier = Modifier.weight(1f)
                        )
                        SeasonMetricBox(
                            label = "Cumul",
                            value = "${distSummary.totalDistanceRunKm} km",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (muddyCount > 0 || dryCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Landscape,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Terrains notés en $year : $dryCount sec/souple • $muddyCount boueux/gras",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Comparaison Manche par Manche 2026 vs 2027
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = SemoisRiverTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comparaison 2026 vs 2027 ($selectedDistanceKm km)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (sameCourseComparisons2026Vs2027.isEmpty()) {
                    Text(
                        text = "Aucune course enregistrée sur cette distance pour comparer 2026 et 2027.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    sameCourseComparisons2026Vs2027.forEach { cmp ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cmp.courseTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "2026: ${cmp.resultSeasonA?.officialTimeFormatted ?: "--:--"} (${cmp.resultSeasonA?.challengePoints ?: 0} pts) • 2027: ${cmp.resultSeasonB?.officialTimeFormatted ?: "À venir"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (cmp.timeDeltaSeconds != null) {
                                    val faster = cmp.timeDeltaSeconds <= 0
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (faster) {
                                                Icons.AutoMirrored.Filled.TrendingDown
                                            } else {
                                                Icons.AutoMirrored.Filled.TrendingUp
                                            },
                                            contentDescription = null,
                                            tint = if (faster) CountingTop15BadgeGreen else ClayTerracotta,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = AlgChallengeCalculator.formatSignedTimeDelta(cmp.timeDeltaSeconds),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (faster) CountingTop15BadgeGreen else ClayTerracotta
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
