package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CourseDifficultyCalculator
import com.example.domain.FormPoint
import com.example.domain.RunnerChallengeSummary
import com.example.domain.SeasonInsightsCalculator
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen

/**
 * Forme de la saison : allure de chaque course ramenée à un parcours moyen
 * (allure ÷ indice de difficulté du parcours, calculé sur les coureurs réguliers).
 */
@Composable
fun FormAndCourseCard(
    summary: RunnerChallengeSummary,
    selectedDistanceKm: Int,
    onOpenRaceDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val points = remember(summary, selectedDistanceKm) {
        SeasonInsightsCalculator.formPoints(summary, selectedDistanceKm)
    }
    val form = remember(points) { SeasonInsightsCalculator.currentFormPace(points) }
    val label = summary.challengeForDistance(selectedDistanceKm).distanceLabel

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("form_and_course_card"),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Forme ${summary.seasonYear} • $label",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Allure ramenée à un parcours moyen de la saison",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (form != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = SeasonInsightsCalculator.formatPace(form),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "forme actuelle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (points.size < 2) {
                Text(
                    text = if (points.isEmpty()) "Aucune course courue sur le $label cette saison."
                    else "Une seule course courue : la courbe apparaîtra dès la deuxième.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FormChart(points = points)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendItem(MaterialTheme.colorScheme.primary, "allure corrigée du parcours", filled = true)
                    LegendItem(MaterialTheme.colorScheme.onSurfaceVariant, "allure brute", filled = false)
                }
                Text(
                    text = "Plus haut = plus rapide. L'indice d'un parcours compare l'allure des coureurs réguliers " +
                        "ce jour-là à leur allure habituelle : il tient compte du dénivelé, du terrain et de la météo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (points.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                points.asReversed().forEach { p ->
                    FormRow(point = p, onClick = { onOpenRaceDetail(p.raceId) })
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = if (filled) color else Color.Transparent,
            border = if (filled) null else BorderStroke(1.5.dp, color),
            modifier = Modifier.size(9.dp)
        ) {}
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormChart(points: List<FormPoint>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val rawColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val corrected = points.map { (it.correctedPaceSecPerKm ?: it.paceSecPerKm).toFloat() }
    val raw = points.map { it.paceSecPerKm.toFloat() }
    val minPace = (corrected + raw).min()
    val maxPace = (corrected + raw).max()
    val span = (maxPace - minPace).coerceAtLeast(10f)

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = SeasonInsightsCalculator.formatPace(minPace.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${points.first().dateIso.substring(5)} → ${points.last().dateIso.substring(5)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .testTag("form_chart")
        ) {
            val padX = 10.dp.toPx()
            val padY = 8.dp.toPx()
            val w = size.width - 2 * padX
            val h = size.height - 2 * padY
            fun x(i: Int) = padX + if (points.size == 1) w / 2 else w * i / (points.size - 1)
            // Allure la plus rapide en haut.
            fun y(pace: Float) = padY + h * (pace - minPace) / span

            for (k in 0..3) {
                val gy = padY + h * k / 3f
                drawLine(gridColor, Offset(padX, gy), Offset(padX + w, gy), strokeWidth = 1f)
            }

            raw.forEachIndexed { i, p ->
                drawCircle(rawColor, radius = 3.5.dp.toPx(), center = Offset(x(i), y(p)), style = Stroke(width = 1.5.dp.toPx()))
            }

            val path = Path()
            corrected.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(x(i), y(p)) else path.lineTo(x(i), y(p))
            }
            drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx()))
            corrected.forEachIndexed { i, p ->
                drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x(i), y(p)))
            }
        }
        Text(
            text = SeasonInsightsCalculator.formatPace(maxPace.toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FormRow(point: FormPoint, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = point.village,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${point.dateIso} • ${point.timeFormatted} • ${SeasonInsightsCalculator.formatPace(point.paceSecPerKm)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = point.correctedPaceSecPerKm?.let { SeasonInsightsCalculator.formatPace(it) } ?: "—",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = point.factor?.let { CourseDifficultyCalculator.formatFactor(it) } ?: "parcours sans indice",
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    point.factor == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    point.factor > 1.0 -> ClayTerracotta
                    else -> CountingTop15BadgeGreen
                }
            )
        }
    }
}
