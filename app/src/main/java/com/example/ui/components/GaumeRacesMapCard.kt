package com.example.ui.components

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.GaumeInitialSeedData
import com.example.domain.RaceItemWithContext
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal
import kotlin.math.hypot

private enum class GaumeMapFilter {
    ALL,
    CONQUERED,
    PLANNED,
    UPCOMING
}

private data class ProjectedMarkerHit(
    val raceId: String,
    val screenX: Float,
    val screenY: Float
)

/**
 * Bornes géographiques WGS84 couvrant toute la Gaume et le Sud-Luxembourg :
 * - Ouest : Bouillon / Muno / Chassepierre / Orval (5.14° E)
 * - Est : Saint-Léger / Musson / Messancy / Athus / Arlon (5.86° E)
 * - Sud : Torgny / Ruette / Signeulx / Virton (49.49° N)
 * - Nord : Léglise / Anlier / Chiny / Herbeumont (49.83° N)
 */
private const val GAUME_MIN_LAT = 49.485
private const val GAUME_MAX_LAT = 49.835
private const val GAUME_MIN_LON = 5.145
private const val GAUME_MAX_LON = 5.865

/**
 * Carte Interactive Native de Gaume (100% Jetpack Compose Canvas haute définition, zéro WebView vide) :
 * - Positionne chaque village aux coordonnées GPS WGS84 exactes
 * - Zoom pincé et boutons +/- (1.0x à 5.5x) + déplacement libre (pan)
 * - Fond Routier (E411, N83, N88, N4, N82 + Rivières Semois, Rulles, Ton, Vire, Chiers + Forêts)
 * - Fond Topographique (3 Cuestes Gaumaises, Massif d'Anlier, courbes de relief & grille GPS)
 * - Calque des 6 Fiefs de Gaume avec progression de conquête
 */
