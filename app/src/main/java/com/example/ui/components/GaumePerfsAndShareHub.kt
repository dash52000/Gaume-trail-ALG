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

enum class GaumeRacePronosticVerdict {
    BETTER_THAN_EXPECTED,
    ON_TARGET,
    SLOWER_THAN_EXPECTED,
    UPCOMING
}

/**
 * Pronostic avant-course et verdict après-course (Prévu vs Réel) pour une manche donnée.
 */
data class GaumeRacePronosticEntry(
    val raceId: String,
    val roundNumber: Int,
    val villageName: String,
    val raceTitle: String,
    val dateIso: String,
    val distanceKm: Double,
    val elevationGainMeters: Int,
    val terrainCondition: String,
    val recentEquivalentFlatPaceSecPerKm: Int,
    val recentEquivalentFlatPaceFormatted: String,
    val predictedTimeSeconds: Int,
    val predictedTimeFormatted: String,
    val predictedPaceSecPerKm: Int,
    val predictedPaceFormatted: String,
    val actualTimeSeconds: Int?,
    val actualTimeFormatted: String?,
    val actualPaceFormatted: String?,
    val actualPoints: Int?,
    val deltaSeconds: Int?, // Chrono réel - Chrono prévu (négatif = plus rapide que prévu !)
    val deltaFormatted: String?,
    val verdict: GaumeRacePronosticVerdict,
    val verdictLabel: String,
    val verdictShortBadge: String
)

/**
 * Course récente de référence (issue des 3 dernières courses courues) servant de base au pronostic.
 */
data class GaumeRecentReferenceRace(
    val raceId: String,
    val roundNumber: Int,
    val villageName: String,
    val dateIso: String,
    val officialTimeFormatted: String,
    val pacePerKmFormatted: String,
    val elevationGainMeters: Int,
    val equivalentFlatPaceFormatted: String
)

/**
 * Synthèse complète des pronostics avant-course (uniquement pour les prochaines manches à venir)
 * calculés à partir des 3 dernières courses courues.
 */
data class GaumeSeasonPronosticsSummary(
    val distanceKm: Int,
    val distanceLabel: String,
    val hasAssignedBib: Boolean,
    val recentEquivalentFlatPaceSecPerKm: Int,
    val recentEquivalentFlatPaceFormatted: String,
    val recentEquivalentSpeedKmh: Double,
    val recentReferenceRaces: List<GaumeRecentReferenceRace> = emptyList(),
    val nextUpcomingPronostic: GaumeRacePronosticEntry?,
    val completedWithVerdict: List<GaumeRacePronosticEntry>,
    val upcomingPronostics: List<GaumeRacePronosticEntry>,
    val allPronosticsByRaceId: Map<String, GaumeRacePronosticEntry>,
    val betterThanExpectedCount: Int,
    val onTargetCount: Int,
    val slowerThanExpectedCount: Int,
    val averageDeltaSeconds: Int
)

data class GaumePersonalRecordEntry(
    val categoryTitle: String,
    val mainValue: String,
    val secondaryValue: String,
    val villageAndRound: String,
    val dateIso: String,
    val accentColorHex: Long
)

data class GaumePersonalRecordsSummary(
    val distanceLabel: String,
    val completedCount: Int,
    val fastestRaceRecord: GaumePersonalRecordEntry?,
    val bestPointsRecord: GaumePersonalRecordEntry?,
    val bestOverallRankRecord: GaumePersonalRecordEntry?,
    val bestCategoryRankRecord: GaumePersonalRecordEntry?,
    val biggestElevationRecord: GaumePersonalRecordEntry?,
    val regularityRecord: GaumePersonalRecordEntry?
)

object GaumePerfsCalculator {

    private fun terrainPenaltyMultiplier(terrainCondition: String?): Double {
        return when (terrainCondition?.trim()) {
            "Souple" -> 1.022
            "Boueux" -> 1.052
            "Très gras" -> 1.085
            else -> 1.0
        }
    }

    private fun effectiveDistanceForChallenge(item: RaceItemWithContext, selectedDistanceKm: Int): Double {
        return if (selectedDistanceKm == 5) {
            5.0
        } else {
            item.race.distanceKm.takeIf { it >= 5.0 } ?: 10.0
        }
    }

    private fun effectiveElevationForChallenge(item: RaceItemWithContext, selectedDistanceKm: Int): Int {
        val rawElev = item.activityMetrics?.elevationGainMeters ?: item.race.elevationGainMeters
        return if (selectedDistanceKm == 5) {
            (rawElev * 0.52).roundToInt().coerceAtLeast(25)
        } else {
            rawElev.coerceAtLeast(35)
        }
    }

    /**
     * Convertit le chrono réel d'une manche vallonnée en Allure Équivalente sur Plat (sec/km)
     * en neutralisant le coût du dénivelé (+100 m D+ ≈ +0.78 km d'effort plat) et du terrain.
     */
    fun computeEquivalentFlatPaceSecPerKm(
        item: RaceItemWithContext,
        selectedDistanceKm: Int
    ): Int? {
        val res = item.resultForDistance(selectedDistanceKm) ?: return null
        val effDistKm = effectiveDistanceForChallenge(item, selectedDistanceKm)
        val effElevM = effectiveElevationForChallenge(item, selectedDistanceKm)
        val terrainMult = terrainPenaltyMultiplier(item.terrainWeather?.terrainCondition)
        val flatEquivalentEffortKm = effDistKm + (effElevM / 100.0) * 0.78
        if (flatEquivalentEffortKm <= 0.5) return null
        val cleanFlatSeconds = res.officialTimeSeconds / terrainMult
        return (cleanFlatSeconds / flatEquivalentEffortKm).roundToInt().coerceIn(165, 540)
    }

