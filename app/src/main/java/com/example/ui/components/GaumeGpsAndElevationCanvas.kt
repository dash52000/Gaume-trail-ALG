package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RunnerProfileEntity
import com.example.domain.TrackPoint
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import kotlin.math.max
import kotlin.math.min

/**
 * Carte interactive sur mesure dessinant :
 * 1) Le tracé GPS 2D de la course (en forêt de Gaume)
 * 2) Le profil altimétrique D+ rempli sous la courbe
 */
@Composable
fun GaumeGpsAndElevationCanvas(
    trackPoints: List<TrackPoint>,
    recordedDistanceKm: Double,
    elevationGainMeters: Int?,
    sourceLabel: String,
    modifier: Modifier = Modifier
) {
    val primaryForest = MaterialTheme.colorScheme.primary
    val accentTerracotta = ClayTerracotta
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tracé GPS 2D & Profil Altimétrique",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryForest.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${recordedDistanceKm} km • " + (elevationGainMeters?.let { "+${it} m D+" } ?: "D+ inconnu"),
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryForest,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            if (trackPoints.size >= 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                val lats = trackPoints.map { it.lat }
                                val lons = trackPoints.map { it.lon }
                                val minLat = lats.minOrNull() ?: 0.0
                                val maxLat = lats.maxOrNull() ?: 1.0
                                val minLon = lons.minOrNull() ?: 0.0
                                val maxLon = lons.maxOrNull() ?: 1.0
                                val latSpan = max(0.0001, maxLat - minLat)
                                val lonSpan = max(0.0001, maxLon - minLon)

                                val path = Path()
                                trackPoints.forEachIndexed { index, pt ->
                                    val x = ((pt.lon - minLon) / lonSpan).toFloat() * size.width
                                    val y = (1f - ((pt.lat - minLat) / latSpan).toFloat()) * size.height
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                drawPath(
                                    path = path,
                                    color = primaryForest,
                                    style = Stroke(
                                        width = 3.2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                val first = trackPoints.first()
                                val startX = ((first.lon - minLon) / lonSpan).toFloat() * size.width
                                val startY = (1f - ((first.lat - minLat) / latSpan).toFloat()) * size.height
                                drawCircle(
                                    color = GoldPodium,
                                    radius = 5.dp.toPx(),
                                    center = Offset(startX, startY)
                                )

                                val last = trackPoints.last()
                                val endX = ((last.lon - minLon) / lonSpan).toFloat() * size.width
                                val endY = (1f - ((last.lat - minLat) / latSpan).toFloat()) * size.height
                                drawCircle(
                                    color = accentTerracotta,
                                    radius = 5.dp.toPx(),
                                    center = Offset(endX, endY)
                                )
                            }
                            Text(
                                text = "Boucle GPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        // Seuls les points ayant une altitude dans le fichier sont dessinés (aucune altitude inventée).
                        val profilePoints = trackPoints.filter { !it.elevationMeters.isNaN() }
                        val elevations = profilePoints.map { it.elevationMeters }
                        val minEle = elevations.minOrNull()?.toInt()
                        val maxEle = elevations.maxOrNull()?.toInt()

                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 18.dp)
                            ) {
                                if (profilePoints.size < 2) return@Canvas
                                val minE = elevations.minOrNull() ?: 0.0
                                val maxE = elevations.maxOrNull() ?: 0.0
                                val eleSpan = max(5.0, maxE - minE)

                                val linePath = Path()
                                val fillPath = Path()

                                profilePoints.forEachIndexed { idx, pt ->
                                    val x = (idx.toFloat() / max(1, profilePoints.lastIndex)) * size.width
                                    val normalizedY = ((pt.elevationMeters - minE) / eleSpan).toFloat()
                                    val y = (1f - normalizedY) * size.height
                                    if (idx == 0) {
                                        linePath.moveTo(x, y)
                                        fillPath.moveTo(x, size.height)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        linePath.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }
                                }
                                fillPath.lineTo(size.width, size.height)
                                fillPath.close()

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            accentTerracotta.copy(alpha = 0.42f),
                                            accentTerracotta.copy(alpha = 0.04f)
                                        )
                                    )
                                )
                                drawPath(
                                    path = linePath,
                                    color = accentTerracotta,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Text(
                                text = maxEle?.let { "Max ${it}m" } ?: "Pas d'altitude",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentTerracotta,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            )
                            Text(
                                text = minEle?.let { "Min ${it}m" } ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
