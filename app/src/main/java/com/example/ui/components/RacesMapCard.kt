package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.VillageInfoEntity
import com.example.domain.RaceItemWithContext
import com.example.domain.VillageMapPoint
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal

private enum class RaceMapFilter { ALL, RUN, PLANNED, TO_RUN }

/**
 * La carte de l'appli, sur un vrai fond OpenStreetMap :
 * - « Manches » : les manches de la saison, courues, prévues ou à courir, avec la fiche de la manche touchée ;
 * - « Mes villages » : tous les villages du calendrier, et combien de fois le coureur y a couru.
 */
@Composable
fun RacesMapCard(
    seasonYear: Int,
    selectedDistanceKm: Int,
    runnerName: String,
    racesWithContext: List<RaceItemWithContext>,
    villagePoints: List<VillageMapPoint>,
    villageInfos: Map<String, VillageInfoEntity>,
    defaultTargetPoints: Int,
    onOpenRaceDetail: (String) -> Unit,
    onTogglePlannedForSimulator: (raceId: String, isPlanned: Boolean, targetPoints: Int) -> Unit,
    onQuickUpdateTerrain: (raceId: String, terrainCondition: String) -> Unit,
    onRequestVillageInfo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableIntStateOf(0) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("races_map_card"),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Carte • $runnerName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ChipRow(
                labels = listOf("Manches $seasonYear (${selectedDistanceKm} km)", "Mes villages"),
                selected = mode,
                onSelect = { mode = it }
            )
            if (mode == 0) {
                SeasonRacesMap(
                    seasonYear = seasonYear,
                    selectedDistanceKm = selectedDistanceKm,
                    racesWithContext = racesWithContext,
                    defaultTargetPoints = defaultTargetPoints,
                    onOpenRaceDetail = onOpenRaceDetail,
                    onTogglePlannedForSimulator = onTogglePlannedForSimulator,
                    onQuickUpdateTerrain = onQuickUpdateTerrain
                )
            } else {
                VillagesMap(
                    points = villagePoints,
                    villageInfos = villageInfos,
                    onRequestVillageInfo = onRequestVillageInfo
                )
            }
        }
    }
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { idx, label ->
            val isSelected = idx == selected
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onSelect(idx) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, hollow: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (hollow) Color.White else color)
                .then(if (hollow) Modifier.background(color.copy(alpha = 0.25f)) else Modifier)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun isPlanned(item: RaceItemWithContext, distanceKm: Int): Boolean =
    item.terrainWeather?.isPlannedForSimulator == true && item.terrainWeather.plannedDistanceKm == distanceKm