    /**
     * Calcule les pronostics UNIQUEMENT pour les prochaines manches à venir (non encore courues)
     * et exclusivement à partir de l'historique récent (les 3 dernières courses courues).
     */
    fun computeSeasonPronostics(
        summary: RunnerChallengeSummary,
        selectedDistanceKm: Int
    ): GaumeSeasonPronosticsSummary {
        val distSummary = summary.challengeForDistance(selectedDistanceKm)
        val sortedItems = summary.racesWithContext.sortedBy { it.race.roundNumber }

        // Extraction des courses réellement courues sur cette distance avec leur allure équivalente plat
        val completedTriples = sortedItems.mapNotNull { item ->
            val res = item.resultForDistance(selectedDistanceKm) ?: return@mapNotNull null
            val flatPace = computeEquivalentFlatPaceSecPerKm(item, selectedDistanceKm) ?: return@mapNotNull null
            Triple(item, res, flatPace)
        }

        // Sélection stricte des 3 dernières courses courues (historique récent)
        val recentLast3 = completedTriples.takeLast(3)

        if (recentLast3.isEmpty()) {
            return GaumeSeasonPronosticsSummary(
                distanceKm = selectedDistanceKm,
                distanceLabel = distSummary.distanceLabel,
                hasAssignedBib = distSummary.hasAssignedBib,
                recentEquivalentFlatPaceSecPerKm = 0,
                recentEquivalentFlatPaceFormatted = "--:-- /km",
                recentEquivalentSpeedKmh = 0.0,
                recentReferenceRaces = emptyList(),
                nextUpcomingPronostic = null,
                completedWithVerdict = emptyList(),
                upcomingPronostics = emptyList(),
                allPronosticsByRaceId = emptyMap(),
                betterThanExpectedCount = 0,
                onTargetCount = 0,
                slowerThanExpectedCount = 0,
                averageDeltaSeconds = 0
            )
        }

        val currentRecentFlatPaceSec = recentLast3.map { it.third }.average().roundToInt()
        val refFlatPaceStr = "${formatSecondsToMmSs(currentRecentFlatPaceSec)} /km"
        val eqSpeedKmh = ((3600.0 / currentRecentFlatPaceSec) * 10.0).roundToInt() / 10.0

        val recentReferenceRaces = recentLast3.reversed().map { (item, res, flatPaceSec) ->
            GaumeRecentReferenceRace(
                raceId = item.race.raceId,
                roundNumber = item.race.roundNumber,
                villageName = item.gaumeGeo.canonicalVillage,
                dateIso = item.race.dateIso,
                officialTimeFormatted = res.officialTimeFormatted,
                pacePerKmFormatted = res.pacePerKmFormatted,
                elevationGainMeters = effectiveElevationForChallenge(item, selectedDistanceKm),
                equivalentFlatPaceFormatted = "${formatSecondsToMmSs(flatPaceSec)} /km"
            )
        }

        val maxCompletedRound = completedTriples.maxOf { it.first.race.roundNumber }

        // Uniquement les prochaines manches à venir (situées après la dernière manche courue et sans résultat)
        val upcomingItems = sortedItems.filter { targetItem ->
            targetItem.resultForDistance(selectedDistanceKm) == null &&
                !targetItem.race.isCompleted &&
                targetItem.race.roundNumber > maxCompletedRound
        }

        val upcomingList = upcomingItems.map { targetItem ->
            val effDist = effectiveDistanceForChallenge(targetItem, selectedDistanceKm)
            val effElev = effectiveElevationForChallenge(targetItem, selectedDistanceKm)
            val terrainCond = targetItem.terrainWeather?.terrainCondition ?: "Sec"
            val terrainMult = terrainPenaltyMultiplier(terrainCond)

            val flatEffortKm = effDist + (effElev / 100.0) * 0.78
            val predictedTimeSec = (currentRecentFlatPaceSec * flatEffortKm * terrainMult).roundToInt()
            val predictedPaceSec = (predictedTimeSec / effDist).roundToInt()

            val predictedTimeStr = formatSecondsToMmSs(predictedTimeSec)
            val predictedPaceStr = "${formatSecondsToMmSs(predictedPaceSec)} /km"

            GaumeRacePronosticEntry(
                raceId = targetItem.race.raceId,
                roundNumber = targetItem.race.roundNumber,
                villageName = targetItem.gaumeGeo.canonicalVillage,
                raceTitle = targetItem.race.title,
                dateIso = targetItem.race.dateIso,
                distanceKm = effDist,
                elevationGainMeters = effElev,
                terrainCondition = terrainCond,
                recentEquivalentFlatPaceSecPerKm = currentRecentFlatPaceSec,
                recentEquivalentFlatPaceFormatted = refFlatPaceStr,
                predictedTimeSeconds = predictedTimeSec,
                predictedTimeFormatted = predictedTimeStr,
                predictedPaceSecPerKm = predictedPaceSec,
                predictedPaceFormatted = predictedPaceStr,
                actualTimeSeconds = null,
                actualTimeFormatted = null,
                actualPaceFormatted = null,
                actualPoints = null,
                deltaSeconds = null,
                deltaFormatted = null,
                verdict = GaumeRacePronosticVerdict.UPCOMING,
                verdictLabel = "Pronostic : $predictedTimeStr ($predictedPaceStr)",
                verdictShortBadge = "Prévu ~$predictedTimeStr"
            )
        }

        val nextUpcoming = upcomingList.firstOrNull()

        return GaumeSeasonPronosticsSummary(
            distanceKm = selectedDistanceKm,
            distanceLabel = distSummary.distanceLabel,
            hasAssignedBib = distSummary.hasAssignedBib,
            recentEquivalentFlatPaceSecPerKm = currentRecentFlatPaceSec,
            recentEquivalentFlatPaceFormatted = refFlatPaceStr,
            recentEquivalentSpeedKmh = eqSpeedKmh,
            recentReferenceRaces = recentReferenceRaces,
            nextUpcomingPronostic = nextUpcoming,
            completedWithVerdict = emptyList(),
            upcomingPronostics = upcomingList,
            allPronosticsByRaceId = upcomingList.associateBy { it.raceId },
            betterThanExpectedCount = 0,
            onTargetCount = 0,
            slowerThanExpectedCount = 0,
            averageDeltaSeconds = 0
        )
    }

