package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.RaceWeatherEntity
import com.example.data.remote.OpenMeteoClient
import com.example.domain.CourseDifficultyCalculator
import com.example.domain.CourseRecord
import com.example.domain.GoalsCalculator
import com.example.domain.RecordStatus
import androidx.compose.material.icons.filled.EmojiEvents
import com.example.domain.RaceItemWithContext

/** Résumé météo réel (Open-Meteo) : « 9,4 °C à 10 h • Pluie • 12 mm sur les 3 jours précédents ». */
fun weatherSummary(w: RaceWeatherEntity): String {
    val parts = mutableListOf<String>()
    if (w.temperatureC != null && w.startHourLocal != null) {
        parts += "${"%.1f".format(w.temperatureC)} °C à ${w.startHourLocal} h"
    } else if (w.dayMinC != null && w.dayMaxC != null) {
        parts += "${"%.0f".format(w.dayMinC)} à ${"%.0f".format(w.dayMaxC)} °C dans la journée"
    }
    OpenMeteoClient.weatherLabel(w.weatherCode)?.let { parts += it }
    w.windKmh?.let { if (it >= 20) parts += "vent ${"%.0f".format(it)} km/h" }
    w.rainPrevious72hMm?.let { parts += "${"%.0f".format(it)} mm de pluie les 3 jours avant" }
    return parts.joinToString(" • ")
}

/** Conditions d'une course : météo réelle, indice du parcours, édition précédente, terrain noté. */
@Composable
fun RaceConditionsCard(
    raceItem: RaceItemWithContext,
    distanceKm: Int,
    terrainCondition: String,
    record: CourseRecord? = null,
    modifier: Modifier = Modifier
) {
    val factor = raceItem.courseFactorForDistance(distanceKm)
    val previousFactor = raceItem.previousEditionFactorForDistance(distanceKm)
    val label = raceItem.race.distanceNameFor(distanceKm)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("race_conditions_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConditionLine(
                icon = Icons.Default.Landscape,
                title = "Parcours ($label)",
                text = when {
                    factor != null -> CourseDifficultyCalculator.describeFactor(factor) +
                        (previousFactor?.let { " (édition précédente : ${CourseDifficultyCalculator.formatFactor(it)})" } ?: "")
                    previousFactor != null -> "Édition précédente : ${CourseDifficultyCalculator.formatFactor(previousFactor)}"
                    else -> "Indice inconnu (pas assez de coureurs réguliers, ou course pas encore courue)"
                } + if (terrainCondition.isNotBlank()) " • terrain noté : $terrainCondition" else ""
            )
            if (record != null && record.distanceLabel == (if (distanceKm == 5) "5 km" else "10 km")) {
                ConditionLine(
                    icon = Icons.Default.EmojiEvents,
                    title = "Record de parcours",
                    text = record.editions.joinToString(" • ") { "${it.seasonYear} : ${it.timeFormatted}" } +
                        when (record.status) {
                            RecordStatus.BEATEN -> " — battu (${GoalsCalculator.formatDelta(record.deltaSeconds ?: 0)})"
                            RecordStatus.NOT_BEATEN -> " — pas battu (${GoalsCalculator.formatDelta(record.deltaSeconds ?: 0)})"
                            RecordStatus.TO_BEAT -> " — à battre : ${record.best.timeFormatted}"
                            RecordStatus.FIRST -> ""
                        }
                )
            }
            val w = raceItem.weather
            ConditionLine(
                icon = Icons.Default.Cloud,
                title = "Météo au départ",
                text = when {
                    w != null -> weatherSummary(w) + " (${w.source})"
                    raceItem.race.startLatitude == null -> "Lieu de départ non publié : météo indisponible"
                    raceItem.race.isCompleted && raceItem.resultsByProfileAndDistance.isEmpty() ->
                        "Récupérée pour les courses courues par le Trio"
                    raceItem.race.isCompleted -> "Récupérée à la prochaine synchro"
                    else -> "Disponible après la course"
                }
            )
        }
    }
}

@Composable
private fun ConditionLine(icon: ImageVector, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
