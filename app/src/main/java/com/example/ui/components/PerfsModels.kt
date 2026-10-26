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
                mainValue = "${res.categoryRank}e" + (if (res.categoryTotal > 0) " / ${res.categoryTotal}" else "") + " cat.",
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
