package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RaceResultEntity
import com.example.data.local.RunnerProfileEntity
import com.example.domain.RaceItemWithContext
import com.example.domain.RunnerChallengeSummary
import com.example.ui.components.GaumeRacesMapCard
import com.example.ui.components.TerrainAndWeatherMiniBadge
import com.example.ui.components.Top15SimulatorBannerCard
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium

private enum class CalendarDisplayFilter {
    ALL,
    COMPLETED,
    UPCOMING
}

/**
 * Onglet 3 : « Trio & Carte » (Sans Doublon, organisé en 2 sous-vues claires) :
 * - Sous-vue 0 : 🗺️ Carte de Gaume & Simulateur Top 15
 * - Sous-vue 1 : 👥 Comparaison Trio (Damien, Laetitia, Élodie) & Calendrier Partagé
 */
@Composable
fun SharedCalendarScreen(
    seasonYear: Int,
    selectedDistanceKm: Int,
    profiles: List<RunnerProfileEntity>,
    trioSummaries: Map<String, RunnerChallengeSummary>,
    horsChallengeItems: List<RaceItemWithContext> = emptyList(),
    onOpenRaceDetail: (String) -> Unit,
    onOpenTop15Simulator: () -> Unit = {},
    onTogglePlannedSimulatorRace: (raceId: String, isPlanned: Boolean, targetPoints: Int) -> Unit,
    onQuickUpdateTerrain: (raceId: String, terrainCondition: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 = Carte de Gaume & Simulateur Top 15, 1 = Comparaison Trio & Calendrier
    var selectedSubView by remember { mutableIntStateOf(0) }
    var displayFilter by remember { mutableStateOf(CalendarDisplayFilter.ALL) }

    val primarySummary = trioSummaries["me"] ?: trioSummaries.values.firstOrNull()
    val allRaces = primarySummary?.racesWithContext.orEmpty()
    val simulatorSummary = primarySummary?.simulatorForDistance(selectedDistanceKm)

    // Calendrier complet : manches et courses hors challenge, dans l'ordre des dates.
    val calendarRaces = remember(allRaces, horsChallengeItems) {
        (allRaces + horsChallengeItems).sortedWith(
            compareBy<RaceItemWithContext>({ it.race.dateIso }, { it.race.roundNumber })
        )
    }
    val filteredRaces = remember(calendarRaces, displayFilter) {
        when (displayFilter) {
            CalendarDisplayFilter.ALL -> calendarRaces
            CalendarDisplayFilter.COMPLETED -> calendarRaces.filter { it.race.isCompleted }
            CalendarDisplayFilter.UPCOMING -> calendarRaces.filter { !it.race.isCompleted }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("shared_calendar_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sélecteur de sous-vue en haut : [Carte & Simulateur] ⇄ [Comparaison Trio & Calendrier]
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val subTabs = listOf(
                        Triple(0, "Carte & Simulateur", Icons.Default.Map),
                        Triple(1, "Trio & Calendrier (${allRaces.size})", Icons.Default.Groups)
                    )
                    subTabs.forEach { (idx, label, icon) ->
                        val isSelected = selectedSubView == idx
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedSubView = idx }
                                .testTag("trio_map_subtab_$idx"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedSubView == 0) {
            // SOUS-VUE 0 : CARTE INTERACTIVE DE LA GAUME & SIMULATEUR TOP 15
            if (allRaces.isNotEmpty()) {
                item {
                    GaumeRacesMapCard(
                        seasonYear = seasonYear,
                        selectedDistanceKm = selectedDistanceKm,
                        racesWithContext = allRaces,
                        defaultTargetPoints = simulatorSummary?.defaultSuggestedTargetPoints ?: 0,
                        onOpenRaceDetail = onOpenRaceDetail,
                        onTogglePlannedForSimulator = onTogglePlannedSimulatorRace,
                        onQuickUpdateTerrain = onQuickUpdateTerrain
                    )
                }
            }

            if (simulatorSummary != null) {
                item {
                    Top15SimulatorBannerCard(
                        simulation = simulatorSummary,
                        onOpenSimulatorModal = onOpenTop15Simulator
                    )
                }
            }
        } else {
            // SOUS-VUE 1 : COMPARAISON DU TRIO (DAMIEN, LAETITIA, ÉLODIE) & CALENDRIER PARTAGÉ
            item {
                TrioComparisonHeaderCard(
                    seasonYear = seasonYear,
                    profiles = profiles,
                    trioSummaries = trioSummaries
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Calendrier $seasonYear (${allRaces.size} manches" +
                                if (horsChallengeItems.isNotEmpty()) " + ${horsChallengeItems.size} hors challenge)" else ")",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filterOptions = listOf(
                            CalendarDisplayFilter.ALL to "Toutes (${calendarRaces.size})",
                            CalendarDisplayFilter.COMPLETED to "Terminées",
                            CalendarDisplayFilter.UPCOMING to "À venir"
                        )
                        filterOptions.forEach { (filterMode, label) ->
                            val isSelected = displayFilter == filterMode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { displayFilter = filterMode }
                                    .testTag("cal_filter_${filterMode.name.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(filteredRaces, key = { it.race.raceId }) { item ->
                SharedCalendarRaceCard(
                    item = item,
                    profiles = profiles,
                    trioSummaries = trioSummaries,
                    onClick = { onOpenRaceDetail(item.race.raceId) }
                )
            }
        }
    }
}

@Composable
private fun TrioComparisonHeaderCard(
    seasonYear: Int,
    profiles: List<RunnerProfileEntity>,
    trioSummaries: Map<String, RunnerChallengeSummary>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trio_comparison_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Bilan Trio Gaume Trail • $seasonYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Challenges 10 km et 5 km comptabilisés séparément (Max 15)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEach { profile ->
                    val summary = trioSummaries[profile.profileId]
                    val c10 = summary?.challenge10km
                    val c5 = summary?.challenge5km
                    val primaryChallenge = if ((c10?.completedRacesCount ?: 0) >= (c5?.completedRacesCount ?: 0) &&
                        profile.preferredDistanceKm == 10
                    ) {
                        c10
                    } else {
                        c5 ?: c10
                    }
                    val secondaryChallenge = if (primaryChallenge?.distanceKm == 10) c5 else c10
                    val color = Color(profile.avatarColorHex)

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = color.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = profile.displayName.substringBefore(" "),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${primaryChallenge?.totalChallengePoints ?: 0} pts",
                                style = MaterialTheme.typography.titleSmall,
                                color = color,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Text(
                                text = "${primaryChallenge?.distanceLabel ?: "10 km"} : ${primaryChallenge?.countingRacesCount ?: 0}/15",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if ((secondaryChallenge?.completedRacesCount ?: 0) > 0) {
                                Text(
                                    text = "${secondaryChallenge?.distanceLabel} : ${secondaryChallenge?.countingRacesCount}/15",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedCalendarRaceCard(
    item: RaceItemWithContext,
    profiles: List<RunnerProfileEntity>,
    trioSummaries: Map<String, RunnerChallengeSummary>,
    onClick: () -> Unit
) {
    val race = item.race
    val tw = item.terrainWeather

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("shared_race_card_${race.roundNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (race.isSeasonFinale) {
                GoldPodium.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!race.isChallenge) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else if (race.isSeasonFinale) {
                            GoldPodium.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = if (race.isChallenge) "M#${race.roundNumber}" else "HC",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (race.isSeasonFinale) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = race.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${race.village} • ${race.dateIso} • " + if (race.isChallenge) {
                                "10 km & 5 km"
                            } else {
                                listOfNotNull(race.longDistanceLabel, race.shortDistanceLabel).joinToString(" & ")
                                    .ifBlank { null }?.let { "$it • " }.orEmpty() + "hors challenge"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Terrain et météo affichés seulement s'ils ont été saisis.
                if (tw != null && tw.terrainCondition.isNotBlank()) {
                    TerrainAndWeatherMiniBadge(
                        terrainCondition = tw.terrainCondition,
                        weatherCondition = tw.weatherCondition,
                        temperatureC = tw.temperatureC
                    )
                }
            }

            if (item.resultsByProfileAndDistance.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    profiles.forEach { profile ->
                        val resultsForRunner: List<RaceResultEntity> =
                            item.resultsByProfileAndDistance[profile.profileId].orEmpty()

                        if (resultsForRunner.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(profile.avatarColorHex))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = profile.displayName.substringBefore(" "),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "Non courue / Dossard non lié",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        } else {
                            resultsForRunner.forEach { result ->
                                val distKm = if (result.distanceLabel == "5 km") 5 else 10
                                val isCounting = trioSummaries[profile.profileId]
                                    ?.challengeForDistance(distKm)
                                    ?.countingRaceIds
                                    ?.contains(race.raceId) == true

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(0.45f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(profile.avatarColorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${profile.displayName.substringBefore(" ")} (${result.distanceName ?: result.distanceLabel})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.weight(0.55f),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${result.bibNumber} • ${result.officialTimeFormatted}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCounting) {
                                                CountingTop15BadgeGreen.copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isCounting) {
                                                    Icon(
                                                        imageVector = Icons.Default.EmojiEvents,
                                                        contentDescription = null,
                                                        tint = CountingTop15BadgeGreen,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                }
                                                Text(
                                                    text = if (race.isChallenge) "${result.challengePoints} pts" else "hors challenge",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isCounting) {
                                                        CountingTop15BadgeGreen
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