@Composable
fun GaumeRacesMapCard(
    seasonYear: Int,
    selectedDistanceKm: Int,
    racesWithContext: List<RaceItemWithContext>,
    defaultTargetPoints: Int,
    onOpenRaceDetail: (String) -> Unit,
    onTogglePlannedForSimulator: (raceId: String, isPlanned: Boolean, targetPoints: Int) -> Unit,
    onQuickUpdateTerrain: (raceId: String, terrainCondition: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapFilter by remember { mutableStateOf(GaumeMapFilter.ALL) }
    var isTopoLayer by remember { mutableStateOf(false) }
    var showTerritoriesOverlay by remember { mutableStateOf(true) }

    var zoomScale by remember { mutableFloatStateOf(1.12f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasPixelSize by remember { mutableStateOf(Size(800f, 550f)) }

    val filteredRaces = remember(racesWithContext, mapFilter, selectedDistanceKm) {
        racesWithContext.filter { item ->
            val hasRun = item.resultForDistance(selectedDistanceKm) != null
            val isPlanned = item.terrainWeather?.isPlannedForSimulator == true &&
                item.terrainWeather.plannedDistanceKm == selectedDistanceKm
            when (mapFilter) {
                GaumeMapFilter.ALL -> true
                GaumeMapFilter.CONQUERED -> hasRun
                GaumeMapFilter.PLANNED -> isPlanned
                GaumeMapFilter.UPCOMING -> !hasRun
            }
        }
    }

    var selectedRaceId by remember(filteredRaces, racesWithContext) {
        mutableStateOf(
            filteredRaces.firstOrNull()?.race?.raceId
                ?: racesWithContext.firstOrNull()?.race?.raceId
        )
    }

    val selectedRaceItem = remember(filteredRaces, selectedRaceId, racesWithContext) {
        filteredRaces.find { it.race.raceId == selectedRaceId }
            ?: racesWithContext.find { it.race.raceId == selectedRaceId }
            ?: filteredRaces.firstOrNull()
            ?: racesWithContext.firstOrNull()
    }

    fun focusOnLatLon(lat: Double, lon: Double, targetZoom: Float = 2.1f) {
        val w = canvasPixelSize.width.coerceAtLeast(300f)
        val h = canvasPixelSize.height.coerceAtLeast(220f)
        val normX = ((lon - GAUME_MIN_LON) / (GAUME_MAX_LON - GAUME_MIN_LON)).toFloat().coerceIn(0f, 1f)
        val normY = (1.0 - (lat - GAUME_MIN_LAT) / (GAUME_MAX_LAT - GAUME_MIN_LAT)).toFloat().coerceIn(0f, 1f)
        val padX = w * 0.07f
        val padY = h * 0.09f
        val baseX = padX + normX * (w - 2f * padX)
        val baseY = padY + normY * (h - 2f * padY)
        val centerX = w / 2f
        val centerY = h / 2f
        zoomScale = targetZoom.coerceIn(1.0f, 5.5f)
        val rawPanX = -(baseX - centerX) * zoomScale
        val rawPanY = -(baseY - centerY) * zoomScale
        val maxPanX = (w * (zoomScale - 1f) / 2f) + w * 0.25f
        val maxPanY = (h * (zoomScale - 1f) / 2f) + h * 0.25f
        panOffset = Offset(
            x = rawPanX.coerceIn(-maxPanX, maxPanX),
            y = rawPanY.coerceIn(-maxPanY, maxPanY)
        )
    }

    val forestGreen = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gaume_races_map_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // En-tête de la Carte Interactive Native de Gaume
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
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = forestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Carte Interactive de Gaume $seasonYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "GPS exact • Zoomez et touchez un village (${selectedDistanceKm} km)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Boutons Calques (Fiefs & Routier/Topo)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (showTerritoriesOverlay) {
                            GoldPodium.copy(alpha = 0.20f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (showTerritoriesOverlay) GoldPodium else Color.Transparent
                        ),
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTerritoriesOverlay = !showTerritoriesOverlay }
                            .testTag("map_toggle_fiefs_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Fiefs",
                                tint = if (showTerritoriesOverlay) GoldPodium else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Fiefs",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isTopoLayer = !isTopoLayer }
                            .testTag("map_toggle_layer_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Fond de carte",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isTopoLayer) "Topo" else "Routier",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Filtres sur une seule ligne
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val conqueredCount = racesWithContext.count { it.resultForDistance(selectedDistanceKm) != null }
                val plannedCount = racesWithContext.count {
                    it.terrainWeather?.isPlannedForSimulator == true &&
                        it.terrainWeather.plannedDistanceKm == selectedDistanceKm
                }
                val filterItems = listOf(
                    GaumeMapFilter.ALL to "Toutes (${racesWithContext.size})",
                    GaumeMapFilter.CONQUERED to "Courues ($conqueredCount)",
                    GaumeMapFilter.PLANNED to "Prévues ($plannedCount)",
                    GaumeMapFilter.UPCOMING to "À conquérir"
                )
                filterItems.forEach { (mode, label) ->
                    val isSelected = mapFilter == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { mapFilter = mode },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) {
                            forestGreen
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Moteur Cartographique Natif Jetpack Compose (100% visible, Zoom/Pan & Clic direct sur les villages)
            var currentMarkerHits by remember { mutableStateOf<List<ProjectedMarkerHit>>(emptyList()) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(295.dp)
                    .testTag("gaume_native_map_canvas"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, forestGreen.copy(alpha = 0.38f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val racesToRender = if (filteredRaces.isNotEmpty()) filteredRaces else racesWithContext

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(racesToRender, zoomScale, panOffset) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        val nextZoom = if (zoomScale < 3.2f) (zoomScale * 1.55f).coerceAtMost(5.5f) else 1.12f
                                        if (nextZoom <= 1.15f) {
                                            zoomScale = 1.12f
                                            panOffset = Offset.Zero
                                        } else {
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val dx = tapOffset.x - cx
                                            val dy = tapOffset.y - cy
                                            zoomScale = nextZoom
                                            panOffset = Offset(
                                                x = (panOffset.x - dx * 0.45f).coerceIn(-size.width.toFloat(), size.width.toFloat()),
                                                y = (panOffset.y - dy * 0.45f).coerceIn(-size.height.toFloat(), size.height.toFloat())
                                            )
                                        }
                                    },
                                    onTap = { tapOffset ->
                                        val hit = currentMarkerHits.minByOrNull { m ->
                                            hypot(tapOffset.x - m.screenX, tapOffset.y - m.screenY)
                                        }
                                        if (hit != null && hypot(tapOffset.x - hit.screenX, tapOffset.y - hit.screenY) <= 58f) {
                                            selectedRaceId = hit.raceId
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1.0f, 5.5f)
                                    val w = size.width.toFloat().coerceAtLeast(300f)
                                    val h = size.height.toFloat().coerceAtLeast(220f)
                                    val maxPanX = (w * (newScale - 1f) / 2f) + w * 0.25f
                                    val maxPanY = (h * (newScale - 1f) / 2f) + h * 0.25f
                                    zoomScale = newScale
                                    panOffset = Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                }
                            }
                    ) {
                        canvasPixelSize = size
                        val w = size.width
                        val h = size.height
                        val padX = w * 0.07f
                        val padY = h * 0.09f
                        val centerX = w / 2f
                        val centerY = h / 2f

                        fun projectGps(lat: Double, lon: Double): Offset {
                            val normX = ((lon - GAUME_MIN_LON) / (GAUME_MAX_LON - GAUME_MIN_LON)).toFloat()
                            val normY = (1.0 - (lat - GAUME_MIN_LAT) / (GAUME_MAX_LAT - GAUME_MIN_LAT)).toFloat()
                            val baseX = padX + normX * (w - 2f * padX)
                            val baseY = padY + normY * (h - 2f * padY)
                            return Offset(
                                x = centerX + (baseX - centerX) * zoomScale + panOffset.x,
                                y = centerY + (baseY - centerY) * zoomScale + panOffset.y
                            )
                        }

                        // 1. Fond de carte (Routier ou Topographique)
                        drawRect(
                            color = if (isTopoLayer) Color(0xFFE7DFD1) else Color(0xFFDDECE0)
                        )

                        // 2. Massifs forestiers de Gaume & Ardenne
                        drawGaumeForestMassifs(::projectGps, isTopoLayer)

                        // 3. Grille GPS & Courbes de relief des 3 Cuestes Gaumaises (si mode Topo ou en filigrane)
                        drawGaumeTopographyAndCuestes(::projectGps, isTopoLayer, zoomScale)

                        // 4. Calque des 6 Fiefs / Territoires de Gaume
                        if (showTerritoriesOverlay) {
                            drawGaumeTerritoriesOverlay(
                                projectGps = ::projectGps,
                                racesWithContext = racesWithContext,
                                selectedDistanceKm = selectedDistanceKm,
                                zoomScale = zoomScale
                            )
                        }

                        // 5. Frontières Franco-Belge et Luxembourgeoise
                        drawGaumeBorders(::projectGps)

                        // 6. Réseau hydrographique réel (La Semois, La Rulles, Le Ton, La Vire, La Chiers)
                        drawGaumeRivers(::projectGps, zoomScale)

                        // 7. Réseau routier réel de Gaume (E411, N83, N88, N4, N82, N87)
                        drawGaumeRoads(::projectGps, isTopoLayer, zoomScale)

                        // 8. Marqueurs GPS exacts de chaque village du Challenge ALG
                        val hits = mutableListOf<ProjectedMarkerHit>()
                        val sortedForZOrder = racesToRender.sortedBy {
                            it.race.raceId == selectedRaceItem?.race?.raceId
                        }

                        sortedForZOrder.forEach { item ->
                            val pos = projectGps(item.gaumeGeo.latitude, item.gaumeGeo.longitude)
                            hits.add(ProjectedMarkerHit(item.race.raceId, pos.x, pos.y))

                            val result = item.resultForDistance(selectedDistanceKm)
                            val isCounting = item.isCountingForDistance(selectedDistanceKm)
                            val isPlanned = item.terrainWeather?.isPlannedForSimulator == true &&
                                item.terrainWeather.plannedDistanceKm == selectedDistanceKm
                            val isSelected = item.race.raceId == selectedRaceItem?.race?.raceId

                            val markerColor = when {
                                result != null && isCounting -> Color(0xFF1E7E48)
                                result != null -> Color(0xFFD49B1B)
                                isPlanned -> Color(0xFF006D77)
                                else -> Color(0xFFC85A32)
                            }

                            val baseRadius = if (isSelected) 16.dp.toPx() else 12.dp.toPx()

                            if (isSelected) {
                                drawCircle(
                                    color = Color(0xFFFFD700).copy(alpha = 0.42f),
                                    radius = baseRadius * 1.65f,
                                    center = pos
                                )
                                drawCircle(
                                    color = Color(0xFF1E7E48),
                                    radius = baseRadius * 1.25f,
                                    center = pos,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // Ombre portée du marqueur
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.28f),
                                radius = baseRadius,
                                center = Offset(pos.x + 1.5f, pos.y + 2.5f)
                            )

                            // Pastille du village
                            drawCircle(
                                color = markerColor,
                                radius = baseRadius,
                                center = pos
                            )
                            drawCircle(
                                color = if (isSelected) Color(0xFFFFD700) else Color.White,
                                radius = baseRadius,
                                center = pos,
                                style = Stroke(width = if (isSelected) 2.6.dp.toPx() else 1.8.dp.toPx())
                            )

                            // Numéro de manche au centre du marqueur
                            val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                textSize = if (isSelected) 11.dp.toPx() else 9.5.dp.toPx()
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }
                            val textOffsetY = (numPaint.descent() + numPaint.ascent()) / 2f
                            drawContext.canvas.nativeCanvas.drawText(
                                item.race.roundNumber.toString(),
                                pos.x,
                                pos.y - textOffsetY,
                                numPaint
                            )

                            // Étiquette du nom du village (permanente si sélectionné ou zoom >= 1.35x ou manche principale)
                            val showVillageLabel = isSelected ||
                                zoomScale >= 1.35f ||
                                result != null ||
                                isPlanned ||
                                item.race.roundNumber % 2 == 1

                            if (showVillageLabel) {
                                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.rgb(20, 38, 26)
                                    textSize = if (isSelected) 10.5.dp.toPx() else 9.dp.toPx()
                                    typeface = Typeface.create(
                                        Typeface.DEFAULT,
                                        if (isSelected) Typeface.BOLD else Typeface.NORMAL
                                    )
                                    textAlign = Paint.Align.CENTER
                                }
                                val labelText = if (isSelected && result != null) {
                                    "${item.gaumeGeo.canonicalVillage} (${result.challengePoints} pts)"
                                } else {
                                    item.gaumeGeo.canonicalVillage
                                }
                                val textWidth = labelPaint.measureText(labelText)
                                val pillPadH = 5.dp.toPx()
                                val pillH = if (isSelected) 16.dp.toPx() else 13.5.dp.toPx()
                                val pillTop = pos.y - baseRadius - pillH - 3.dp.toPx()
                                val pillLeft = pos.x - textWidth / 2f - pillPadH

                                drawRoundRect(
                                    color = if (isSelected) Color(0xFF0E2B1C) else Color.White.copy(alpha = 0.88f),
                                    topLeft = Offset(pillLeft, pillTop),
                                    size = Size(textWidth + pillPadH * 2f, pillH),
                                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                                )
                                if (isSelected) {
                                    drawRoundRect(
                                        color = Color(0xFFFFD700),
                                        topLeft = Offset(pillLeft, pillTop),
                                        size = Size(textWidth + pillPadH * 2f, pillH),
                                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                                drawContext.canvas.nativeCanvas.drawText(
                                    labelText,
                                    pos.x,
                                    pillTop + pillH * 0.73f,
                                    labelPaint
                                )
                            }
                        }
                        currentMarkerHits = hits
                    }

                    // Contrôles de Zoom (+ / - / Vue Gaume) en haut à droite
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    zoomScale = 1.12f
                                    panOffset = Offset.Zero
                                }
                                .testTag("map_reset_bounds_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Recentrer Gaume",
                                    tint = Color(0xFF1B5E3A),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Vue Gaume (${"%.1f".format(zoomScale)}x)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1B5E3A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        zoomScale = (zoomScale * 1.3f).coerceAtMost(5.5f)
                                    }
                                    .testTag("map_zoom_in_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Zoom avant",
                                        tint = Color(0xFF1B5E3A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val next = (zoomScale / 1.3f).coerceAtLeast(1.0f)
                                        zoomScale = next
                                        if (next <= 1.15f) panOffset = Offset.Zero
                                    }
                                    .testTag("map_zoom_out_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Zoom arrière",
                                        tint = Color(0xFF1B5E3A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Légende en bas à gauche
                    Surface(
                        color = Color.White.copy(alpha = 0.93f),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(color = CountingTop15BadgeGreen, label = "Courue (Top 15)")
                            LegendDot(color = SemoisRiverTeal, label = "Prévue")
                            LegendDot(color = ClayTerracotta, label = "À conquérir")
                        }
                    }
                }
            }

            // Ruban de sélection rapide par village (centre la carte directement sur les coordonnées GPS du village)
            val ribbonRaces = if (filteredRaces.isNotEmpty()) filteredRaces else racesWithContext
            if (ribbonRaces.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ribbonRaces.forEach { item ->
                        val isSelected = item.race.raceId == selectedRaceItem?.race?.raceId
                        val hasRun = item.resultForDistance(selectedDistanceKm) != null
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isSelected -> forestGreen
                                hasRun -> CountingTop15BadgeGreen.copy(alpha = 0.14f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) forestGreen
                                else if (hasRun) CountingTop15BadgeGreen.copy(alpha = 0.45f)
                                else Color.Transparent
                            ),
                            modifier = Modifier
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedRaceId = item.race.raceId
                                    focusOnLatLon(item.gaumeGeo.latitude, item.gaumeGeo.longitude, targetZoom = 2.0f)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "M#${item.race.roundNumber} ${item.gaumeGeo.canonicalVillage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected || hasRun) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        Color.White
                                    } else if (hasRun) {
                                        CountingTop15BadgeGreen
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

            // Fiche interactive du village sélectionné sur la carte de Gaume
            selectedRaceItem?.let { item ->
                val result = item.resultForDistance(selectedDistanceKm)
                val tw = item.terrainWeather
                val currentTerrain = tw?.terrainCondition ?: "Sec"
                val currentWeather = tw?.weatherCondition ?: "Nuageux"
                val isPlanned = tw?.isPlannedForSimulator == true && tw.plannedDistanceKm == selectedDistanceKm

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "M#${item.race.roundNumber} • ${item.gaumeGeo.canonicalVillage} (${item.gaumeGeo.territoryName})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = forestGreen,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.race.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "GPS exact : %.4f° N, %.4f° E • %s".format(
                                        item.gaumeGeo.latitude,
                                        item.gaumeGeo.longitude,
                                        item.race.dateIso
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (result != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CountingTop15BadgeGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${result.officialTimeFormatted} • ${result.challengePoints} pts",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CountingTop15BadgeGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                TerrainAndWeatherMiniBadge(
                                    terrainCondition = currentTerrain,
                                    weatherCondition = currentWeather,
                                    temperatureC = tw?.temperatureC
                                )
                            }
                        }

                        // Sélecteur 1-clic de l'état du terrain en Gaume (Sec / Souple / Boueux / Très gras)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Terrain :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            listOf("Sec", "Souple", "Boueux", "Très gras").forEach { cond ->
                                val isCondSelected = currentTerrain == cond
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (isCondSelected) {
                                        forestGreen
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCondSelected) forestGreen else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .clickable { onQuickUpdateTerrain(item.race.raceId, cond) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cond,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isCondSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCondSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Actions rapides sur une seule ligne (Itinéraire GPS | Simulateur Top 15 | Fiche Course)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val geoUri = Uri.parse(
                                            "geo:${item.gaumeGeo.latitude},${item.gaumeGeo.longitude}?q=${item.gaumeGeo.latitude},${item.gaumeGeo.longitude}(${Uri.encode("${item.gaumeGeo.canonicalVillage} - ${item.race.title}")})"
                                        )
                                        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("map_gps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GPS Départ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (result == null) {
                                OutlinedButton(
                                    onClick = {
                                        onTogglePlannedForSimulator(
                                            item.race.raceId,
                                            !isPlanned,
                                            tw?.simulatedTargetPoints ?: defaultTargetPoints
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("map_plan_simulator_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlanned) Icons.Default.CheckCircle else Icons.Default.AddTask,
                                        contentDescription = null,
                                        tint = if (isPlanned) CountingTop15BadgeGreen else forestGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPlanned) "Prévue ✓" else "Simuler Top 15",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = { onOpenRaceDetail(item.race.raceId) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("map_open_detail_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Détail",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
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
}

private fun DrawScope.drawGaumeForestMassifs(
    projectGps: (Double, Double) -> Offset,
    isTopoLayer: Boolean
) {
    val forestColor = if (isTopoLayer) {
        Color(0xFFC5D5BE).copy(alpha = 0.55f)
    } else {
        Color(0xFFB7D6BF).copy(alpha = 0.65f)
    }

    val massifs = listOf(
        // 1. Grande Forêt de Chiny, Suxy, Rossignol & Orval Nord
        listOf(
            49.790 to 5.200,
            49.780 to 5.420,
            49.735 to 5.510,
            49.695 to 5.475,
            49.640 to 5.335,
            49.685 to 5.215
        ),
        // 2. Forêt Domaniale d'Anlier & Rulles
        listOf(
            49.825 to 5.530,
            49.820 to 5.735,
            49.740 to 5.710,
            49.725 to 5.545
        ),
        // 3. Massif forestier de Buzenol, Étalle, Saint-Léger & Bon-Lieu (Virton)
        listOf(
            49.660 to 5.530,
            49.655 to 5.695,
            49.605 to 5.675,
            49.590 to 5.515,
            49.620 to 5.440
        )
    )

    massifs.forEach { pts ->
        val path = Path()
        pts.forEachIndexed { idx, (lat, lon) ->
            val p = projectGps(lat, lon)
            if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path = path, color = forestColor)
    }
}

private fun DrawScope.drawGaumeTopographyAndCuestes(
    projectGps: (Double, Double) -> Offset,
    isTopoLayer: Boolean,
    zoomScale: Float
) {
    // Grille de coordonnées GPS WGS84
    val gridAlpha = if (isTopoLayer) 0.28f else 0.14f
    val gridColor = Color(0xFF4A5D4E).copy(alpha = gridAlpha)
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb((gridAlpha * 255).toInt().coerceIn(40, 180), 50, 70, 55)
        textSize = 8.dp.toPx()
    }

    for (lat in listOf(49.55, 49.65, 49.75)) {
        val left = projectGps(lat, GAUME_MIN_LON)
        val right = projectGps(lat, GAUME_MAX_LON)
        drawLine(
            color = gridColor,
            start = left,
            end = right,
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )
        drawContext.canvas.nativeCanvas.drawText(
            "%.2f°N".format(lat),
            8.dp.toPx(),
            left.y - 3.dp.toPx(),
            labelPaint
        )
    }

    for (lon in listOf(5.30, 5.50, 5.70)) {
        val top = projectGps(GAUME_MAX_LAT, lon)
        val bottom = projectGps(GAUME_MIN_LAT, lon)
        drawLine(
            color = gridColor,
            start = top,
            end = bottom,
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )
    }

    // Les 3 Cuestes Gaumaises (Lignes de crête lorraines)
    val cuesteAlpha = if (isTopoLayer) 0.72f else 0.22f
    val cuesteColor = Color(0xFF9C6B3B).copy(alpha = cuesteAlpha)
    val cuestes = listOf(
        "Cueste Sinémurienne (365 m)" to listOf(
            49.705 to 5.245,
            49.685 to 5.360,
            49.668 to 5.490,
            49.665 to 5.630,
            49.675 to 5.775
        ),
        "Cueste Charmouthienne (395 m)" to listOf(
            49.628 to 5.340,
            49.605 to 5.480,
            49.592 to 5.585,
            49.612 to 5.680,
            49.625 to 5.790
        ),
        "Cueste Bajocienne • Torgny (345 m)" to listOf(
            49.512 to 5.465,
            49.545 to 5.525,
            49.552 to 5.625,
            49.558 to 5.745
        )
    )

    val cuestePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(122, 78, 36)
        textSize = 8.5.dp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }

    cuestes.forEach { (title, pts) ->
        val path = Path()
        pts.forEachIndexed { idx, (lat, lon) ->
            val p = projectGps(lat, lon)
            if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(
            path = path,
            color = cuesteColor,
            style = Stroke(
                width = if (isTopoLayer) 2.2.dp.toPx() else 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
            )
        )
        if (isTopoLayer || zoomScale >= 1.6f) {
            val mid = pts[pts.size / 2]
            val mp = projectGps(mid.first, mid.second)
            drawContext.canvas.nativeCanvas.drawText(title, mp.x - 45.dp.toPx(), mp.y - 4.dp.toPx(), cuestePaint)
        }
    }
}

private fun DrawScope.drawGaumeTerritoriesOverlay(
    projectGps: (Double, Double) -> Offset,
    racesWithContext: List<RaceItemWithContext>,
    selectedDistanceKm: Int,
    zoomScale: Float
) {
    val fiefLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8.5.dp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    GaumeInitialSeedData.gaumeTerritories.forEach { t ->
        val center = projectGps(t.centerLat, t.centerLon)
        val edge = projectGps(t.centerLat, t.centerLon + (t.radiusMeters / 72000.0))
        val radiusPx = hypot(edge.x - center.x, edge.y - center.y).coerceAtLeast(24.dp.toPx())

        val inTerritory = racesWithContext.filter { it.gaumeGeo.territoryId == t.territoryId }
        val conqueredCount = inTerritory.count { it.resultForDistance(selectedDistanceKm) != null }
        val isConquered = conqueredCount > 0
        val baseColor = Color(t.colorHex)

        drawCircle(
            color = baseColor.copy(alpha = if (isConquered) 0.19f else 0.08f),
            radius = radiusPx,
            center = center
        )
        drawCircle(
            color = baseColor.copy(alpha = if (isConquered) 0.78f else 0.42f),
            radius = radiusPx,
            center = center,
            style = Stroke(
                width = if (isConquered) 2.dp.toPx() else 1.2.dp.toPx(),
                pathEffect = if (isConquered) null else PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
        )

        if (zoomScale <= 2.4f) {
            fiefLabelPaint.color = android.graphics.Color.rgb(25, 45, 32)
            val shortName = t.title.substringBefore("&").trim()
            val label = "$shortName ($conqueredCount/${inTerritory.size})"
            drawContext.canvas.nativeCanvas.drawText(
                label,
                center.x,
                center.y + radiusPx * 0.72f,
                fiefLabelPaint
            )
        }
    }
}

private fun DrawScope.drawGaumeBorders(
    projectGps: (Double, Double) -> Offset
) {
    // Frontière Franco-Belge (Sud de la Gaume : Muno -> Orval -> Torgny -> Signeulx -> Athus)
    val frBeBorder = listOf(
        49.700 to 5.150,
        49.660 to 5.230,
        49.615 to 5.330,
        49.585 to 5.425,
        49.502 to 5.472,
        49.528 to 5.565,
        49.542 to 5.660,
        49.545 to 5.760,
        49.552 to 5.845
    )
    val borderPath = Path()
    frBeBorder.forEachIndexed { idx, (lat, lon) ->
        val p = projectGps(lat, lon)
        if (idx == 0) borderPath.moveTo(p.x, p.y) else borderPath.lineTo(p.x, p.y)
    }
    drawPath(
        path = borderPath,
        color = Color(0xFF6E5A4F).copy(alpha = 0.55f),
        style = Stroke(
            width = 1.6.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 5f, 3f, 5f))
        )
    )
}

private fun DrawScope.drawGaumeRivers(
    projectGps: (Double, Double) -> Offset,
    zoomScale: Float
) {
    val riverColor = Color(0xFF1E88A8)
    // 1. La Semois (Arlon -> Étalle -> Tintigny -> Jamoigne -> Izel -> Chiny -> Florenville -> Chassepierre -> Herbeumont)
    val semois = listOf(
        49.683 to 5.816,
        49.685 to 5.714,
        49.674 to 5.563,
        49.671 to 5.533,
        49.683 to 5.513,
        49.680 to 5.479,
        49.695 to 5.409,
        49.693 to 5.374,
        49.738 to 5.343,
        49.713 to 5.318,
        49.699 to 5.310,
        49.707 to 5.265,
        49.729 to 5.242,
        49.776 to 5.236
    )

    // 2. La Rulles (Forêt d'Anlier -> Habay -> Rulles -> Marbehan -> Tintigny)
    val rulles = listOf(
        49.772 to 5.620,
        49.728 to 5.645,
        49.723 to 5.618,
        49.709 to 5.595,
        49.718 to 5.560,
        49.726 to 5.535,
        49.693 to 5.524,
        49.683 to 5.513
    )

    // 3. Le Ton & La Vire (Châtillon -> Saint-Léger -> Ethe -> Virton -> Saint-Mard -> Torgny)
    val ton = listOf(
        49.622 to 5.691,
        49.612 to 5.656,
        49.583 to 5.582,
        49.567 to 5.533,
        49.555 to 5.524,
        49.546 to 5.498,
        49.527 to 5.473,
        49.507 to 5.475
    )

    val vire = listOf(
        49.581 to 5.677,
        49.556 to 5.658,
        49.541 to 5.592,
        49.558 to 5.573,
        49.555 to 5.524
    )

    listOf(semois to 2.8.dp.toPx(), rulles to 2.0.dp.toPx(), ton to 2.1.dp.toPx(), vire to 1.7.dp.toPx())
        .forEach { (pts, strokeW) ->
            val path = Path()
            pts.forEachIndexed { idx, (lat, lon) ->
                val p = projectGps(lat, lon)
                if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(
                path = path,
                color = riverColor.copy(alpha = 0.78f),
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

    if (zoomScale >= 1.05f) {
        val riverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(16, 105, 138)
            textSize = 8.5.dp.toPx()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        }
        val semoisLabelPos = projectGps(49.703, 5.385)
        drawContext.canvas.nativeCanvas.drawText("La Semois", semoisLabelPos.x, semoisLabelPos.y - 4.dp.toPx(), riverPaint)

        val tonLabelPos = projectGps(49.592, 5.610)
        drawContext.canvas.nativeCanvas.drawText("Le Ton", tonLabelPos.x, tonLabelPos.y - 3.dp.toPx(), riverPaint)
    }
}

private fun DrawScope.drawGaumeRoads(
    projectGps: (Double, Double) -> Offset,
    isTopoLayer: Boolean,
    zoomScale: Float
) {
    val roadAlpha = if (isTopoLayer) 0.45f else 0.75f
    // Autoroute E411 (Léglise -> Habay -> Fouches -> Arlon)
    val e411 = listOf(
        49.810 to 5.510,
        49.735 to 5.635,
        49.695 to 5.715,
        49.670 to 5.820
    )
    // Route Nationale N83 (Florenville -> Pin -> Jamoigne -> Tintigny -> Étalle -> Vance -> Arlon)
    val n83 = listOf(
        49.711 to 5.174,
        49.699 to 5.310,
        49.689 to 5.347,
        49.694 to 5.409,
        49.683 to 5.513,
        49.674 to 5.563,
        49.672 to 5.665,
        49.683 to 5.816
    )
    // Route Nationale N88 (Florenville -> Orval -> Meix-devant-Virton -> Virton -> Signeulx -> Musson -> Athus)
    val n88 = listOf(
        49.699 to 5.310,
        49.639 to 5.349,
        49.605 to 5.480,
        49.567 to 5.533,
        49.556 to 5.658,
        49.558 to 5.705,
        49.565 to 5.837
    )
    // Route N82 & N87 (Rulles -> Étalle -> Buzenol -> Ethe -> Virton -> Saint-Mard -> Torgny & Saint-Léger -> Arlon)
    val n87 = listOf(
        49.726 to 5.535,
        49.718 to 5.560,
        49.674 to 5.563,
        49.636 to 5.594,
        49.583 to 5.582,
        49.567 to 5.533,
        49.555 to 5.524,
        49.507 to 5.475
    )
    val n82 = listOf(
        49.567 to 5.533,
        49.583 to 5.582,
        49.612 to 5.656,
        49.622 to 5.691,
        49.683 to 5.816
    )

    fun drawRoadPolyline(pts: List<Pair<Double, Double>>, color: Color, widthPx: Float) {
        val path = Path()
        pts.forEachIndexed { idx, (lat, lon) ->
            val p = projectGps(lat, lon)
            if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    drawRoadPolyline(e411, Color(0xFFD97724).copy(alpha = roadAlpha), 2.8.dp.toPx())
    drawRoadPolyline(n83, Color(0xFFE29D38).copy(alpha = roadAlpha), 2.1.dp.toPx())
    drawRoadPolyline(n88, Color(0xFFE29D38).copy(alpha = roadAlpha), 2.1.dp.toPx())
    drawRoadPolyline(n87, Color(0xFFF2C14E).copy(alpha = roadAlpha), 1.7.dp.toPx())
    drawRoadPolyline(n82, Color(0xFFF2C14E).copy(alpha = roadAlpha), 1.7.dp.toPx())

    if (!isTopoLayer && zoomScale >= 1.0f) {
        val roadBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(95, 55, 15)
            textSize = 8.dp.toPx()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        listOf(
            Triple("E411", 49.755, 5.605),
            Triple("N83", 49.680, 5.615),
            Triple("N88", 49.625, 5.425),
            Triple("N82", 49.645, 5.740)
        ).forEach { (code, lat, lon) ->
            val p = projectGps(lat, lon)
            drawContext.canvas.nativeCanvas.drawText(code, p.x, p.y, roadBadgePaint)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF1F2D24),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
