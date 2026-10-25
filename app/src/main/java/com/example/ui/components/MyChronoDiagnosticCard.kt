package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.RunnerProfileEntity
import com.example.data.repository.SyncDiagnostics
import com.example.ui.theme.ClayTerracotta

/**
 * Diagnostic : pour chaque profil, ce que MyChrono publie sous son identifiant et ce que l'appli
 * a retrouvé. Un écart (ex. 3 courses 5 km publiées, 0 retrouvée) se voit immédiatement.
 */
@Composable
fun MyChronoDiagnosticCard(
    profiles: List<RunnerProfileEntity>,
    diagnostics: SyncDiagnostics,
    foundResultCounts: Map<String, Map<Int, Map<String, Int>>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mychrono_diagnostic_card"),
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
                text = "Diagnostic MyChrono • version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            val seasons = (diagnostics.publishedRacesBySeason.keys + diagnostics.cachedRacesBySeason.keys).sorted()
            if (seasons.isNotEmpty()) {
                Text(
                    text = "Classements en mémoire : " + seasons.joinToString(" • ") { y ->
                        "$y : ${diagnostics.cachedRacesBySeason[y] ?: 0} / ${diagnostics.publishedRacesBySeason[y] ?: 0}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            diagnostics.repairs.forEach {
                Text(text = "Corrigé : $it", style = MaterialTheme.typography.bodySmall, color = ClayTerracotta)
            }

            profiles.forEach { p ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                val report = diagnostics.profiles.firstOrNull { it.profileId == p.profileId }
                val id = p.myChronoRunnerId.trim()
                Text(
                    text = "${p.displayName} • identifiant ${id.ifBlank { "vide" }}" +
                        (report?.myChronoName?.let { " = $it" } ?: if (id.isNotBlank()) " (inconnu de MyChrono)" else ""),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                val published = report?.publishedBySeason.orEmpty()
                val found = foundResultCounts[p.profileId].orEmpty()
                (published.keys + found.keys).sorted().forEach { year ->
                    val line = listOf("10 km", "5 km").mapNotNull { label ->
                        val pub = published[year]?.get(label) ?: 0
                        val got = found[year]?.get(label) ?: 0
                        if (pub == 0 && got == 0) null else "$label : $got retrouvée(s) / $pub publiée(s)"
                    }
                    if (line.isNotEmpty()) {
                        val mismatch = listOf("10 km", "5 km").any { label ->
                            (published[year]?.get(label) ?: 0) > (found[year]?.get(label) ?: 0)
                        }
                        Text(
                            text = "$year — " + line.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (mismatch) ClayTerracotta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
