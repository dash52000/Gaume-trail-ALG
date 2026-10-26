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

/** Badge compact de l'état du terrain noté par le coureur (Sec, Souple, Boueux, Très gras). */
@Composable
fun TerrainMiniBadge(
    terrainCondition: String,
    modifier: Modifier = Modifier
) {
    val (terrainBg, terrainFg) = when (terrainCondition) {
        "Boueux" -> Color(0xFF795548).copy(alpha = 0.16f) to Color(0xFF5D4037)
        "Très gras" -> ClayTerracotta.copy(alpha = 0.18f) to ClayTerracotta
        "Souple" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> CountingTop15BadgeGreen.copy(alpha = 0.14f) to CountingTop15BadgeGreen
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = terrainBg,
        modifier = modifier
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
