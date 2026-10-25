package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.VillageInfoEntity
import com.example.domain.PassportVillage
import com.example.domain.VillagePassport
import com.example.ui.theme.SemoisRiverTeal

/** Passeport des villages : chaque village où le coureur a couru, toutes saisons confondues. */
@Composable
fun VillagePassportCard(
    runnerName: String,
    passport: VillagePassport,
    onOpenRaceDetail: (String) -> Unit,
    villageInfos: Map<String, VillageInfoEntity> = emptyMap(),
    onRequestVillageInfo: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("village_passport"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Passeport des villages • $runnerName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${passport.visited.size} village(s) • ${passport.totalVisits} course(s), toutes saisons " +
                        "et courses hors challenge comprises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (passport.notYetVisited.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "Jamais couru (calendrier ${passport.latestCalendarSeason ?: ""}) : ${passport.notYetVisited.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = passport.notYetVisited.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (passport.visited.isEmpty()) {
            Text(
                text = "Aucune course courue pour l'instant.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        }

        passport.visited.forEach { village ->
            PassportVillageRow(
                village = village,
                info = villageInfos[GaumeInitialSeedData.normalizeKey(village.village)],
                onOpenRaceDetail = onOpenRaceDetail,
                onRequestVillageInfo = onRequestVillageInfo
            )
        }
    }
}

@Composable
private fun PassportVillageRow(
    village: PassportVillage,
    info: VillageInfoEntity?,
    onOpenRaceDetail: (String) -> Unit,
    onRequestVillageInfo: (String) -> Unit
) {
    var expanded by remember(village.village) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = SemoisRiverTeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = village.village,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${village.territoryName} • depuis ${village.firstSeason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "×${village.visits.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            if (expanded) {
                LaunchedEffect(village.village) { onRequestVillageInfo(village.village) }
                village.visits.forEach { v ->
                    Text(
                        text = "${v.dateIso} • ${v.raceTitle.substringAfter("— ")} • ${v.distanceName} • ${v.timeFormatted}" +
                            if (!v.isChallenge) " • hors challenge" else "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRaceDetail(v.raceId) }
                            .padding(vertical = 2.dp)
                    )
                }
                VillageInfoBlock(village = village.village, visits = village.visits.size, info = info, showHeader = false)
            }
        }
    }
}

@Composable
fun VillageInfoBlock(village: String, visits: Int, info: VillageInfoEntity?, showHeader: Boolean = true) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (showHeader) {
            Text(
                "$village • " + if (visits > 0) "$visits course(s)" else "jamais couru",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        when {
            info == null -> Text("Recherche de la fiche…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            !info.found -> Text("Pas de fiche Wikipédia fiable pour ce village.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                Text(info.extract.orEmpty(), style = MaterialTheme.typography.bodySmall)
                if (!info.pageUrl.isNullOrBlank()) {
                    TextButton(onClick = { openExternalUrl(context, info.pageUrl) }) {
                        Text("Source : Wikipédia (« ${info.title} »)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

