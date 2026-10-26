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
 * CARTE DE COURSE ÉPURÉE (3 lignes claires et lisibles) :
 * - Ligne 1 : M#N + Nom de la course & Village + Badge Top 15 (ou Non courue)
 * - Ligne 2 (si courue) : Chrono Officiel • Allure (/km) • Classement (Xe) • Points Challenge (XXX pts)
 * - Ligne 3 : Badge Pronostic Prévu vs Réel + bouton rapide Carte Bilan
 */
@Composable
internal fun RaceRoundOverviewCard(
    item: RaceItemWithContext,
    selectedDistanceKm: Int,
    courseRecord: CourseRecord? = null,
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

            // Record de parcours : duel contre les éditions précédentes
            courseRecord?.let { RecordHintLine(it) }

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
internal fun HorsChallengeSectionHeader(seasonYear: Int, count: Int, runCount: Int) {
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
internal fun HorsChallengeRaceCard(item: RaceItemWithContext, onClick: () -> Unit) {
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

@Composable
private fun RecordHintLine(record: CourseRecord) {
    val ref = record.bestBefore
    val (text, color) = when (record.status) {
        RecordStatus.BEATEN -> "Record de parcours battu : ${GoalsCalculator.formatDelta(record.deltaSeconds ?: 0)} sur ${ref?.seasonYear ?: ""}" to CountingTop15BadgeGreen
        RecordStatus.NOT_BEATEN -> "${GoalsCalculator.formatDelta(record.deltaSeconds ?: 0)} sur ton record (${ref?.timeFormatted ?: ""}, ${ref?.seasonYear ?: ""})" to ClayTerracotta
        RecordStatus.TO_BEAT -> "Record à battre : ${record.best.timeFormatted} (${record.best.seasonYear})" to GoldPodium
        RecordStatus.FIRST -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
