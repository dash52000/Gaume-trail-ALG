package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.GaumeBadgeItem
import com.example.domain.GaumeConquestSummary
import com.example.domain.GaumeStreakSummary
import com.example.domain.GaumeTerritoryConquestProgress
import com.example.domain.GaumeTrioQuestProgress
import com.example.domain.GaumeVillageStampItem
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal

private enum class GamificationSectionTab {
    PASSPORT_STAMPS,
    STREAK_AND_QUESTS,
    TERRITORIES,
    BADGES
}

/**
 * Carte de Gamification Complète :
 * 1) Passeport Collector de Gaume avec Tampons Officiels par village
 * 2) Série de régularité (Streak), Bonus de fidélité & Quêtes du Trio en Gaume
 * 3) Conquête des 6 Fiefs de Gaume
 * 4) 9 Badges de Terroir & Trophées
 */
@Composable
fun GaumeConquestGamificationCard(
    runnerName: String,
    conquest: GaumeConquestSummary,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(GamificationSectionTab.PASSPORT_STAMPS) }

    val unlockedBadgesCount = conquest.badges.count { it.isUnlocked }
    val completedQuestsCount = conquest.trioQuests.count { it.isCompleted }
    val stampedVillagesCount = conquest.passportStamps.count { it.isStamped }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gaume_conquest_gamification_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, GoldPodium.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // En-tête de Gamification (Niveau, Titre, XP Gaumaise, Série & Multiplicateur de fidélité)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF143D27),
                                Color(0xFF1F5C3B)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                shape = CircleShape,
                                color = GoldPodium.copy(alpha = 0.22f),
                                border = BorderStroke(1.5.dp, GoldPodium),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "N.${conquest.runnerLevel}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = GoldPodium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = conquest.runnerRankTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$runnerName • ${conquest.streakSummary.loyaltyTierTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldPodium.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, GoldPodium.copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = GoldPodium,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${conquest.xpPoints} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldPodium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // 4 Compteurs synthétiques : Tampons Passeport | Série (Streak) | Quêtes Trio | Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GamificationMiniStatBox(
                            label = "Tampons",
                            value = "$stampedVillagesCount / ${conquest.passportStamps.size}",
                            accent = Color(0xFF8BE2A8),
                            modifier = Modifier.weight(1f)
                        )
                        GamificationMiniStatBox(
                            label = "Série Streak",
                            value = "${conquest.streakSummary.currentStreakRaces} (x${conquest.streakSummary.loyaltyMultiplier})",
                            accent = Color(0xFFFFB74D),
                            modifier = Modifier.weight(1f)
                        )
                        GamificationMiniStatBox(
                            label = "Quêtes Trio",
                            value = "$completedQuestsCount / ${conquest.trioQuests.size}",
                            accent = Color(0xFF7AD7F0),
                            modifier = Modifier.weight(1f)
                        )
                        GamificationMiniStatBox(
                            label = "Badges",
                            value = "$unlockedBadgesCount / ${conquest.badges.size}",
                            accent = GoldPodium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sélecteur des 4 volets de Gamification (Passeport | Quêtes & Série | Fiefs | Badges)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    Triple(GamificationSectionTab.PASSPORT_STAMPS, "Passeport ($stampedVillagesCount)", "gamification_tab_passport"),
                    Triple(GamificationSectionTab.STREAK_AND_QUESTS, "Quêtes & Série", "gamification_tab_quests"),
                    Triple(GamificationSectionTab.TERRITORIES, "Fiefs (${conquest.territoriesConqueredCount}/6)", "gamification_tab_territories"),
                    Triple(GamificationSectionTab.BADGES, "Badges ($unlockedBadgesCount)", "gamification_tab_badges")
                )

                tabs.forEach { (tab, title, tag) ->
                    val isSelected = activeTab == tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeTab = tab }
                            .testTag(tag),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
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
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            when (activeTab) {
                GamificationSectionTab.PASSPORT_STAMPS -> {
                    GaumeCollectorPassportSection(
                        stamps = conquest.passportStamps
                    )
                }

                GamificationSectionTab.STREAK_AND_QUESTS -> {
                    GaumeStreakAndQuestsSection(
                        streak = conquest.streakSummary,
                        quests = conquest.trioQuests
                    )
                }

                GamificationSectionTab.TERRITORIES -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        conquest.territories.forEach { territory ->
                            TerritoryConquestRow(territory = territory)
                        }
                    }
                }

                GamificationSectionTab.BADGES -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        conquest.badges.chunked(2).forEach { rowBadges ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBadges.forEach { badge ->
                                    GaumeBadgeCard(
                                        badge = badge,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowBadges.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GaumeCollectorPassportSection(
    stamps: List<GaumeVillageStampItem>
) {
    var showOnlyStamped by remember { mutableStateOf(false) }
    val stampedCount = stamps.count { it.isStamped }
    val displayedStamps = remember(stamps, showOnlyStamped) {
        if (showOnlyStamped) stamps.filter { it.isStamped } else stamps.take(12)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF6F1E6),
            border = BorderStroke(1.dp, Color(0xFFD6C7AE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Passeport Collector des Villages de Gaume",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2B2318),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (stampedCount > 0) {
                            "$stampedCount tampon(s) officiel(s) validé(s) sur ${stamps.size} villages"
                        } else {
                            "0 tampon validé • Courez une manche avec votre dossard pour tamponner le village"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5D503F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (showOnlyStamped) Color(0xFF1B5E3A) else Color(0xFFE5DAC6),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showOnlyStamped = !showOnlyStamped }
                ) {
                    Text(
                        text = if (showOnlyStamped) "Voir tous (${stamps.size})" else "Tamponnés ($stampedCount)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (showOnlyStamped) Color.White else Color(0xFF2B2318),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (displayedStamps.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Aucun village tamponné sur cette distance pour le moment (0 course).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            displayedStamps.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { stamp ->
                        PassportStampCell(
                            stamp = stamp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PassportStampCell(
    stamp: GaumeVillageStampItem,
    modifier: Modifier = Modifier
) {
    val stampColor = Color(stamp.territoryColorHex)
    val rotationDeg = remember(stamp.roundNumber) {
        when (stamp.roundNumber % 4) {
            0 -> -4f
            1 -> 3.5f
            2 -> -2.5f
            else -> 4f
        }
    }

    Surface(
        modifier = modifier.height(94.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (stamp.isStamped) {
            Color(0xFFFAF5EA)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = if (stamp.isStamped) 1.5.dp else 1.dp,
            color = if (stamp.isStamped) stampColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (stamp.isStamped) {
                // Vrai tampon encré incliné du village de Gaume
                Column(
                    modifier = Modifier.rotate(rotationDeg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = stampColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, stampColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = stampColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "M#${stamp.roundNumber} VALIDÉ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = stampColor,
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = stamp.villageName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = stampColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${stamp.stampedTimeFormatted ?: "--:--"} • ${stamp.stampedPoints ?: 0} pts",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2D24),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Dossard #${stamp.stampedBib ?: "--"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5D503F),
                        maxLines = 1
                    )
                }
            } else {
                // Emplacement en filigrane "À tamponner"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "M#${stamp.roundNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stamp.villageName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "À tamponner",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun GaumeStreakAndQuestsSection(
    streak: GaumeStreakSummary,
    quests: List<GaumeTrioQuestProgress>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Carte Série de Régularité (Streak) & Bonus de Fidélité Gaumaise
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = GoldPodium.copy(alpha = 0.13f),
            border = BorderStroke(1.dp, GoldPodium.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFE07A2E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Série de Régularité : ${streak.currentStreakRaces} manche(s) de suite",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Meilleure série : ${streak.bestStreakRaces} • Rang : ${streak.loyaltyTierTitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE07A2E).copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "Bonus +${streak.bonusLoyaltyXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB85412),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Liste des Défis & Quêtes du Trio en Gaume
        quests.forEach { quest ->
            val accent = Color(quest.accentColorHex)
            val progressFraction = if (quest.targetProgress > 0) {
                (quest.currentProgress.toFloat() / quest.targetProgress).coerceIn(0f, 1f)
            } else 0f

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (quest.isCompleted) {
                    CountingTop15BadgeGreen.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                },
                border = BorderStroke(
                    1.dp,
                    if (quest.isCompleted) CountingTop15BadgeGreen.copy(alpha = 0.55f)
                    else accent.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                shape = RoundedCornerShape(6.dp),
                                color = accent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = quest.categoryTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = quest.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = if (quest.isCompleted) "Validée (+${quest.rewardXp} XP)" else "${quest.currentProgress}/${quest.targetProgress} (+${quest.rewardXp} XP)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (quest.isCompleted) CountingTop15BadgeGreen else accent
                        )
                    }

                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = if (quest.isCompleted) CountingTop15BadgeGreen else accent,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
private fun GamificationMiniStatBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TerritoryConquestRow(
    territory: GaumeTerritoryConquestProgress
) {
    val accentColor = Color(territory.colorHex)
    val fraction = if (territory.totalRacesInTerritory > 0) {
        (territory.conqueredRacesCount.toFloat() / territory.totalRacesInTerritory).coerceIn(0f, 1f)
    } else 0f

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (territory.isTerritoryConquered) {
            accentColor.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (territory.isTerritoryConquered) {
                accentColor.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (territory.isTerritoryConquered) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Map
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = territory.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (territory.villagesConqueredNames.isNotEmpty()) {
                                "Conquis : ${territory.villagesConqueredNames.joinToString(", ")}"
                            } else {
                                "Villages : ${territory.subtitle}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (territory.isTerritoryConquered) {
                        CountingTop15BadgeGreen.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = if (territory.isFullyMastered) {
                            "100% Conquis"
                        } else if (territory.isTerritoryConquered) {
                            "${territory.conqueredRacesCount}/${territory.totalRacesInTerritory} • Conquis"
                        } else {
                            "0/${territory.totalRacesInTerritory}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (territory.isTerritoryConquered) {
                            CountingTop15BadgeGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun GaumeBadgeCard(
    badge: GaumeBadgeItem,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(badge.accentColorHex)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (badge.isUnlocked) {
            accentColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        },
        border = BorderStroke(
            width = if (badge.isUnlocked) 1.5.dp else 1.dp,
            color = if (badge.isUnlocked) {
                accentColor
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (badge.isUnlocked) {
                                accentColor.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (badge.isUnlocked) {
                            Icons.Default.EmojiEvents
                        } else {
                            Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = if (badge.isUnlocked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (badge.isUnlocked) {
                        CountingTop15BadgeGreen.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = if (badge.isUnlocked) "Débloqué ✓" else badge.progressText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isUnlocked) {
                            CountingTop15BadgeGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = badge.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (badge.isUnlocked) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