    fun computePersonalRecords(
        distanceSummary: DistanceChallengeSummary,
        racesWithContext: List<RaceItemWithContext>
    ): GaumePersonalRecordsSummary {
        val distKm = distanceSummary.distanceKm
        val completedPairs = racesWithContext.mapNotNull { item ->
            val res = item.resultForDistance(distKm)
            if (res != null) item to res else null
        }

        if (completedPairs.isEmpty()) {
            return GaumePersonalRecordsSummary(
                distanceLabel = distanceSummary.distanceLabel,
                completedCount = 0,
                fastestRaceRecord = null,
                bestPointsRecord = null,
                bestOverallRankRecord = null,
                bestCategoryRankRecord = null,
                biggestElevationRecord = null,
                regularityRecord = null
            )
        }

        val fastest = completedPairs.maxByOrNull { it.second.speedKmh }
        val fastestEntry = fastest?.let { (item, res) ->
            GaumePersonalRecordEntry(
                categoryTitle = "Record de Vitesse & Allure",
                mainValue = "${res.pacePerKmFormatted} • ${"%.2f".format(res.speedKmh)} km/h",
                secondaryValue = "Chrono : ${res.officialTimeFormatted} (${item.race.distanceKm} km)",
                villageAndRound = "${item.gaumeGeo.canonicalVillage} (Manche #${item.race.roundNumber})",
                dateIso = item.race.dateIso,
                accentColorHex = 0xFF1B5E3A
            )
        }

        val bestPts = completedPairs.maxByOrNull { it.second.challengePoints }
        val bestPtsEntry = bestPts?.let { (item, res) ->
            GaumePersonalRecordEntry(
                categoryTitle = "Record de Points Challenge",
                mainValue = "${res.challengePoints} pts / 1000",
                secondaryValue = "Chrono : ${res.officialTimeFormatted} • ${res.overallRank}e scratch",
                villageAndRound = "${item.gaumeGeo.canonicalVillage} (Manche #${item.race.roundNumber})",
                dateIso = item.race.dateIso,
                accentColorHex = 0xFFD49B1B
            )
        }

        val bestRank = completedPairs.minByOrNull { it.second.overallRank }
        val bestRankEntry = bestRank?.let { (item, res) ->
            val topPct = if (res.totalFinishers > 0) {
                ((res.overallRank.toDouble() / res.totalFinishers.toDouble()) * 100.0).roundToInt().coerceIn(1, 100)
            } else 10
            GaumePersonalRecordEntry(
                categoryTitle = "Meilleure Place Scratch",
                mainValue = "${res.overallRank}e / ${res.totalFinishers}",
                secondaryValue = "Top $topPct% du peloton • ${res.challengePoints} pts",
                villageAndRound = "${item.gaumeGeo.canonicalVillage} (Manche #${item.race.roundNumber})",
                dateIso = item.race.dateIso,
                accentColorHex = 0xFF006D77
            )
        }

        val bestCat = completedPairs.minByOrNull { it.second.categoryRank }
        val bestCatEntry = bestCat?.let { (item, res) ->
            GaumePersonalRecordEntry(
                categoryTitle = "Meilleure Place Catégorie",
                mainValue = "${res.categoryRank}e / ${res.categoryTotal} cat.",
                secondaryValue = "Chrono : ${res.officialTimeFormatted} (${res.pacePerKmFormatted})",
                villageAndRound = "${item.gaumeGeo.canonicalVillage} (Manche #${item.race.roundNumber})",
                dateIso = item.race.dateIso,
                accentColorHex = 0xFFC85A32
            )
        }

        val biggestElev = completedPairs.maxByOrNull { (item, _) ->
            item.activityMetrics?.elevationGainMeters ?: item.race.elevationGainMeters
        }
        val biggestElevEntry = biggestElev?.let { (item, res) ->
            val dPlus = item.activityMetrics?.elevationGainMeters ?: item.race.elevationGainMeters
            val ratioPerKm = if (item.race.distanceKm > 0) (dPlus / item.race.distanceKm).roundToInt() else 15
            GaumePersonalRecordEntry(
                categoryTitle = "Trophée Grimpeur (Max D+)",
                mainValue = "+$dPlus m D+",
                secondaryValue = "$ratioPerKm m D+/km • Finisher en ${res.officialTimeFormatted}",
                villageAndRound = "${item.gaumeGeo.canonicalVillage} (Manche #${item.race.roundNumber})",
                dateIso = item.race.dateIso,
                accentColorHex = 0xFF2B59C3
            )
        }

        val pacesSec = completedPairs.map { (item, res) ->
            val d = if (distKm == 5) 5.0 else item.race.distanceKm.coerceAtLeast(5.0)
            (res.officialTimeSeconds / d).roundToInt()
        }
        val minPace = pacesSec.minOrNull() ?: 270
        val maxPace = pacesSec.maxOrNull() ?: 270
        val spreadSec = (maxPace - minPace).coerceAtLeast(0)
        val regularityEntry = GaumePersonalRecordEntry(
            categoryTitle = "Régularité & Volume Saison",
            mainValue = "Moy. ${distanceSummary.averagePaceFormatted}",
            secondaryValue = "Écart max : ±${spreadSec / 2}s/km • ${"%.1f".format(distanceSummary.totalDistanceRunKm)} km cumulés",
            villageAndRound = "${completedPairs.size} manche(s) • +${distanceSummary.totalElevationGainMeters} m D+ total",
            dateIso = "Saison 2026",
            accentColorHex = 0xFF6A4C93
        )

        return GaumePersonalRecordsSummary(
            distanceLabel = distanceSummary.distanceLabel,
            completedCount = completedPairs.size,
            fastestRaceRecord = fastestEntry,
            bestPointsRecord = bestPtsEntry,
            bestOverallRankRecord = bestRankEntry,
            bestCategoryRankRecord = bestCatEntry,
            biggestElevationRecord = biggestElevEntry,
            regularityRecord = regularityEntry
        )
    }

