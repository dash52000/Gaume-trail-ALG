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
    val elevationGainMeters: Int?,
    val terrainCondition: String?,
    val recentAveragePaceSecPerKm: Int,
    val recentAveragePaceFormatted: String,
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
    val elevationGainMeters: Int?,
    val paceUsedFormatted: String
)

/**
 * Synthèse complète des pronostics avant-course (uniquement pour les prochaines manches à venir)
 * calculés à partir des 3 dernières courses courues.
 */
data class GaumeSeasonPronosticsSummary(
    val distanceKm: Int,
    val distanceLabel: String,
    val hasAssignedBib: Boolean,
    val recentAveragePaceSecPerKm: Int,
    val recentAveragePaceFormatted: String,
    val recentAverageSpeedKmh: Double,
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

    /** Libellé de D+ : jamais de valeur inventée. */
    fun dPlusLabel(meters: Int?): String = meters?.let { "+$it m D+" } ?: "D+ inconnu"

    /** Longueur réelle publiée par MyChrono pour la distance du challenge, ou null si non publiée. */
    fun realDistanceKm(item: RaceItemWithContext, selectedDistanceKm: Int): Double? =
        item.race.realDistanceKmFor(selectedDistanceKm)

    /** D+ connu pour la distance : mesuré sur la trace GPX importée, sinon publié pour la manche (10 km). */
    fun knownElevation(item: RaceItemWithContext, selectedDistanceKm: Int): Int? =
        AlgChallengeCalculator.knownElevationForRace(item.race, item.activityMetrics, selectedDistanceKm)

    /** Allure réelle d'une manche courue (temps officiel / longueur réelle du parcours), en s/km. */
    fun actualPaceSecPerKm(item: RaceItemWithContext, selectedDistanceKm: Int): Int? {
        val res = item.resultForDistance(selectedDistanceKm) ?: return null
        val km = AlgChallengeCalculator.realKmOfResult(res, item.race, selectedDistanceKm)
        if (km <= 0.5) return null
        return (res.officialTimeSeconds / km).roundToInt()
    }

    /**
     * Allure « km-effort » (convention ITRA : 100 m de D+ comptent comme 1 km) d'une manche courue,
     * seulement si son D+ est connu (trace GPX). Sinon null.
     */
    fun kmEffortPaceSecPerKm(item: RaceItemWithContext, selectedDistanceKm: Int): Int? {
        val res = item.resultForDistance(selectedDistanceKm) ?: return null
        val dPlus = knownElevation(item, selectedDistanceKm) ?: return null
        val km = AlgChallengeCalculator.realKmOfResult(res, item.race, selectedDistanceKm)
        val kmEffort = km + dPlus / 100.0
        if (kmEffort <= 0.5) return null
        return (res.officialTimeSeconds / kmEffort).roundToInt()
    }

    /**
     * Pronostics des prochaines manches.
     * Forme = médiane des allures corrigées du parcours des 3 dernières courses courues
     * (allure ÷ indice de difficulté du parcours, calculé sur le peloton).
     * Allure prévue = forme × indice du parcours à venir (celui de son édition précédente),
     * temps prévu = allure prévue × longueur réelle publiée. Sans indice connu, le parcours est
     * traité comme un parcours moyen, et c'est indiqué.
     * En début de saison, sans course courue, la forme de fin de saison précédente sert de base.
     */
    fun computeSeasonPronostics(
        summary: RunnerChallengeSummary,
        selectedDistanceKm: Int
    ): GaumeSeasonPronosticsSummary {
        val distSummary = summary.challengeForDistance(selectedDistanceKm)
        val sortedItems = summary.racesWithContext.sortedWith(
            compareBy<RaceItemWithContext>({ it.race.dateIso }, { it.race.roundNumber })
        )
        val formPoints = com.example.domain.SeasonInsightsCalculator.formPoints(summary, selectedDistanceKm)
        val recentLast3 = formPoints.takeLast(3)
        val formPace = com.example.domain.SeasonInsightsCalculator.currentFormPace(formPoints)
            ?: summary.fallbackFormPaceForDistance(selectedDistanceKm)

        if (formPace == null || formPace <= 0) {
            return GaumeSeasonPronosticsSummary(
                distanceKm = selectedDistanceKm,
                distanceLabel = distSummary.distanceLabel,
                hasAssignedBib = distSummary.hasAssignedBib,
                recentAveragePaceSecPerKm = 0,
                recentAveragePaceFormatted = "--:-- /km",
                recentAverageSpeedKmh = 0.0,
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

        val formPaceStr = "${formatSecondsToMmSs(formPace)} /km"
        val formSpeedKmh = ((3600.0 / formPace) * 10.0).roundToInt() / 10.0
        val itemsById = sortedItems.associateBy { it.race.raceId }

        val recentReferenceRaces = recentLast3.reversed().map { p ->
            GaumeRecentReferenceRace(
                raceId = p.raceId,
                roundNumber = p.roundNumber,
                villageName = p.village,
                dateIso = p.dateIso,
                officialTimeFormatted = p.timeFormatted,
                pacePerKmFormatted = "${formatSecondsToMmSs(p.paceSecPerKm)} /km",
                elevationGainMeters = itemsById[p.raceId]?.let { knownElevation(it, selectedDistanceKm) },
                paceUsedFormatted = "${formatSecondsToMmSs(p.correctedPaceSecPerKm ?: p.paceSecPerKm)} /km" +
                    (p.factor?.let { " (parcours ${com.example.domain.CourseDifficultyCalculator.formatFactor(it)})" } ?: " (parcours sans indice)")
            )
        }

        val lastRunDate = formPoints.lastOrNull()?.dateIso ?: ""
        val upcomingItems = sortedItems.filter { targetItem ->
            targetItem.resultForDistance(selectedDistanceKm) == null &&
                !targetItem.race.isCompleted &&
                targetItem.race.dateIso >= lastRunDate
        }

        val upcomingList = upcomingItems.mapNotNull { targetItem ->
            // Sans longueur publiée, pas de pronostic (on ne suppose pas « 5 km » ou « 10 km » pile).
            val realKm = realDistanceKm(targetItem, selectedDistanceKm) ?: return@mapNotNull null
            val factor = targetItem.bestKnownFactorForDistance(selectedDistanceKm)
            val predictedPace = (formPace * (factor ?: 1.0)).roundToInt()
            val predictedTimeSec = (predictedPace * realKm).roundToInt()
            val predictedTimeStr = formatSecondsToMmSs(predictedTimeSec)
            val predictedPaceStr = "${formatSecondsToMmSs(predictedPace)} /km"
            val factorLabel = factor?.let { com.example.domain.CourseDifficultyCalculator.formatFactor(it) }

            GaumeRacePronosticEntry(
                raceId = targetItem.race.raceId,
                roundNumber = targetItem.race.roundNumber,
                villageName = targetItem.gaumeGeo.canonicalVillage,
                raceTitle = targetItem.race.title,
                dateIso = targetItem.race.dateIso,
                distanceKm = realKm,
                elevationGainMeters = knownElevation(targetItem, selectedDistanceKm),
                terrainCondition = targetItem.terrainWeather?.terrainCondition?.ifBlank { null },
                recentAveragePaceSecPerKm = formPace,
                recentAveragePaceFormatted = formPaceStr,
                predictedTimeSeconds = predictedTimeSec,
                predictedTimeFormatted = predictedTimeStr,
                predictedPaceSecPerKm = predictedPace,
                predictedPaceFormatted = predictedPaceStr,
                actualTimeSeconds = null,
                actualTimeFormatted = null,
                actualPaceFormatted = null,
                actualPoints = null,
                deltaSeconds = null,
                deltaFormatted = null,
                verdict = GaumeRacePronosticVerdict.UPCOMING,
                verdictLabel = "Pronostic : $predictedTimeStr ($predictedPaceStr) • " +
                    (factorLabel?.let { "parcours $it" } ?: "parcours sans indice connu"),
                verdictShortBadge = "Prévu ~$predictedTimeStr"
            )
        }

        return GaumeSeasonPronosticsSummary(
            distanceKm = selectedDistanceKm,
            distanceLabel = distSummary.distanceLabel,
            hasAssignedBib = distSummary.hasAssignedBib,
            recentAveragePaceSecPerKm = formPace,
            recentAveragePaceFormatted = formPaceStr,
            recentAverageSpeedKmh = formSpeedKmh,
            recentReferenceRaces = recentReferenceRaces,
            nextUpcomingPronostic = upcomingList.firstOrNull(),
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
                secondaryValue = "Chrono : ${res.officialTimeFormatted} (${"%.1f".format(AlgChallengeCalculator.realKmOfResult(res, item.race, distKm))} km)",
                villageAndRound = "${item.gaumeGeo.canonicalVillage} (Manche #${item.race.roundNumber})",
                dateIso = item.race.dateIso,
                accentColorHex = 0xFF1B5E3A
            )
        }

        val bestPts = completedPairs.maxByOrNull { it.second.challengePoints }
        val bestPtsEntry = bestPts?.let { (item, res) ->
            GaumePersonalRecordEntry(
                categoryTitle = "Record de Points Challenge",
                mainValue = "${res.challengePoints} pts / 1200",
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
            } else null
            GaumePersonalRecordEntry(
                categoryTitle = "Meilleure Place Scratch",
                mainValue = "${res.overallRank}e / ${res.totalFinishers}",
                secondaryValue = (topPct?.let { "Top $it% du peloton • " } ?: "") + "${res.challengePoints} pts",
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

        // Seulement les manches dont le D+ est réellement connu (trace GPX importée).
        val biggestElev = completedPairs
            .mapNotNull { pair -> knownElevation(pair.first, distKm)?.let { pair to it } }
            .maxByOrNull { it.second }
        val biggestElevEntry = biggestElev?.let { (pair, dPlus) ->
            val (item, res) = pair
            val km = AlgChallengeCalculator.realKmOfResult(res, item.race, distKm)
            val ratioPerKm = if (km > 0) (dPlus / km).roundToInt() else null
            GaumePersonalRecordEntry(
                categoryTitle = "Trophée Grimpeur (Max D+)",
                mainValue = "+$dPlus m D+",
                secondaryValue = (ratioPerKm?.let { "$it m D+/km • " } ?: "") + "Finisher en ${res.officialTimeFormatted}",
                villageAndRound = "${item.gaumeGeo.canonicalVillage} (Manche #${item.race.roundNumber})",
                dateIso = item.race.dateIso,
                accentColorHex = 0xFF2B59C3
            )
        }

        val pacesSec = completedPairs.mapNotNull { (item, _) -> actualPaceSecPerKm(item, distKm) }
        val minPace = pacesSec.minOrNull() ?: 0
        val maxPace = pacesSec.maxOrNull() ?: 0
        val spreadSec = (maxPace - minPace).coerceAtLeast(0)
        val regularityEntry = GaumePersonalRecordEntry(
            categoryTitle = "Régularité & Volume Saison",
            mainValue = "Moy. ${distanceSummary.averagePaceFormatted}",
            secondaryValue = "Écart max : ±${spreadSec / 2}s/km • ${"%.1f".format(distanceSummary.totalDistanceRunKm)} km cumulés",
            villageAndRound = "${completedPairs.size} manche(s)" + if (distanceSummary.elevationKnownRacesCount > 0) {
                " • +${distanceSummary.totalElevationGainMeters} m D+ (${distanceSummary.elevationKnownRacesCount} trace(s) GPX)"
            } else "",
            dateIso = "Saison ${completedPairs.first().first.race.seasonYear}",
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
                            text = "${"%.1f".format(nextRace.distanceKm)} km • ${GaumePerfsCalculator.dPlusLabel(nextRace.elevationGainMeters)} • ${nextRace.dateIso}",
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
                text = "Pronostic avant-course (${"%.1f".format(entry.distanceKm)} km)",
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
                        text = "$runnerName • Ta forme (3 dernières courses, corrigées du parcours) × l'indice du parcours à venir",
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
                        text = "Aucune course courue sur le Challenge ${pronosticsSummary.distanceLabel} pour le moment, ni la saison précédente. Les pronostics apparaîtront après ta première manche.",
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
                                text = "Forme (parcours moyen)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pronosticsSummary.recentAveragePaceFormatted,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${pronosticsSummary.recentAverageSpeedKmh} km/h (médiane des 3 dernières)",
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
                                    text = "• M#${ref.roundNumber} ${ref.villageName} (${GaumePerfsCalculator.dPlusLabel(ref.elevationGainMeters)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ref.officialTimeFormatted} (${ref.paceUsedFormatted})",
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
                                        text = "Parcours : ${"%.1f".format(nextUpcoming.distanceKm)} km • ${GaumePerfsCalculator.dPlusLabel(nextUpcoming.elevationGainMeters)}" +
                                            (nextUpcoming.terrainCondition?.let { " • Terrain $it" } ?: ""),
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
                                    text = "${entry.dateIso} • ${GaumePerfsCalculator.dPlusLabel(entry.elevationGainMeters)}" +
                                        (entry.terrainCondition?.let { " • Terrain $it" } ?: ""),
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
    // Allure de base : 3 dernières courses courues. Allure km-effort si leur D+ est connu (GPX), sinon allure réelle.
    val recentBase = remember(summary, selectedDistanceKm) {
        val completed = summary.racesWithContext
            .sortedBy { it.race.roundNumber }
            .mapNotNull { item ->
                val effort = GaumePerfsCalculator.kmEffortPaceSecPerKm(item, selectedDistanceKm)
                val actual = GaumePerfsCalculator.actualPaceSecPerKm(item, selectedDistanceKm)
                when {
                    effort != null -> effort to true
                    actual != null -> actual to false
                    else -> null
                }
            }
            .takeLast(3)
        if (completed.isEmpty()) null
        else completed.map { it.first }.average().roundToInt() to completed.count { it.second }
    }
    val defaultBasePaceSec = recentBase?.first ?: if (selectedDistanceKm == 5) 330 else 300

    var selectedCalendarRaceId by remember(summary.seasonYear) {
        val nextUpcoming = summary.racesWithContext.firstOrNull { !it.race.isCompleted }
            ?: summary.racesWithContext.firstOrNull()
        mutableStateOf(nextUpcoming?.race?.raceId.orEmpty())
    }

    val selectedCalendarItem = summary.racesWithContext.find { it.race.raceId == selectedCalendarRaceId }
        ?: summary.racesWithContext.firstOrNull()

    var basePaceSec by remember(defaultBasePaceSec) { mutableFloatStateOf(defaultBasePaceSec.toFloat()) }
    var targetElevationMeters by remember(selectedCalendarItem, selectedDistanceKm) {
        val known = selectedCalendarItem?.let { GaumePerfsCalculator.knownElevation(it, selectedDistanceKm) }
        mutableFloatStateOf((known ?: 0).coerceIn(0, 400).toFloat())
    }

    val publishedDistanceKm = selectedCalendarItem?.let { GaumePerfsCalculator.realDistanceKm(it, selectedDistanceKm) }
    val effectiveDistanceKm = publishedDistanceKm ?: selectedDistanceKm.toDouble()

    // Convention km-effort (ITRA) : 100 m de D+ comptent comme 1 km de plat.
    val kmEffort = effectiveDistanceKm + targetElevationMeters / 100.0
    val predictedTotalSec = (kmEffort * basePaceSec).roundToInt()
    val predictedPaceSecPerKm = (predictedTotalSec / effectiveDistanceKm).roundToInt()
    val reliefPenaltySec = ((targetElevationMeters / 100.0) * basePaceSec).roundToInt()

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
                        text = "Simulateur selon le dénivelé",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Convention km-effort (ITRA) : 100 m de D+ = 1 km (${activeDistanceSummary.distanceLabel})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "1. Choisir une manche (pré-remplit la distance réelle) :",
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
                    val km = GaumePerfsCalculator.realDistanceKm(item, selectedDistanceKm)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedCalendarRaceId = item.race.raceId
                                targetElevationMeters =
                                    (GaumePerfsCalculator.knownElevation(item, selectedDistanceKm) ?: 0).coerceIn(0, 400).toFloat()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        }
                    ) {
                        Text(
                            text = "${item.gaumeGeo.canonicalVillage} (${km?.let { "%.1f km".format(it) } ?: "? km"})",
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
                        text = if (recentBase != null) "Allure de base (3 dernières courses)" else "Allure de base (à régler)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${GaumePerfsCalculator.formatSecondsToMmSs(basePaceSec.roundToInt())} /km",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = basePaceSec,
                    onValueChange = { basePaceSec = it },
                    valueRange = 150f..720f,
                    modifier = Modifier.testTag("predictor_base_pace_slider")
                )
                if (recentBase != null && recentBase.second < 3) {
                    Text(
                        text = "Le D+ des courses de référence n'est connu que via les traces GPX importées : " +
                            "sans trace, leur allure inclut déjà leur relief.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "D+ estimé du parcours (à renseigner)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "+${targetElevationMeters.roundToInt()} m D+ (" +
                            (publishedDistanceKm?.let { "%.1f km".format(it) } ?: "distance non publiée : ${selectedDistanceKm} km") + ")",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC85A32)
                    )
                }
                Slider(
                    value = targetElevationMeters,
                    onValueChange = { targetElevationMeters = it },
                    valueRange = 0f..400f,
                    modifier = Modifier.testTag("predictor_elevation_slider")
                )
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
                                text = "CHRONO ESTIMÉ",
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
                                text = "ALLURE MOYENNE",
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
                        text = "Effet du relief : +${GaumePerfsCalculator.formatSecondsToMmSs(reliefPenaltySec)} " +
                            "(${"%.1f".format(kmEffort)} km-effort). Le terrain et la météo ne sont pas modélisés.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
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
                        text = "Générez et partagez votre fiche Finisher avec tampon et bilan Pronostic",
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
                                        text = "${selectedRaceItem.gaumeGeo.territoryName} • ${GaumePerfsCalculator.dPlusLabel(GaumePerfsCalculator.knownElevation(selectedRaceItem, selectedDistanceKm))}",
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
                        text = "Exporter mes courses (CSV)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Une ligne par course : chrono, allure, indice du parcours, allure corrigée, points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val exportText = buildString {
                        val points = com.example.domain.SeasonInsightsCalculator.formPoints(summary, distanceSummary.distanceKm)
                        appendLine("Saison;Manche;Date;Village;Course;Distance_km;Chrono;Allure_s_km;Indice_parcours;Allure_corrigee_s_km;Points;Compte_Top15")
                        points.forEach { p ->
                            appendLine(
                                listOf(
                                    summary.seasonYear,
                                    p.roundNumber,
                                    p.dateIso,
                                    p.village,
                                    p.raceTitle.replace(';', ','),
                                    "%.2f".format(java.util.Locale.ROOT, p.distanceKm),
                                    p.timeFormatted,
                                    p.paceSecPerKm,
                                    p.factor?.let { "%.3f".format(java.util.Locale.ROOT, it) } ?: "",
                                    p.correctedPaceSecPerKm ?: "",
                                    p.points,
                                    if (p.raceId in distanceSummary.countingRaceIds) "oui" else "non"
                                ).joinToString(";")
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
                Text("Exporter le ${distanceSummary.distanceLabel} ${summary.seasonYear} (CSV)")
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
        val shareDistKm = if (result.distanceLabel == "5 km") 5 else 10
        val shareKm = AlgChallengeCalculator.realKmOfResult(result, raceItem.race, shareDistKm)
        val shareDPlus = AlgChallengeCalculator.knownElevationForRace(raceItem.race, raceItem.activityMetrics, shareDistKm)
        canvas.drawText(
            "Parcours : ${"%.1f".format(shareKm)} km" + (shareDPlus?.let { "  •  Dénivelé : +$it m D+" } ?: ""),
            112f, 555f, fiefPaint
        )

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
            Triple("POINTS CHALLENGE", "${result.challengePoints} pts", "Sur 1200 pts")
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