@Composable
private fun SeasonRacesMap(
    seasonYear: Int,
    selectedDistanceKm: Int,
    racesWithContext: List<RaceItemWithContext>,
    defaultTargetPoints: Int,
    onOpenRaceDetail: (String) -> Unit,
    onTogglePlannedForSimulator: (raceId: String, isPlanned: Boolean, targetPoints: Int) -> Unit,
    onQuickUpdateTerrain: (raceId: String, terrainCondition: String) -> Unit
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(RaceMapFilter.ALL) }
    var selectedRaceId by rememberSaveable(seasonYear) { mutableStateOf<String?>(null) }

    val runCount = racesWithContext.count { it.resultForDistance(selectedDistanceKm) != null }
    val plannedCount = racesWithContext.count { isPlanned(it, selectedDistanceKm) }
    ChipRow(
        labels = listOf(
            "Toutes (${racesWithContext.size})",
            "Courues ($runCount)",
            "Prévues ($plannedCount)",
            "À courir (${racesWithContext.size - runCount})"
        ),
        selected = filter.ordinal,
        onSelect = { filter = RaceMapFilter.entries[it] }
    )

    val shown = racesWithContext.filter { item ->
        val run = item.resultForDistance(selectedDistanceKm) != null
        when (filter) {
            RaceMapFilter.ALL -> true
            RaceMapFilter.RUN -> run
            RaceMapFilter.PLANNED -> isPlanned(item, selectedDistanceKm)
            RaceMapFilter.TO_RUN -> !run
        }
    }
    // Manche sans lieu de départ connu : pas de pastille (aucune position inventée).
    val markers = shown.mapNotNull { item ->
        val lat = item.gaumeGeo.latitude ?: return@mapNotNull null
        val lon = item.gaumeGeo.longitude ?: return@mapNotNull null
        val result = item.resultForDistance(selectedDistanceKm)
        val counting = item.isCountingForDistance(selectedDistanceKm)
        MapMarker(
            key = item.race.raceId,
            latitude = lat,
            longitude = lon,
            label = item.gaumeGeo.canonicalVillage,
            badge = item.race.roundNumber.toString(),
            color = when {
                result != null && counting -> CountingTop15BadgeGreen
                result != null -> GoldPodium
                isPlanned(item, selectedDistanceKm) -> SemoisRiverTeal
                else -> ClayTerracotta
            },
            hollow = result == null && !isPlanned(item, selectedDistanceKm)
        )
    }

    OsmMap(
        markers = markers,
        selectedKey = selectedRaceId,
        onSelect = { selectedRaceId = it },
        fitKey = "races_${seasonYear}_${filter}"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        LegendDot(CountingTop15BadgeGreen, "Courue (Top 15)")
        LegendDot(GoldPodium, "Courue")
        LegendDot(SemoisRiverTeal, "Prévue")
        LegendDot(ClayTerracotta, "À courir", hollow = true)
    }

    if (shown.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shown.forEach { item ->
                val isSelected = item.race.raceId == selectedRaceId
                val hasRun = item.resultForDistance(selectedDistanceKm) != null
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        hasRun -> CountingTop15BadgeGreen.copy(alpha = 0.14f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedRaceId = item.race.raceId }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = "M${item.race.roundNumber} ${item.gaumeGeo.canonicalVillage}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected || hasRun) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                hasRun -> CountingTop15BadgeGreen
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    val item = racesWithContext.firstOrNull { it.race.raceId == selectedRaceId }
    if (item == null) {
        Text("Touche une manche sur la carte pour sa fiche.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val result = item.resultForDistance(selectedDistanceKm)
    val tw = item.terrainWeather
    val currentTerrain = tw?.terrainCondition.orEmpty()
    val planned = isPlanned(item, selectedDistanceKm)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manche ${item.race.roundNumber} • ${item.gaumeGeo.canonicalVillage} • ${item.race.dateIso}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
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
                }
                if (result != null) {
                    Surface(shape = RoundedCornerShape(8.dp), color = CountingTop15BadgeGreen.copy(alpha = 0.15f)) {
                        Text(
                            text = "${result.officialTimeFormatted} • ${result.challengePoints} pts",
                            style = MaterialTheme.typography.labelMedium,
                            color = CountingTop15BadgeGreen,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (currentTerrain.isNotBlank()) {
                    TerrainMiniBadge(terrainCondition = currentTerrain)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Terrain :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                listOf("Sec", "Souple", "Boueux", "Très gras").forEach { cond ->
                    val isCond = currentTerrain == cond
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (isCond) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isCond) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
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
                                fontWeight = if (isCond) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCond) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val la = item.gaumeGeo.latitude
                        val lo = item.gaumeGeo.longitude
                        if (la != null && lo != null) openDirectionsTo(context, la, lo)
                    },
                    enabled = item.gaumeGeo.hasKnownLocation,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Itinéraire", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                if (result == null && !item.race.isCompleted) {
                    OutlinedButton(
                        onClick = {
                            onTogglePlannedForSimulator(item.race.raceId, !planned, tw?.simulatedTargetPoints ?: defaultTargetPoints)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(
                            imageVector = if (planned) Icons.Default.CheckCircle else Icons.Default.AddTask,
                            contentDescription = null,
                            tint = if (planned) CountingTop15BadgeGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (planned) "Prévue ✓" else "Prévoir", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                Button(
                    onClick = { onOpenRaceDetail(item.race.raceId) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fiche", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun VillagesMap(
    points: List<VillageMapPoint>,
    villageInfos: Map<String, VillageInfoEntity>,
    onRequestVillageInfo: (String) -> Unit
) {
    val seasons = remember(points) { points.flatMap { it.calendarSeasons }.distinct().sorted() }
    var seasonIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = toutes saisons
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val seasonFilter = seasons.getOrNull(seasonIndex - 1)

    ChipRow(
        labels = listOf("Toutes saisons") + seasons.map { it.toString() },
        selected = seasonIndex,
        onSelect = { seasonIndex = it }
    )
    val shown = points.filter { seasonFilter == null || seasonFilter in it.calendarSeasons }
    fun visits(p: VillageMapPoint) = if (seasonFilter == null) p.visitsBySeason.values.sum() else p.visitsBySeason[seasonFilter] ?: 0
    Text(
        "${shown.count { visits(it) > 0 }} village(s) courus sur ${shown.size}" + (seasonFilter?.let { " au calendrier $it" } ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val markers = shown.map { p ->
        val n = visits(p)
        MapMarker(
            key = p.key,
            latitude = p.latitude,
            longitude = p.longitude,
            label = p.village,
            badge = if (n > 0) n.toString() else null,
            color = if (n > 0) CountingTop15BadgeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            hollow = n == 0,
            sizeFactor = if (n > 0) 1f + minOf(n, 6) * 0.08f else 0.75f
        )
    }
    OsmMap(
        markers = markers,
        selectedKey = selectedKey,
        onSelect = { selectedKey = it },
        fitKey = "villages_$seasonFilter"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LegendDot(CountingTop15BadgeGreen, "Couru (nombre de fois)")
        LegendDot(MaterialTheme.colorScheme.onSurfaceVariant, "Jamais couru", hollow = true)
    }
    val selected = shown.firstOrNull { it.key == selectedKey }
    if (selected != null) {
        LaunchedEffect(selected.key) { onRequestVillageInfo(selected.village) }
        VillageInfoBlock(selected.village, visits(selected), villageInfos[selected.key])
    } else {
        Text("Touche un village pour sa fiche.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
