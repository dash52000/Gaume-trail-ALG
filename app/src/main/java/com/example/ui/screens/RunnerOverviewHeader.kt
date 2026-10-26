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

/**
 * EN-TÊTE UNIQUE COMPACT :
 * Fusionne la bannière Forêt de Gaume et la carte double 10 km / 5 km en un seul bloc compact,
 * pour voir immédiatement le contenu de l'onglet actif dès l'ouverture sur Galaxy S25.
 */
@Composable
internal fun UnifiedCompactChallengeHeaderCard(
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
internal fun NoRealResultsYetBanner(
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
internal fun SeasonKpiBentoGrid(distanceSummary: DistanceChallengeSummary) {
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

@Composable
internal fun SeasonOverBanner(recap: SeasonRecap, onOpenRecap: () -> Unit) {
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
