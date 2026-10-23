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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RunnerProfileEntity
import com.example.domain.ChallengeStandingsCalculator
import com.example.domain.ProfileChallengeStanding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatPoints(points: Int): String = "%,d".format(Locale.FRANCE, points).replace(' ', ' ')

/**
 * Classement provisoire du Challenge pour le profil actif sur la distance choisie,
 * avec le coureur juste devant et un récapitulatif du Trio.
 */
@Composable
fun ChallengeStandingCard(
    activeProfile: RunnerProfileEntity,
    selectedDistanceKm: Int,
    trioStandings: Map<String, Map<Int, ProfileChallengeStanding>>,
    profiles: List<RunnerProfileEntity>,
    modifier: Modifier = Modifier
) {
    val mine = trioStandings[activeProfile.profileId]?.get(selectedDistanceKm)
    val distanceLabel = "$selectedDistanceKm km"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("challenge_standing_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFD49B1B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Classement challenge $distanceLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "provisoire",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (mine == null) {
                Text(
                    text = "${activeProfile.displayName} n'apparaît pas encore au classement du $distanceLabel " +
                        "(aucun résultat trouvé avec son identifiant ou son dossard, ou synchronisation à faire).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val st = mine.standing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${st.overallRank}e / ${mine.runnersInDistance}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "au général",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${st.categoryRank}e / ${mine.runnersInCategory}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "catégorie ${st.category.ifBlank { "?" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${formatPoints(st.totalPoints)} pts • ${st.racesCounted}/${ChallengeStandingsCalculator.MAX_COUNTING_RACES} " +
                        "courses comptées" + if (st.racesRun > st.racesCounted) " (${st.racesRun} courues)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val above = mine.nextAbove
                val gap = mine.pointsGapToNextAbove
                val needed = mine.racePointsNeededToPassNext
                if (above == null || gap == null || needed == null) {
                    Text(
                        text = "En tête du classement provisoire !",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E7E48)
                    )
                } else {
                    Text(
                        text = "Juste devant : ${above.fullName} (${above.overallRank}e) à ${formatPoints(gap - 1)} pts",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            needed > 1200 -> "Hors de portée en une seule course (il faudrait ${formatPoints(needed)} pts)."
                            st.racesCounted >= ChallengeStandingsCalculator.MAX_COUNTING_RACES ->
                                "Pour passer devant : une course à ${formatPoints(needed)} pts ou plus " +
                                    "(elle remplacerait ton plus faible résultat compté, ${formatPoints(st.lowestCountedPoints)} pts)."
                            else -> "Pour passer devant : une course à ${formatPoints(needed)} pts ou plus suffit."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val trioLines = profiles.mapNotNull { p ->
                val byDist = trioStandings[p.profileId].orEmpty()
                val dist = when {
                    byDist.containsKey(p.preferredDistanceKm) -> p.preferredDistanceKm
                    else -> byDist.keys.firstOrNull()
                } ?: return@mapNotNull null
                val s = byDist[dist] ?: return@mapNotNull null
                "${p.displayName.substringBefore(" ")} • $dist km : ${s.standing.overallRank}e / ${s.runnersInDistance} • " +
                    "${formatPoints(s.standing.totalPoints)} pts"
            }
            if (trioLines.size > 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "Le Trio",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                trioLines.forEach { line ->
                    Text(text = line, style = MaterialTheme.typography.bodySmall)
                }
            }

            val computedAt = mine?.standing?.computedAtEpochMillis
                ?: trioStandings.values.flatMap { it.values }.firstOrNull()?.standing?.computedAtEpochMillis
            Text(
                text = "Recalculé depuis les résultats MyChrono (15 meilleurs résultats sur la distance)" +
                    (computedAt?.let { " le " + SimpleDateFormat("dd/MM à HH:mm", Locale.FRANCE).format(Date(it)) } ?: "") +
                    ". Le classement officiel peut différer en cas d'égalité.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
