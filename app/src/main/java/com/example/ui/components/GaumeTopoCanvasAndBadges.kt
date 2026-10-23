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
 * Barre supérieure optimisée pour Samsung Galaxy S25 :
 * - Bandeau supérieur de dégagement sous le poinçon de l'appareil photo frontal (statusBarsPadding + marge dédiée)
 * - Ligne de commandes agrandie (42 dp) : Sélecteur d'année dès 2026 (2026, 2027...) + Bascule Thème Sombre/Clair + Bouton Sync MyChrono
 * - Ligne coureurs : Sélecteur des 3 coureurs (Moi, Laetitia, Élodie) sur 1 ligne sans retour à la ligne.
 */
@Composable
fun RunnerAndSeasonHeaderBar(
    profiles: List<RunnerProfileEntity>,
    selectedProfileId: String,
    selectedSeasonYear: Int,
    availableSeasons: List<Int>,
    isSyncing: Boolean,
    onSelectProfile: (String) -> Unit,
    onSelectSeasonYear: (Int) -> Unit,
    onTriggerSync: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val seasonsToDisplay = remember(availableSeasons) {
        val clean = availableSeasons.filter { it >= 2026 }.sorted()
        if (clean.isEmpty()) listOf(2026, 2027) else clean
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bandeau de titre discret en haut : laisse la zone centrale sous l'appareil photo du S25 libre de tout bouton
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Landscape,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gaume Trail • Challenge ALG",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Saison $selectedSeasonYear",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Ligne de commandes descendue sous l'appareil photo avec grands boutons (42 dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sélecteur d'année agrandi (2026 / 2027)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .height(42.dp)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        seasonsToDisplay.forEach { year ->
                            val isYearSelected = year == selectedSeasonYear
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isYearSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { onSelectSeasonYear(year) }
                                    .testTag("season_chip_$year")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = year.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isYearSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Bouton bascule Thème Sombre / Clair agrandi avec icône + libellé clair
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleDarkTheme() }
                        .testTag("header_dark_theme_toggle_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Activer le thème clair" else "Activer le thème sombre",
                            tint = if (isDarkTheme) GoldPodium else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDarkTheme) "Clair" else "Sombre",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                // Bouton Sync MyChrono aligné à 42 dp
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isSyncing) { onTriggerSync() }
                        .testTag("header_sync_mychrono_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Synchroniser MyChrono",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isSyncing) "Sync…" else "MyChrono",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Sélecteur Trio (Moi / Laetitia / Élodie) calibré pour S25 (360dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                profiles.forEach { profile ->
                    val isSelected = profile.profileId == selectedProfileId
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            Color(profile.avatarColorHex)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        label = "runner_pill_bg"
                    )
                    val contentColor = if (isSelected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val shortLabel = when (profile.profileId) {
                        "me" -> "Moi"
                        "laetitia" -> "Laetitia"
                        "elodie" -> "Élodie"
                        else -> profile.displayName.substringBefore(" ")
                    }

                    val activeBib = if (profile.preferredDistanceKm == 5) {
                        profile.defaultBib5km.ifBlank { profile.defaultBib10km }
                    } else {
                        profile.defaultBib10km.ifBlank { profile.defaultBib5km }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectProfile(profile.profileId) }
                            .testTag("runner_selector_${profile.profileId}"),
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White else Color(profile.avatarColorHex)
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (activeBib.isNotBlank()) "$shortLabel #$activeBib" else shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Badge compact indiquant l'état du terrain (Sec, Souple, Boueux, Très gras) et la météo.
 */
@Composable
fun TerrainAndWeatherMiniBadge(
    terrainCondition: String,
    weatherCondition: String,
    temperatureC: Int? = null,
    modifier: Modifier = Modifier
) {
    val (terrainBg, terrainFg) = when (terrainCondition) {
        "Boueux" -> Color(0xFF795548).copy(alpha = 0.16f) to Color(0xFF5D4037)
        "Très gras" -> ClayTerracotta.copy(alpha = 0.18f) to ClayTerracotta
        "Souple" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> CountingTop15BadgeGreen.copy(alpha = 0.14f) to CountingTop15BadgeGreen
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = terrainBg
        ) {
            Text(
                text = "Terrain $terrainCondition",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = terrainFg,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (temperatureC != null) "$weatherCondition ${temperatureC}°C" else weatherCondition,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Badge indiquant si la course compte parmi les 15 meilleures de la distance choisie (10 km ou 5 km).
 */
@Composable
fun Top15StatusBadge(
    isCountingInTop15: Boolean,
    rankAmongRunnerRaces: Int?,
    distanceLabel: String = "",
    modifier: Modifier = Modifier
) {
    if (rankAmongRunnerRaces == null) return
    val bgColor = if (isCountingInTop15) {
        CountingTop15BadgeGreen.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isCountingInTop15) {
        CountingTop15BadgeGreen
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderStroke = if (isCountingInTop15) {
        BorderStroke(1.dp, CountingTop15BadgeGreen.copy(alpha = 0.45f))
    } else null

    val distPrefix = if (distanceLabel.isNotBlank()) "$distanceLabel • " else ""

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCountingInTop15) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isCountingInTop15) {
                    "${distPrefix}Top 15 (#$rankAmongRunnerRaces/15)"
                } else {
                    "${distPrefix}Hors Top 15 (#$rankAmongRunnerRaces)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Carte interactive sur mesure dessinant :
 * 1) Le tracé GPS 2D de la course (en forêt de Gaume)
 * 2) Le profil altimétrique D+ rempli sous la courbe
 */
@Composable
fun GaumeGpsAndElevationCanvas(
    trackPoints: List<TrackPoint>,
    recordedDistanceKm: Double,
    elevationGainMeters: Int,
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
                        text = "${recordedDistanceKm} km • +${elevationGainMeters}m D+",
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
                        val elevations = trackPoints.map { it.elevationMeters }
                        val minEle = elevations.minOrNull()?.toInt() ?: 210
                        val maxEle = elevations.maxOrNull()?.toInt() ?: 350

                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 18.dp)
                            ) {
                                val minE = elevations.minOrNull() ?: 200.0
                                val maxE = elevations.maxOrNull() ?: 350.0
                                val eleSpan = max(5.0, maxE - minE)

                                val linePath = Path()
                                val fillPath = Path()

                                trackPoints.forEachIndexed { idx, pt ->
                                    val x = (idx.toFloat() / max(1, trackPoints.lastIndex)) * size.width
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
                                text = "Max ${maxEle}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentTerracotta,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            )
                            Text(
                                text = "Min ${minEle}m",
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

/**
 * Boîte de dialogue pour configurer le numéro de dossard et la distance (5 km / 10 km) d'un profil.
 */
@Composable
fun EditProfileBibAndDistanceDialog(
    profile: RunnerProfileEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        displayName: String,
        myChronoId: String,
        bib10km: String,
        bib5km: String,
        preferredDistanceKm: Int,
        clubName: String,
        categoryLabel: String
    ) -> Unit
) {
    var displayName by remember(profile) { mutableStateOf(profile.displayName) }
    var preferredDistanceKm by remember(profile) { mutableIntStateOf(profile.preferredDistanceKm) }
    var bib10km by remember(profile) { mutableStateOf(profile.defaultBib10km) }
    var bib5km by remember(profile) { mutableStateOf(profile.defaultBib5km) }
    var myChronoId by remember(profile) { mutableStateOf(profile.myChronoRunnerId) }
    var clubName by remember(profile) { mutableStateOf(profile.clubName) }
    var categoryLabel by remember(profile) { mutableStateOf(profile.categoryLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Dossards & Distance (${profile.displayName})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Les résultats du 10 km et du 5 km sont comptabilisés séparément pour chaque Challenge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Distance affichée par défaut :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = preferredDistanceKm == 10,
                        onClick = { preferredDistanceKm = 10 },
                        label = { Text("Challenge 10 km", maxLines = 1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_dist_10km_chip")
                    )
                    FilterChip(
                        selected = preferredDistanceKm == 5,
                        onClick = { preferredDistanceKm = 5 },
                        label = { Text("Challenge 5 km", maxLines = 1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_dist_5km_chip")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bib10km,
                        onValueChange = { bib10km = it },
                        label = { Text("Dossard 10 km", maxLines = 1) },
                        placeholder = { Text("Ex: 697") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_bib_10km_input")
                    )
                    OutlinedTextField(
                        value = bib5km,
                        onValueChange = { bib5km = it },
                        label = { Text("Dossard 5 km", maxLines = 1) },
                        placeholder = { Text("Ex: 1150") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_bib_5km_input")
                    )
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nom / Prénom sur MyChrono") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = myChronoId,
                        onValueChange = { myChronoId = it },
                        label = { Text("ID MyChrono", maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(0.45f)
                    )
                    OutlinedTextField(
                        value = categoryLabel,
                        onValueChange = { categoryLabel = it },
                        label = { Text("Catégorie", maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(0.55f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        displayName,
                        myChronoId,
                        bib10km,
                        bib5km,
                        preferredDistanceKm,
                        clubName,
                        categoryLabel
                    )
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("dialog_save_bib_button")
            ) {
                Text("Enregistrer & Sync", maxLines = 1)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", maxLines = 1)
            }
        }
    )
}
