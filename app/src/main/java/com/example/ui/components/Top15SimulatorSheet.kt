package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.AlgChallengeCalculator
import com.example.domain.Top15SimulationSummary
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal

/**
 * Carte de synthèse et modale interactive du Simulateur Top 15 & Prochaines Courses à courir
 * (strictement séparée entre le Challenge 10 km et le Challenge 5 km).
 */
@Composable
fun Top15SimulatorBannerCard(
    simulation: Top15SimulationSummary,
    onOpenSimulatorModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top15_simulator_banner_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SemoisRiverTeal.copy(alpha = 0.10f)
        ),
        border = BorderStroke(1.dp, SemoisRiverTeal.copy(alpha = 0.38f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = SemoisRiverTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Simulateur Top 15 (${simulation.distanceKm} km)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (simulation.plannedRacesCount > 0) {
                                "${simulation.plannedRacesCount} prochaine(s) course(s) cochée(s) • +${simulation.netPointsGain} pts"
                            } else {
                                "Cochez vos prochaines courses pour simuler votre Top 15"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenSimulatorModal,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("open_top15_simulator_button")
                ) {
                    Text(
                        text = "Simuler",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimulatorMetricPill(
                    label = "Jauge Projetée",
                    value = "${simulation.projectedCountingRaces} / ${AlgChallengeCalculator.MAX_COUNTING_RACES}",
                    accent = if (simulation.willCompleteChallenge) CountingTop15BadgeGreen else SemoisRiverTeal,
                    modifier = Modifier.weight(1f)
                )
                SimulatorMetricPill(
                    label = "Total Projeté",
                    value = "${simulation.projectedTotalPoints} pts",
                    accent = GoldPodium,
                    modifier = Modifier.weight(1f)
                )
                SimulatorMetricPill(
                    label = "Seuil Top 15",
                    value = if (simulation.minimumScoreToBeatInTop15 > 0) {
                        ">${simulation.minimumScoreToBeatInTop15} pts"
                    } else "--",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SimulatorMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
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

@Composable
fun Top15SimulatorModalDialog(
    runnerName: String,
    simulation: Top15SimulationSummary,
    onDismiss: () -> Unit,
    onTogglePlannedRace: (raceId: String, isPlanned: Boolean, targetPoints: Int) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Simulateur Top 15 • ${simulation.distanceKm} km",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$runnerName • Sélectionnez vos prochaines courses à courir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                // Bandeau récapitulatif de la projection
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SemoisRiverTeal.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SemoisRiverTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Actuel : ${simulation.currentCountingRaces}/15 (${simulation.currentTotalPoints} pts)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = "Gain : +${simulation.netPointsGain} pts",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = CountingTop15BadgeGreen,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "Projeté : ${simulation.projectedCountingRaces}/15 courses • ${simulation.projectedTotalPoints} pts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (simulation.willCompleteChallenge) {
                                "Challenge ${simulation.distanceKm} km COMPLET (15/15) ! Score min retenu : ${simulation.minimumScoreToBeatInTop15} pts."
                            } else {
                                val stillMissing = AlgChallengeCalculator.MAX_COUNTING_RACES - simulation.projectedCountingRaces
                                "Cochez encore $stillMissing course(s) pour atteindre 15/15 sur le ${simulation.distanceKm} km."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (simulation.willCompleteChallenge) CountingTop15BadgeGreen else SemoisRiverTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Courses disponibles (${simulation.candidateRaces.size}) :",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(simulation.candidateRaces, key = { it.race.raceId }) { candidate ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (candidate.isPlanned) {
                                CountingTop15BadgeGreen.copy(alpha = 0.10f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (candidate.isPlanned) {
                                    CountingTop15BadgeGreen.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onTogglePlannedRace(
                                        candidate.race.raceId,
                                        !candidate.isPlanned,
                                        candidate.targetPoints
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (candidate.isPlanned) {
                                            Icons.Default.CheckBox
                                        } else {
                                            Icons.Default.CheckBoxOutlineBlank
                                        },
                                        contentDescription = null,
                                        tint = if (candidate.isPlanned) {
                                            CountingTop15BadgeGreen
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "M#${candidate.race.roundNumber} • ${candidate.race.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${candidate.race.village} • ${candidate.race.dateIso} • Terrain ${candidate.terrainCondition}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Ajustement rapide des points cibles (-10 / +10)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                onTogglePlannedRace(
                                                    candidate.race.raceId,
                                                    true,
                                                    (candidate.targetPoints - 10).coerceAtLeast(100)
                                                )
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Moins 10 pts",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${candidate.targetPoints} pts",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (candidate.isPlanned) {
                                            CountingTop15BadgeGreen
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                onTogglePlannedRace(
                                                    candidate.race.raceId,
                                                    true,
                                                    (candidate.targetPoints + 10).coerceAtMost(1000)
                                                )
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Plus 10 pts",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Valider ma sélection Top 15", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
