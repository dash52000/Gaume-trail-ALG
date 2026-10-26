package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.domain.AlgChallengeCalculator
import com.example.domain.DistanceChallengeSummary
import com.example.domain.RaceItemWithContext
import com.example.domain.RunnerChallengeSummary
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hub complet de l'onglet « Perfs » dans l'écran principal :
 * 1. Pronostics des Prochaines Manches à Venir (basés sur les 3 dernières courses)
 * 2. Tableau des Records Personnels (PR) & Statistiques de Saison
 * 3. Carte Bilan Visuelle Partageable d'Après-Course (PNG + Texte) & Export CSV
 * 4. Comparaison inter-saisons (2026 vs 2027)
 */
@Composable
fun GaumePerfsAndToolsHubSection(
    summary: RunnerChallengeSummary,
    selectedDistanceKm: Int,
    preselectedShareRaceId: String?,
    onSelectShareRaceId: (String) -> Unit,
    onOpenRaceDetail: (String) -> Unit = {},
    activeSubTab: Int = 0,
    onSelectSubTab: (Int) -> Unit = {},
    statsSlot: @Composable () -> Unit = {},
    seasonsSlot: (@Composable () -> Unit)? = null,
    recapSlot: (@Composable () -> Unit)? = null
) {
    val activeDistanceSummary = summary.summaryForDistance(selectedDistanceKm)
    val personalRecords = remember(activeDistanceSummary, summary.racesWithContext) {
        GaumePerfsCalculator.computePersonalRecords(
            distanceSummary = activeDistanceSummary,
            racesWithContext = summary.racesWithContext
        )
    }
    val pronosticsSummary = remember(summary, selectedDistanceKm) {
        GaumePerfsCalculator.computeSeasonPronostics(
            summary = summary,
            selectedDistanceKm = selectedDistanceKm
        )
    }

    var showPredictorAccordion by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gaume_perfs_hub_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Barre de sous-vues épurée : [Pronostics] [Records] [Carte Bilan] [Saisons]
        val subTabs = buildList {
            add(0 to "Forme")
            add(1 to "Records (${personalRecords.completedCount})")
            add(2 to "Bilan")
            if (seasonsSlot != null) {
                add(3 to "Saisons")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                subTabs.forEach { (idx, label) ->
                    val isSel = activeSubTab == idx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSubTab(idx) }
                            .testTag("perfs_subtab_$idx"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSel) {
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

        when (activeSubTab) {
            0 -> {
                // Sous-vue 1 : forme corrigée du parcours, puis pronostics des prochaines manches
                FormAndCourseCard(
                    summary = summary,
                    selectedDistanceKm = selectedDistanceKm,
                    onOpenRaceDetail = onOpenRaceDetail
                )
                GaumePronosticPrevuVsReelSectionCard(
                    runnerName = summary.profile.displayName,
                    pronosticsSummary = pronosticsSummary,
                    onOpenRaceDetail = onOpenRaceDetail
                )

                // Simulateur D+ en accordéon repliable pour ne pas alourdir l'écran
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showPredictorAccordion = !showPredictorAccordion }
                        .testTag("toggle_predictor_accordion")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Landscape,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Simulateur d'allure selon le dénivelé",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (showPredictorAccordion) "Touchez pour replier le simulateur" else "Touchez pour tester une allure ou un dénivelé personnalisé",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = if (showPredictorAccordion) "Masquer ▲" else "Ouvrir ▼",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showPredictorAccordion) {
                    GaumeElevationPacePredictorCard(
                        summary = summary,
                        selectedDistanceKm = selectedDistanceKm,
                        activeDistanceSummary = activeDistanceSummary
                    )
                }
            }

            1 -> {
                // Sous-vue 2 : Tableau des 6 Records Personnels (PR) + Statistiques de Saison
                GaumePersonalRecordsSectionCard(
                    runnerName = summary.profile.displayName,
                    recordsSummary = personalRecords
                )
                statsSlot()
            }

            2 -> {
                // Sous-vue 3 : bilan de la saison, carte d'une course (PNG) & export CSV
                recapSlot?.invoke()
                GaumeShareableFinisherCard(
                    summary = summary,
                    selectedDistanceKm = selectedDistanceKm,
                    pronosticsSummary = pronosticsSummary,
                    preselectedShareRaceId = preselectedShareRaceId,
                    onSelectShareRaceId = onSelectShareRaceId
                )
                GaumeSeasonCsvExportCard(
                    summary = summary,
                    distanceSummary = activeDistanceSummary,
                    pronosticsSummary = pronosticsSummary
                )
            }

            3 -> {
                // Sous-vue 4 : Comparaison inter-saisons (2026 vs 2027)
                seasonsSlot?.invoke()
            }
        }
    }
}