    fun formatSecondsToMmSs(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val min = (safe % 3600) / 60
        val sec = safe % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, min, sec)
        } else {
            "%02d:%02d".format(min, sec)
        }
    }
}

/**
 * Bandeau synthétique affiché en haut de l'onglet « Courses » :
 * - Pronostic Avant-Course uniquement pour la prochaine manche à venir, basé sur les 3 dernières courses courues.
 */
@Composable
fun NextUpcomingPronosticBannerCard(
    pronosticsSummary: GaumeSeasonPronosticsSummary,
    onOpenPerfsTab: () -> Unit,
    onOpenRaceDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!pronosticsSummary.hasAssignedBib) return
    val nextRace = pronosticsSummary.nextUpcomingPronostic ?: return
    val recentVillagesLabel = pronosticsSummary.recentReferenceRaces.joinToString(", ") { it.villageName }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("next_race_pronostic_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Pronostic Prochaine Manche (${pronosticsSummary.distanceLabel})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenPerfsTab() }
                        .testTag("open_pronostics_hub_button")
                ) {
                    Text(
                        text = "Prochaines manches",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenRaceDetail(nextRace.raceId) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROCHAINE MANCHE • #${nextRace.roundNumber} ${nextRace.villageName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${"%.1f".format(nextRace.distanceKm)} km • +${nextRace.elevationGainMeters} m D+ • ${nextRace.dateIso}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Basé sur vos ${pronosticsSummary.recentReferenceRaces.size} dernière(s) course(s) ($recentVillagesLabel)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TEMPS PRÉVU",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = nextRace.predictedTimeFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = nextRace.predictedPaceFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SemoisRiverTeal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Badge compact affiché uniquement sur les cartes des prochaines manches à venir :
 * - Affiche le Temps Estimé selon l'allure équivalente des 3 dernières courses et le D+ du parcours
 */
@Composable
fun RacePronosticVsActualMiniBanner(
    entry: GaumeRacePronosticEntry,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pronostic avant-course (+${entry.elevationGainMeters} m D+)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "~${entry.predictedTimeFormatted} (${entry.predictedPaceFormatted})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

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
    seasonsSlot: (@Composable () -> Unit)? = null
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
            add(0 to "Pronostics")
            add(1 to "Records (${personalRecords.completedCount})")
            add(2 to "Carte Bilan")
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
                // Sous-vue 1 : Pronostics des prochaines manches à venir basés sur les 3 dernières courses
                GaumePronosticPrevuVsReelSectionCard(
                    runnerName = summary.profile.displayName,
                    pronosticsSummary = pronosticsSummary,
                    onOpenRaceDetail = onOpenRaceDetail
                )

                // Simulateur D+ & Boue en accordéon repliable pour ne pas alourdir l'écran
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
                                    text = "Simulateur manuel d'allure selon le D+ & la boue",
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
                // Sous-vue 3 : Carte Bilan Visuelle Partageable (PNG) & Export CSV/Texte
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

/**
 * Carte maîtresse : Pronostics des prochaines manches à venir basés exclusivement
 * sur l'historique récent (jusqu'aux 3 dernières courses courues).
 */
@Composable
fun GaumePronosticPrevuVsReelSectionCard(
    runnerName: String,
    pronosticsSummary: GaumeSeasonPronosticsSummary,
    onOpenRaceDetail: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pronostic_prevu_vs_reel_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Pronostics des Prochaines Manches (${pronosticsSummary.distanceLabel})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$runnerName • Calculés uniquement pour les courses à venir à partir de tes 3 dernières courses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (pronosticsSummary.recentReferenceRaces.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aucune course enregistrée sur le Challenge ${pronosticsSummary.distanceLabel} pour le moment. Dès que tu auras couru au moins 1 manche (synchronisée via MyChrono), l'application calculera automatiquement tes pronostics pour toutes les prochaines courses à venir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Encadré Allure Équivalente Récente + Nombre de courses récentes retenues
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Allure Équiv. Récente (Plat)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pronosticsSummary.recentEquivalentFlatPaceFormatted,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${pronosticsSummary.recentEquivalentSpeedKmh} km/h (corrigé D+)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = CountingTop15BadgeGreen.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, CountingTop15BadgeGreen.copy(alpha = 0.32f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Base Historique Récente",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${pronosticsSummary.recentReferenceRaces.size} dernière(s) course(s)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = CountingTop15BadgeGreen
                            )
                            Text(
                                text = "${pronosticsSummary.upcomingPronostics.size} manche(s) à venir",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Détail des 3 dernières courses servant de base de calcul
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Historique récent utilisé (${pronosticsSummary.recentReferenceRaces.size} dernière(s) course(s) courue(s)) :",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        pronosticsSummary.recentReferenceRaces.forEach { ref ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• M#${ref.roundNumber} ${ref.villageName} (+${ref.elevationGainMeters}m D+)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ref.officialTimeFormatted} (${ref.pacePerKmFormatted}) → ${ref.equivalentFlatPaceFormatted} plat",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 1. Prochaine manche à venir mise en avant
                val nextUpcoming = pronosticsSummary.nextUpcomingPronostic
                if (nextUpcoming != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenRaceDetail(nextUpcoming.raceId) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PROCHAINE MANCHE À VENIR (#${nextUpcoming.roundNumber})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = nextUpcoming.dateIso,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${nextUpcoming.villageName} • ${nextUpcoming.raceTitle}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Parcours : ${"%.1f".format(nextUpcoming.distanceKm)} km • +${nextUpcoming.elevationGainMeters} m D+ • Terrain ${nextUpcoming.terrainCondition}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = nextUpcoming.predictedTimeFormatted,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = nextUpcoming.predictedPaceFormatted,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SemoisRiverTeal
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Liste de toutes les prochaines manches à venir
                Text(
                    text = "Toutes les prochaines manches à venir (${pronosticsSummary.upcomingPronostics.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                pronosticsSummary.upcomingPronostics.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenRaceDetail(entry.raceId) }
                            .testTag("pronostic_upcoming_row_${entry.roundNumber}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "M#${entry.roundNumber} ${entry.villageName} • ${"%.1f".format(entry.distanceKm)} km",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${entry.dateIso} • +${entry.elevationGainMeters} m D+ • Terrain ${entry.terrainCondition}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "~${entry.predictedTimeFormatted}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = entry.predictedPaceFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SemoisRiverTeal
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
fun GaumePersonalRecordsSectionCard(
    runnerName: String,
    recordsSummary: GaumePersonalRecordsSummary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("personal_records_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFD49B1B),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Records Personnels de Gaume",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "$runnerName • Challenge ${recordsSummary.distanceLabel} (${recordsSummary.completedCount} course(s) officielle(s))",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (recordsSummary.completedCount == 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aucun record enregistré sur le Challenge ${recordsSummary.distanceLabel} pour le moment (0 course officielle courue sur cette distance).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val entries = listOfNotNull(
                    recordsSummary.fastestRaceRecord,
                    recordsSummary.bestPointsRecord,
                    recordsSummary.bestOverallRankRecord,
                    recordsSummary.bestCategoryRankRecord,
                    recordsSummary.biggestElevationRecord,
                    recordsSummary.regularityRecord
                )

                entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { rec ->
                            val accent = Color(rec.accentColorHex)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = accent.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.32f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = rec.categoryTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = rec.mainValue,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = rec.secondaryValue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = rec.villageAndRound,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GaumeElevationPacePredictorCard(
    summary: RunnerChallengeSummary,
    selectedDistanceKm: Int,
    activeDistanceSummary: DistanceChallengeSummary
) {
    val defaultBasePaceSec = remember(activeDistanceSummary, selectedDistanceKm) {
        val completed = summary.racesWithContext.mapNotNull {
            GaumePerfsCalculator.computeEquivalentFlatPaceSecPerKm(it, selectedDistanceKm)
        }
        if (completed.isNotEmpty()) {
            completed.takeLast(3).average().roundToInt().coerceIn(195, 450)
        } else {
            if (selectedDistanceKm == 5) 300 else 248
        }
    }

    var selectedCalendarRaceId by remember(summary.seasonYear) {
        val nextUpcoming = summary.racesWithContext.firstOrNull { !it.race.isCompleted }
            ?: summary.racesWithContext.firstOrNull()
        mutableStateOf(nextUpcoming?.race?.raceId.orEmpty())
    }

    val selectedCalendarItem = summary.racesWithContext.find { it.race.raceId == selectedCalendarRaceId }
        ?: summary.racesWithContext.firstOrNull()

    var baseFlatPaceSec by remember(defaultBasePaceSec) { mutableFloatStateOf(defaultBasePaceSec.toFloat()) }
    var targetElevationMeters by remember(selectedCalendarItem, selectedDistanceKm) {
        val rawElev = selectedCalendarItem?.race?.elevationGainMeters ?: 155
        val scaledElev = if (selectedDistanceKm == 5) (rawElev * 0.52).roundToInt() else rawElev
        mutableFloatStateOf(scaledElev.coerceIn(30, 350).toFloat())
    }
    var selectedTerrain by remember { mutableStateOf("Sec") }

    val effectiveDistanceKm = remember(selectedCalendarItem, selectedDistanceKm) {
        if (selectedDistanceKm == 5) {
            5.0
        } else {
            selectedCalendarItem?.race?.distanceKm?.takeIf { it > 3.0 } ?: 10.0
        }
    }

    val elevationAdditionalEffortKm = (targetElevationMeters / 100.0) * 0.78
    val elevationPenaltyTotalSec = (elevationAdditionalEffortKm * baseFlatPaceSec).roundToInt()

    val terrainMultiplier = when (selectedTerrain) {
        "Souple" -> 1.022
        "Boueux" -> 1.052
        "Très gras" -> 1.085
        else -> 1.0
    }

    val rawFlatTotalSec = (effectiveDistanceKm * baseFlatPaceSec).roundToInt()
    val predictedTotalSec = ((rawFlatTotalSec + elevationPenaltyTotalSec) * terrainMultiplier).roundToInt()
    val predictedPaceSecPerKm = (predictedTotalSec / effectiveDistanceKm).roundToInt()
    val totalReliefAndMudPenaltySec = (predictedTotalSec - rawFlatTotalSec).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pace_predictor_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Landscape,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Simulateur de Pronostic selon le D+ & la Boue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Ajustez votre allure équivalente ou la météo du jour (${activeDistanceSummary.distanceLabel})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "1. Choisir un village de Gaume (pré-remplit la distance et le D+) :",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                summary.racesWithContext.forEach { item ->
                    val isSelected = item.race.raceId == selectedCalendarRaceId
                    val dPlusDisplay = if (selectedDistanceKm == 5) {
                        (item.race.elevationGainMeters * 0.52).roundToInt()
                    } else {
                        item.race.elevationGainMeters
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedCalendarRaceId = item.race.raceId
                                targetElevationMeters = dPlusDisplay.coerceIn(30, 350).toFloat()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        }
                    ) {
                        Text(
                            text = "${item.gaumeGeo.canonicalVillage} (+${dPlusDisplay}m)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Allure équivalente récente (plat)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${GaumePerfsCalculator.formatSecondsToMmSs(baseFlatPaceSec.roundToInt())} /km",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = baseFlatPaceSec,
                    onValueChange = { baseFlatPaceSec = it },
                    valueRange = 195f..450f,
                    modifier = Modifier.testTag("predictor_base_pace_slider")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dénivelé positif du parcours (D+)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "+${targetElevationMeters.roundToInt()} m D+ (${"%.1f".format(effectiveDistanceKm)} km)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC85A32)
                    )
                }
                Slider(
                    value = targetElevationMeters,
                    onValueChange = { targetElevationMeters = it },
                    valueRange = 25f..350f,
                    modifier = Modifier.testTag("predictor_elevation_slider")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val terrains = listOf(
                    "Sec" to "Sec (+0%)",
                    "Souple" to "Souple (+2%)",
                    "Boueux" to "Boueux (+5%)",
                    "Très gras" to "Gras (+8%)"
                )
                terrains.forEach { (key, label) ->
                    val isSel = selectedTerrain == key
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTerrain = key },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) {
                            Color(0xFF1B5E3A)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CHRONO PRONOSTIQUÉ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = GaumePerfsCalculator.formatSecondsToMmSs(predictedTotalSec),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ALLURE CIBLE AJUSTÉE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${GaumePerfsCalculator.formatSecondsToMmSs(predictedPaceSecPerKm)} /km",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "Impact relief & terrain : +${GaumePerfsCalculator.formatSecondsToMmSs(totalReliefAndMudPenaltySec)} par rapport à un parcours plat sec (+${targetElevationMeters.roundToInt()} m D+, terrain ${selectedTerrain.lowercase()}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                    val splitMarkers = if (selectedDistanceKm == 5) {
                        listOf(1.0 to "Km 1", 2.5 to "Km 2,5", 4.0 to "Km 4", effectiveDistanceKm to "Arrivée")
                    } else {
                        listOf(2.0 to "Km 2", 5.0 to "Km 5", 8.0 to "Km 8", effectiveDistanceKm to "Arrivée")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        splitMarkers.forEach { (kmMark, label) ->
                            val splitSec = ((kmMark / effectiveDistanceKm) * predictedTotalSec).roundToInt()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = GaumePerfsCalculator.formatSecondsToMmSs(splitSec),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
fun GaumeShareableFinisherCard(
    summary: RunnerChallengeSummary,
    selectedDistanceKm: Int,
    pronosticsSummary: GaumeSeasonPronosticsSummary,
    preselectedShareRaceId: String?,
    onSelectShareRaceId: (String) -> Unit
) {
    val context = LocalContext.current
    val activeDistanceSummary = summary.summaryForDistance(selectedDistanceKm)
    val completedItems = remember(summary.racesWithContext, selectedDistanceKm) {
        summary.racesWithContext.filter { it.resultForDistance(selectedDistanceKm) != null }
    }

    var activeRaceId by remember(completedItems, preselectedShareRaceId) {
        val initial = if (!preselectedShareRaceId.isNullOrBlank() && completedItems.any { it.race.raceId == preselectedShareRaceId }) {
            preselectedShareRaceId
        } else {
            completedItems.lastOrNull()?.race?.raceId.orEmpty()
        }
        mutableStateOf(initial)
    }

    LaunchedEffect(preselectedShareRaceId) {
        if (!preselectedShareRaceId.isNullOrBlank() && completedItems.any { it.race.raceId == preselectedShareRaceId }) {
            activeRaceId = preselectedShareRaceId
        }
    }

    val selectedRaceItem = completedItems.find { it.race.raceId == activeRaceId }
        ?: completedItems.lastOrNull()
    val selectedResult = selectedRaceItem?.resultForDistance(selectedDistanceKm)
    val selectedPronostic = selectedRaceItem?.let { pronosticsSummary.allPronosticsByRaceId[it.race.raceId] }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shareable_finisher_card_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Carte Bilan Visuelle Partageable",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Générez et partagez votre fiche Finisher officielle avec tampon et bilan Pronostic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (completedItems.isEmpty() || selectedRaceItem == null || selectedResult == null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aucune course terminée sur le Challenge ${activeDistanceSummary.distanceLabel} pour générer une Carte Bilan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (completedItems.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        completedItems.forEach { item ->
                            val isSel = item.race.raceId == selectedRaceItem.race.raceId
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        activeRaceId = item.race.raceId
                                        onSelectShareRaceId(item.race.raceId)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                }
                            ) {
                                Text(
                                    text = "#${item.race.roundNumber} ${item.gaumeGeo.canonicalVillage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                val isCounting = selectedRaceItem.isCountingForDistance(selectedDistanceKm)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0E3520),
                                    Color(0xFF1B5E3A),
                                    Color(0xFF234E36)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ALLURES LIBRES DE GAUME ${summary.seasonYear}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD166)
                                )
                                Text(
                                    text = summary.profile.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFD166)
                            ) {
                                Text(
                                    text = "DOSSARD #${selectedResult.bibNumber} • ${selectedResult.distanceLabel.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF143622),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Manche #${selectedRaceItem.race.roundNumber} • ${selectedRaceItem.race.dateIso}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = selectedRaceItem.gaumeGeo.canonicalVillage,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${selectedRaceItem.gaumeGeo.territoryName} • +${selectedRaceItem.race.elevationGainMeters} m D+",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFD166)
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        rotate(-10f) {
                                            drawCircle(
                                                color = Color(0xFFFFD166),
                                                radius = size.minDimension / 2f - 3.dp.toPx(),
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                            drawCircle(
                                                color = Color(0xFFFFD166).copy(alpha = 0.7f),
                                                radius = size.minDimension / 2f - 8.dp.toPx(),
                                                style = Stroke(
                                                    width = 1.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                                                )
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "TAMPON",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFD166)
                                        )
                                        Text(
                                            text = selectedRaceItem.gaumeGeo.canonicalVillage.take(8).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FinisherMetricBox(
                                label = "CHRONO",
                                value = selectedResult.officialTimeFormatted,
                                sub = selectedResult.distanceLabel,
                                modifier = Modifier.weight(1f)
                            )
                            FinisherMetricBox(
                                label = "ALLURE",
                                value = selectedResult.pacePerKmFormatted,
                                sub = "${"%.2f".format(selectedResult.speedKmh)} km/h",
                                modifier = Modifier.weight(1f)
                            )
                            FinisherMetricBox(
                                label = "CLASSEMENT",
                                value = "${selectedResult.overallRank}e",
                                sub = "sur ${selectedResult.totalFinishers}",
                                modifier = Modifier.weight(1f)
                            )
                            FinisherMetricBox(
                                label = "CHALLENGE",
                                value = "${selectedResult.challengePoints} pts",
                                sub = if (isCounting) "Top 15 Actif" else "Saison ${summary.seasonYear}",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (selectedPronostic != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.28f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pronostic avant-course : ${selectedPronostic.predictedTimeFormatted}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = selectedPronostic.verdictLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFD166)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Score Top 15 (${activeDistanceSummary.distanceLabel}) : ${activeDistanceSummary.totalChallengePoints} pts (${activeDistanceSummary.countingRacesCount}/15 courses)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFFFFD166),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareFinisherCardAsPngAndText(
                                context = context,
                                summary = summary,
                                distanceSummary = activeDistanceSummary,
                                raceItem = selectedRaceItem,
                                pronosticEntry = selectedPronostic,
                                selectedDistanceKm = selectedDistanceKm
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_finisher_png_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager la Carte (PNG)")
                    }
                }
            }
        }
    }
}

@Composable
private fun FinisherMetricBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.24f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD166),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Carte d'export Bilan de Saison (CSV / Texte partageable) avec les pronostics et écarts Prévu vs Réel.
 */
@Composable
fun GaumeSeasonCsvExportCard(
    summary: RunnerChallengeSummary,
    distanceSummary: DistanceChallengeSummary,
    pronosticsSummary: GaumeSeasonPronosticsSummary
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("season_csv_export_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Exporter mon Bilan de Saison (CSV / Texte)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Partagez vos résultats officiels, vos pronostics et vos écarts Prévu vs Réel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val exportText = buildString {
                        appendLine("=== BILAN CHALLENGE DES ALLURES LIBRES DE GAUME ${summary.seasonYear} ===")
                        appendLine("Coureur : ${summary.profile.displayName} (Challenge ${distanceSummary.distanceLabel} • Dossard #${distanceSummary.assignedBibNumber})")
                        appendLine("Score Top 15 : ${distanceSummary.totalChallengePoints} pts (${distanceSummary.countingRacesCount}/15 courses comptabilisées)")
                        appendLine("Allure équivalente récente (plat) : ${pronosticsSummary.recentEquivalentFlatPaceFormatted}")
                        appendLine()
                        appendLine("Manche;Village;Date;Distance_km;D+_m;Pronostic;Chrono_Reel;Ecart;Verdict;Points_ALG")
                        pronosticsSummary.completedWithVerdict.sortedBy { it.roundNumber }.forEach { e ->
                            appendLine(
                                "#${e.roundNumber};${e.villageName};${e.dateIso};${"%.1f".format(e.distanceKm)};+${e.elevationGainMeters};${e.predictedTimeFormatted};${e.actualTimeFormatted ?: "-"};${e.deltaFormatted ?: "-"};${e.verdictLabel};${e.actualPoints ?: 0}"
                            )
                        }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Bilan Gaume ${summary.seasonYear} - ${summary.profile.displayName}")
                        putExtra(Intent.EXTRA_TEXT, exportText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Exporter mon bilan Gaume"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_season_csv_button")
            ) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exporter Résultats & Pronostics (${distanceSummary.distanceLabel})")
            }
        }
    }
}

fun shareFinisherCardAsPngAndText(
    context: Context,
    summary: RunnerChallengeSummary,
    distanceSummary: DistanceChallengeSummary,
    raceItem: RaceItemWithContext,
    pronosticEntry: GaumeRacePronosticEntry?,
    selectedDistanceKm: Int
) {
    val result = raceItem.resultForDistance(selectedDistanceKm) ?: return
    val shareText = buildString {
        appendLine("🏃 Challenge des Allures Libres de Gaume ${summary.seasonYear}")
        appendLine("📍 ${raceItem.gaumeGeo.canonicalVillage} (Manche #${raceItem.race.roundNumber} • ${result.distanceLabel})")
        appendLine("👤 ${summary.profile.displayName} (Dossard #${result.bibNumber})")
        appendLine("⏱️ Chrono : ${result.officialTimeFormatted} (${result.pacePerKmFormatted} • ${"%.2f".format(result.speedKmh)} km/h)")
        if (pronosticEntry != null) {
            appendLine("🎯 Pronostic : ${pronosticEntry.predictedTimeFormatted} → ${pronosticEntry.verdictLabel}")
        }
        appendLine("🏆 Classement : ${result.overallRank}e / ${result.totalFinishers} (${result.challengePoints} pts)")
        append("🌲 Score Top 15 (${distanceSummary.distanceLabel}) : ${distanceSummary.totalChallengePoints} pts (${distanceSummary.countingRacesCount}/15 courses)")
    }

    runCatching {
        val width = 1080
        val height = 1320
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF0E3520.toInt(), 0xFF1B5E3A.toInt(), 0xFF123D26.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = 0xFFFFD166.toInt()
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 42f, 42f, borderPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD166.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ALLURES LIBRES DE GAUME • SAISON ${summary.seasonYear}", 84f, 120f, goldPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(summary.profile.displayName, 84f, 205f, namePaint)

        val bibSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD8F3DC.toInt()
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Dossard #${result.bibNumber}  •  Challenge ${result.distanceLabel}  •  ${summary.profile.clubName}", 84f, 265f, bibSubPaint)

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x26FFFFFF
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(76f, 320f, width - 76f, 600f), 36f, 36f, boxPaint)

        val villagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 68f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(raceItem.gaumeGeo.canonicalVillage, 112f, 430f, villagePaint)

        val fiefPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD166.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Manche #${raceItem.race.roundNumber} (${raceItem.race.dateIso}) • ${raceItem.gaumeGeo.territoryName}", 112f, 495f, fiefPaint)
        canvas.drawText("Parcours : ${raceItem.race.distanceKm} km  •  Dénivelé : +${raceItem.race.elevationGainMeters} m D+", 112f, 555f, fiefPaint)

        val stampCenterX = width - 220f
        val stampCenterY = 460f
        val stampRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD166.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(stampCenterX, stampCenterY, 96f, stampRingPaint)
        stampRingPaint.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
        canvas.drawCircle(stampCenterX, stampCenterY, 80f, stampRingPaint)

        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD166.toInt()
            textSize = 26f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("FINISHER", stampCenterX, stampCenterY - 10f, stampTextPaint)
        canvas.drawText("GAUME #${raceItem.race.roundNumber}", stampCenterX, stampCenterY + 26f, stampTextPaint)

        val metrics = listOf(
            Triple("CHRONO OFFICIEL", result.officialTimeFormatted, "Challenge ${result.distanceLabel}"),
            Triple("ALLURE & VITESSE", result.pacePerKmFormatted, "${"%.2f".format(result.speedKmh)} km/h"),
            Triple("CLASSEMENT", "${result.overallRank}e / ${result.totalFinishers}", "Catégorie : ${result.categoryRank}e"),
            Triple("POINTS CHALLENGE", "${result.challengePoints} pts", "Sur 1000 pts")
        )

        metrics.forEachIndexed { idx, (mLabel, mVal, mSub) ->
            val col = idx % 2
            val row = idx / 2
            val left = if (col == 0) 76f else 556f
            val right = if (col == 0) 524f else width - 76f
            val top = if (row == 0) 640f else 875f
            val bottom = top + 200f

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x38000000
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(left, top, right, bottom), 28f, 28f, cardPaint)

            val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFB7E4C7.toInt()
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(mLabel, left + 32f, top + 52f, lblPaint)

            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFD166.toInt()
                textSize = 54f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(mVal, left + 32f, top + 125f, valPaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 28f
            }
            canvas.drawText(mSub, left + 32f, top + 172f, subPaint)
        }

        if (pronosticEntry != null) {
            val pronoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFD166.toInt()
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                "Pronostic : ${pronosticEntry.predictedTimeFormatted}  →  ${pronosticEntry.verdictLabel}",
                84f,
                1140f,
                pronoPaint
            )
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "Bilan Top 15 (${distanceSummary.distanceLabel}) : ${distanceSummary.totalChallengePoints} pts  •  ${distanceSummary.countingRacesCount}/15 courses comptabilisées",
            84f,
            1220f,
            footerPaint
        )

        val sharedDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val outFile = File(sharedDir, "bilan_gaume_${raceItem.race.raceId}_${selectedDistanceKm}km.png")
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager ma Carte Bilan Gaume"))
    }.onFailure {
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(textIntent, "Partager mon résultat Gaume"))
    }
}
