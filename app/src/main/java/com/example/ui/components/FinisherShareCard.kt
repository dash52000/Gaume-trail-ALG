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
