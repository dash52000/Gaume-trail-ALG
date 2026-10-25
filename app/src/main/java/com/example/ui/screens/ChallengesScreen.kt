package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.VillageInfoEntity
import com.example.domain.CourseRecord
import com.example.domain.FactBadge
import com.example.domain.GoalProgress
import com.example.domain.GoalsCalculator
import com.example.domain.RecordStatus
import com.example.domain.RecordsSummary
import com.example.domain.TrioChallenge
import com.example.domain.VillageMapPoint
import com.example.domain.VillagePassport
import com.example.ui.components.VillageInfoBlock
import com.example.ui.components.VillagePassportCard
import com.example.ui.components.openExternalUrl
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import kotlin.math.cos
import kotlin.math.hypot

/** Onglet « Défis » : records de parcours, badges, défis du Trio, villages et objectifs. */
@Composable
fun ChallengesContent(
    runnerName: String,
    seasonYear: Int,
    selectedDistanceKm: Int,
    records: RecordsSummary?,
    badges: List<FactBadge>,
    trioChallenges: List<TrioChallenge>,
    goalSeason: Int,
    goals: List<GoalProgress>,
    passport: VillagePassport?,
    mapPoints: List<VillageMapPoint>,
    villageInfos: Map<String, VillageInfoEntity>,
    onSelectDistanceKm: (Int) -> Unit,
    onOpenRaceDetail: (String) -> Unit,
    onSetGoal: (goalType: String, target: Int) -> Unit,
    onRequestVillageInfo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val unlocked = badges.count { it.unlocked }
            listOf(
                "Records" + (records?.let { " (${it.beaten})" } ?: ""),
                "Badges ($unlocked/${badges.size})",
                "Trio",
                "Villages" + (passport?.let { " (${it.visited.size})" } ?: ""),
                "Objectifs $goalSeason"
            ).forEachIndexed { idx, label ->
                val sel = tab == idx
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { tab = idx }
                        .testTag("challenges_tab_$idx")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        when (tab) {
            0 -> RecordsSection(seasonYear, selectedDistanceKm, records, onSelectDistanceKm, onOpenRaceDetail)
            1 -> BadgesSection(badges)
            2 -> TrioSection(seasonYear, trioChallenges)
            3 -> VillagesSection(runnerName, passport, mapPoints, villageInfos, onOpenRaceDetail, onRequestVillageInfo)
            else -> GoalsSection(goalSeason, goals, onSetGoal)
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
private fun Progress(progress: Int, target: Int, done: Boolean) {
    LinearProgressIndicator(
        progress = { if (target > 0) (progress.toFloat() / target).coerceIn(0f, 1f) else 0f },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(99.dp)),
        color = if (done) CountingTop15BadgeGreen else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

// ---------- Records de parcours ----------

@Composable
private fun RecordsSection(
    seasonYear: Int,
    selectedDistanceKm: Int,
    records: RecordsSummary?,
    onSelectDistanceKm: (Int) -> Unit,
    onOpenRaceDetail: (String) -> Unit
) {
    SectionCard {
        Text("Records de parcours • $seasonYear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Chaque parcours déjà couru devient un duel contre toi-même : bats ta meilleure édition. " +
                "Même longueur : au temps ; longueur changée : à l'allure.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(10, 5).forEach { d ->
                val sel = d == selectedDistanceKm
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectDistanceKm(d) }
                ) {
                    Text(
                        "$d km",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        if (records != null && records.records.isNotEmpty()) {
            Text(
                "${records.beaten} battu(s) • ${records.notBeaten} pas battu(s) • ${records.toBeat} à battre",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    if (records == null || records.records.isEmpty()) {
        Text("Aucun parcours couru sur cette distance.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
        return
    }
    records.records.forEach { r -> RecordCard(r, onOpenRaceDetail) }
}

@Composable
private fun RecordCard(r: CourseRecord, onOpenRaceDetail: (String) -> Unit) {
    val (label, color) = when (r.status) {
        RecordStatus.BEATEN -> "Record battu" to CountingTop15BadgeGreen
        RecordStatus.NOT_BEATEN -> "Pas battu" to ClayTerracotta
        RecordStatus.TO_BEAT -> "À battre" to GoldPodium
        RecordStatus.FIRST -> "1re édition" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenRaceDetail(r.upcomingRaceId ?: r.editions.last().raceId) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(r.village, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(r.title.substringAfter("— "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Text(
                r.editions.joinToString("  •  ") { e ->
                    "${e.seasonYear} : ${e.timeFormatted}" + if (e == r.best && r.editions.size > 1) " ★" else ""
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            r.deltaSeconds?.let {
                Text(
                    "${GoalsCalculator.formatDelta(it)} sur la référence ${r.bestBefore?.seasonYear ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it < 0) CountingTop15BadgeGreen else ClayTerracotta
                )
            }
            if (r.status == RecordStatus.TO_BEAT && r.upcomingDateIso != null) {
                Text(
                    "Le ${r.upcomingDateIso.substring(8)}/${r.upcomingDateIso.substring(5, 7)} : ${r.best.timeFormatted} à battre (${r.best.seasonYear})",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldPodium
                )
            }
        }
    }
}

// ---------- Badges ----------

@Composable
private fun BadgesSection(badges: List<FactBadge>) {
    SectionCard {
        Text("Badges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Tous viennent de faits vérifiables : résultats MyChrono, météo réelle au départ, indice des parcours, calendrier.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    badges.groupBy { it.category }.forEach { (category, list) ->
        Text(category, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        list.sortedByDescending { it.unlocked }.forEach { b -> BadgeRow(b) }
    }
}

@Composable
private fun BadgeRow(b: FactBadge) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("badge_${b.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (b.unlocked) GoldPodium.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (b.unlocked) GoldPodium.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (b.unlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = if (b.unlocked) GoldPodium else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(b.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(b.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                b.detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
                if (!b.unlocked && b.target > 1) {
                    Progress(b.progress, b.target, false)
                    Text("${b.progress} / ${b.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---------- Trio ----------

@Composable
private fun TrioSection(seasonYear: Int, challenges: List<TrioChallenge>) {
    SectionCard {
        Text("Défis du Trio • $seasonYear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text("À trois, pas les uns contre les autres.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    challenges.forEach { c ->
        SectionCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(c.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (c.target != null) "${c.progress} / ${c.target} ${c.unit}" else "${c.progress} ${c.unit}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(c.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (c.target != null) Progress(c.progress, c.target, c.progress >= c.target)
            c.detail?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

// ---------- Villages : carte + fiches ----------

@Composable
private fun VillagesSection(
    runnerName: String,
    passport: VillagePassport?,
    points: List<VillageMapPoint>,
    villageInfos: Map<String, VillageInfoEntity>,
    onOpenRaceDetail: (String) -> Unit,
    onRequestVillageInfo: (String) -> Unit
) {
    val seasons = points.flatMap { it.calendarSeasons }.distinct().sorted()
    var seasonFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }

    SectionCard {
        Text("Carte des villages • $runnerName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (listOf<Int?>(null) + seasons).forEach { y ->
                val sel = seasonFilter == y
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { seasonFilter = y }
                ) {
                    Text(
                        y?.toString() ?: "Toutes saisons",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        val shown = points.filter { seasonFilter == null || seasonFilter in it.calendarSeasons }
        fun visits(p: VillageMapPoint) = if (seasonFilter == null) p.visitsBySeason.values.sum() else p.visitsBySeason[seasonFilter] ?: 0
        Text(
            "${shown.count { visits(it) > 0 }} village(s) courus sur ${shown.size}" + (seasonFilter?.let { " au calendrier $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (shown.isNotEmpty()) {
            VillageMap(shown, ::visits, selectedKey) { selectedKey = it }
        }
        val selected = shown.firstOrNull { it.key == selectedKey }
        if (selected != null) {
            LaunchedEffect(selected.key) { onRequestVillageInfo(selected.village) }
            VillageInfoBlock(selected.village, visits(selected), villageInfos[selected.key])
        } else {
            Text("Touche un village pour sa fiche.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (passport != null) {
        VillagePassportCard(
            runnerName = runnerName,
            passport = passport,
            onOpenRaceDetail = onOpenRaceDetail,
            villageInfos = villageInfos,
            onRequestVillageInfo = onRequestVillageInfo
        )
    }
}

@Composable
private fun VillageMap(
    points: List<VillageMapPoint>,
    visits: (VillageMapPoint) -> Int,
    selectedKey: String?,
    onSelect: (String) -> Unit
) {
    val visitedColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val selectColor = GoldPodium
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    val lonScale = cos(Math.toRadians((minLat + maxLat) / 2))
    val spanX = ((maxLon - minLon) * lonScale).coerceAtLeast(0.01)
    val spanY = (maxLat - minLat).coerceAtLeast(0.01)

    fun project(p: VillageMapPoint, w: Float, h: Float, pad: Float): Offset {
        val scale = minOf((w - 2 * pad) / spanX, (h - 2 * pad) / spanY).toFloat()
        val offX = (w - spanX.toFloat() * scale) / 2
        val offY = (h - spanY.toFloat() * scale) / 2
        return Offset(
            offX + ((p.longitude - minLon) * lonScale).toFloat() * scale,
            offY + (maxLat - p.latitude).toFloat() * scale
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .testTag("village_map")
                .pointerInput(points) {
                    detectTapGestures { tap ->
                        val pad = 14.dp.toPx()
                        val nearest = points.minByOrNull {
                            val o = project(it, this.size.width.toFloat(), this.size.height.toFloat(), pad)
                            hypot(o.x - tap.x, o.y - tap.y)
                        }
                        if (nearest != null) {
                            val o = project(nearest, this.size.width.toFloat(), this.size.height.toFloat(), pad)
                            if (hypot(o.x - tap.x, o.y - tap.y) < 28.dp.toPx()) onSelect(nearest.key)
                        }
                    }
                }
        ) {
            drawRect(bg)
            val pad = 14.dp.toPx()
            points.sortedBy { visits(it) }.forEach { p ->
                val o = project(p, size.width, size.height, pad)
                val n = visits(p)
                if (n > 0) {
                    drawCircle(visitedColor, radius = (4.dp.toPx() + minOf(n, 6) * 1.2.dp.toPx()), center = o)
                } else {
                    drawCircle(emptyColor, radius = 3.dp.toPx(), center = o, style = Stroke(width = 1.5.dp.toPx()))
                }
                if (p.key == selectedKey) {
                    drawCircle(selectColor, radius = 12.dp.toPx(), center = o, style = Stroke(width = 2.5.dp.toPx()))
                }
            }
        }
    }
}

// ---------- Objectifs ----------

@Composable
private fun GoalsSection(goalSeason: Int, goals: List<GoalProgress>, onSetGoal: (String, Int) -> Unit) {
    SectionCard {
        Text("Objectifs $goalSeason", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Ceux que tu choisis, rien d'imposé. Les valeurs proposées viennent de ta saison précédente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    goals.forEach { g ->
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(g.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(g.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (g.target != null) "${g.progress} / ${g.target}" else "${g.progress}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (g.target != null && g.progress >= g.target) CountingTop15BadgeGreen else MaterialTheme.colorScheme.primary
                )
            }
            if (g.target != null) {
                Progress(g.progress, g.target, g.progress >= g.target)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val step = if (g.type == GoalsCalculator.KM) 10 else 1
                    OutlinedButton(onClick = { onSetGoal(g.type, g.target - step) }) { Text("−") }
                    OutlinedButton(onClick = { onSetGoal(g.type, g.target + step) }) { Text("+") }
                    TextButton(onClick = { onSetGoal(g.type, 0) }) { Text("Retirer") }
                }
            } else {
                OutlinedButton(onClick = { onSetGoal(g.type, g.suggested) }) {
                    Text("Fixer : ${g.suggested} ${g.unit}")
                }
            }
        }
    }
}
